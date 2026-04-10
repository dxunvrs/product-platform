package network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final Selector selector;
    private final DatagramChannel udpChannel;
    private final ServerSocketChannel tcpServerSocket;

    private final Map<SocketChannel, Queue<ByteBuffer>> serverQueue = new ConcurrentHashMap<>();
    private final BlockingQueue<RawResponse> responseQueue;

    private final List<SocketChannel> servers = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    private final ByteBuffer readBuffer = ByteBuffer.allocate(65535);

    public ConnectionManager(int udpPort, int tcpPort, BlockingQueue<RawResponse> responseQueue) throws IOException {
        this.responseQueue = responseQueue;
        this.selector = Selector.open();
        this.udpChannel = DatagramChannel.open();
        udpChannel.configureBlocking(false);
        udpChannel.bind(new InetSocketAddress(udpPort));
        udpChannel.register(selector, SelectionKey.OP_READ);

        this.tcpServerSocket = ServerSocketChannel.open();
        tcpServerSocket.configureBlocking(false);
        tcpServerSocket.bind(new InetSocketAddress(tcpPort));
        tcpServerSocket.register(selector, SelectionKey.OP_ACCEPT);

        logger.info("Сетевые ресурсы открыты");
    }

    public RawUDPRequest receive() throws IOException {
        if (selector.select() == 0) return null;

        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = keys.next();
            keys.remove();

            if (!key.isValid()) continue;

            if (key.isAcceptable()) {
                serverAccept();
                return null;
            }
            if (key.isReadable()) {
                if (key.channel() instanceof DatagramChannel) {
                    return clientRead(key);
                } else {
                    serverRead(key);
                    return null;
                }
            }
            if (key.isWritable()) {
                serverWrite(key);
                return null;
            }
        }
        return null;
    }

    private void serverAccept() {
        try {
            SocketChannel serverChannel = tcpServerSocket.accept();
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(65536));
            servers.add(serverChannel);
            serverQueue.put(serverChannel, new ConcurrentLinkedQueue<>());
            logger.info("Сервер {} подключен, создана пустая очередь на отправку", serverChannel.getRemoteAddress());
        } catch (IOException e) {
            logger.error("Ошибка соединения с сервером", e);
        }
    }

    private RawUDPRequest clientRead(SelectionKey key) throws IOException {
        DatagramChannel clientChannel = (DatagramChannel) key.channel();
        readBuffer.clear();
        SocketAddress clientAddress = clientChannel.receive(readBuffer);
        readBuffer.flip();
        byte[] bytes = new byte[readBuffer.remaining()];
        readBuffer.get(bytes);

        return new RawUDPRequest((InetSocketAddress) clientAddress, bytes);
    }

    public void clientSend(SocketAddress address, byte[] data) throws IOException {
        udpChannel.send(ByteBuffer.wrap(data), address);
    }

    private void serverRead(SelectionKey key) {
        SocketChannel serverChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        try {
            int read = serverChannel.read(buffer);
            if (read == -1) {
                logger.debug("Соединение разорвано");
                handleServerError(serverChannel, key);
                return;
            }
            responseQueue.put(Objects.requireNonNull(decode(buffer)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.error("Ошибка при отправке на сервер", e);
        }
    }

    private RawResponse decode(ByteBuffer buffer) {
        buffer.flip();
        if (buffer.remaining() < 4) {
            buffer.compact();
            return null;
        }

        buffer.mark();
        int length = buffer.getInt();

        if (buffer.remaining() < length) {
            buffer.reset();
            buffer.compact();
            return null;
        }

        byte[] data = new byte[length];
        buffer.get(data);
        buffer.compact();
        return new RawResponse(data);
    }

    private void serverWrite(SelectionKey key) {
        SocketChannel serverChannel = (SocketChannel) key.channel();
        Queue<ByteBuffer> queue = serverQueue.get(serverChannel);

        if (queue == null) return;

        try {
            while (!queue.isEmpty()) {
                ByteBuffer buffer = queue.peek();
                serverChannel.write(buffer);

                if (buffer.hasRemaining()) {
                    return;
                }
                queue.poll();
            }
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        } catch (IOException e) {
            logger.error("Ошибка при отправке серверу", e);
            handleServerError(serverChannel, key);
        }
    }

    public void serverSend(SocketChannel serverChannel, ByteBuffer buffer) {
        Queue<ByteBuffer> queue = serverQueue.get(serverChannel);

        queue.add(buffer);
        SelectionKey key = serverChannel.keyFor(selector);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    public SocketChannel getNextServer() {
        synchronized (servers) {
            if (servers.isEmpty()) {
                return null;
            }
        }
        int index = Math.abs(roundRobinCounter.getAndIncrement() % servers.size());
        return servers.get(index);
    }

    public void selectorWakeUp() {
        selector.wakeup();
    }

    private void retryToAnotherServer(ByteBuffer buffer) {
        buffer.rewind();

        SocketChannel nextServer = getNextServer();
        if (nextServer != null) {
            Queue<ByteBuffer> nextQueue = serverQueue.get(nextServer);
            nextQueue.add(buffer);

            SelectionKey nextKey = nextServer.keyFor(selector);
            nextKey.interestOps(nextKey.interestOps() | SelectionKey.OP_WRITE);
        } else {
            logger.error("Нет доступных серверов для отправки");
        }
    }

    private void handleServerError(SocketChannel serverChannel, SelectionKey key) {
        try {
            logger.debug("Очистка от сервера {}", serverChannel.getRemoteAddress());
            key.cancel();
            serverChannel.close();
        } catch (IOException e) {
            logger.error("Ошибка при закрытии канала", e);
        } finally {
            synchronized (servers) {
                servers.remove(serverChannel);
            }

            Queue<ByteBuffer> failedQueue = serverQueue.remove(serverChannel);

            if (!failedQueue.isEmpty()) {
                logger.warn("Найдены не отправленные сообщения, ищем другой сервер");
                for (ByteBuffer buffer: failedQueue) {
                    retryToAnotherServer(buffer);
                }
            }
        }
        logger.info("Очистка ресурсов сервера завершена");
    }

    @Override
    public void close() {
        logger.debug("Закрытие сетевых ресурсов");
        try {
            if (selector != null && selector.isOpen()) selector.close();
            if (tcpServerSocket != null && tcpServerSocket.isOpen()) tcpServerSocket.close();
            if (udpChannel != null && udpChannel.isOpen()) udpChannel.close();
        } catch (IOException e) {
            logger.error("Ошибка при закрытии сетевых ресурсов", e);
        }
    }
}
