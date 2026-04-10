package network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPConnectionManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(TCPConnectionManager.class);

    private static final int MAX_ATTEMPTS = 3;
    private int reconnectAttempts = 0;

    private final Selector selector;
    private SocketChannel gatewayChannel;

    private final Queue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();

    private final String gatewayHost;
    private final int gatewayPort;

    private final Runnable stopHook;

    public TCPConnectionManager(String gatewayHost, int gatewayPort, Runnable stopHook) throws IOException {
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        this.selector = Selector.open();
        this.stopHook = stopHook;

        connect();

        logger.debug("Селектор открыт");
    }

    public void connect() {
        try {
            if (reconnectAttempts >= MAX_ATTEMPTS) {
                stopHook.run();
            }
            reconnectAttempts++;

            gatewayChannel = SocketChannel.open();
            gatewayChannel.configureBlocking(false);

            if (gatewayChannel.connect(new InetSocketAddress(gatewayHost, gatewayPort))) {
                gatewayChannel.register(selector, SelectionKey.OP_READ);
            } else {
                gatewayChannel.register(selector, SelectionKey.OP_CONNECT);
            }
        } catch (IOException e) {
            logger.error("Ошибка установки соединения с gateway", e);
        }
    }

    public RawTCPRequest receive() throws IOException {
        if (selector.select() == 0) return null;

        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = keys.next();
            keys.remove();

            if (!key.isValid()) continue;

            if (key.isConnectable()) {
                finishConnect(key);
                return null;
            }
            if (key.isWritable()) {
                write(key);
                return null;
            }
            if (key.isReadable()) return read(key);
        }
        return null;
    }

    private void finishConnect(SelectionKey key) {
        try {
            if (gatewayChannel.finishConnect()) {
                logger.info("TCP-соединение с gateway установлено");
                key.interestOps(SelectionKey.OP_READ);
                key.attach(ByteBuffer.allocate(65536));
                reconnectAttempts = 0;
            }
        } catch (IOException e) {
            logger.error("Ошибка установки соединения с gateway", e);
            disconnect(key);
        }
    }

    private RawTCPRequest read(SelectionKey key) {
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        try {
            int read = gatewayChannel.read(buffer);
            if (read == -1) {
                logger.debug("Соединение разорвано");
                disconnect(key);
                return null;
            }
            return decode(buffer);
        } catch (IOException e) {
            logger.error("Ошибка чтения", e);
            disconnect(key);
            return null;
        }
    }

    private RawTCPRequest decode(ByteBuffer buffer) {
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
        return new RawTCPRequest(data);
    }

    public void send(ByteBuffer buffer) throws IOException {
       queue.add(buffer);

       SelectionKey key = gatewayChannel.keyFor(selector);
       key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
       selector.wakeup();
    }

    private void write(SelectionKey key) {
        try {
            while (!queue.isEmpty()) {
                ByteBuffer buffer = queue.peek();
                gatewayChannel.write(buffer);

                if (buffer.hasRemaining()) {
                    return;
                }
                queue.poll();
            }
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        } catch (IOException e) {
            logger.error("Ошибка отправки");
            disconnect(key);
        }
    }

    public void selectorWakeUp() {
        selector.wakeup();
    }

    private void disconnect(SelectionKey key) {
        try {
            key.cancel();
            gatewayChannel.close();
            Thread.sleep(5000);
            logger.debug("Попытка переподключения");
            connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.error("Ошибка закрытия сетевых ресурсов", e);
        }
    }

    @Override
    public void close() {
        logger.debug("Закрытие сетевых ресурсов");
        try {
            if (selector != null && selector.isOpen()) selector.close();
            if (gatewayChannel != null && gatewayChannel.isOpen()) gatewayChannel.close();
        } catch (IOException e) {
            logger.error("Ошибка при закрытии сетевых ресурсов", e);
        }
    }
}
