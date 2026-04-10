package multithread;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

public class ReaderThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ReaderThread.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final BlockingQueue<RawUDPRequest> requestQueue;
    private final ConnectionManager connectionManager;

    public ReaderThread(BlockingQueue<RawUDPRequest> requestQueue, ConnectionManager connectionManager) {
        this.requestQueue = requestQueue;
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RawUDPRequest raw = requestQueue.take();
                logger.debug("Поток {} берет задачу чтения из очереди", Thread.currentThread().getName());
                Request request = mapper.readValue(raw.data(), Request.class);
                InetSocketAddress clientAddress = new InetSocketAddress(raw.address().getHostName(), raw.address().getPort());
                Request requestWithAddress = new Request.Builder(request)
                        .host(clientAddress.getHostName())
                        .port(clientAddress.getPort()).build();

                SocketChannel currentServer = connectionManager.getNextServer();
                if (currentServer == null) {
                    logger.error("Нет доступных серверов");
                    connectionManager.clientSend(clientAddress,
                            mapper.writeValueAsBytes(new Response.Builder().
                                    type(ResponseType.ERROR).
                                    message("Нет доступных серверов").build()));
                    return;
                }
                byte[] requestBytes = mapper.writeValueAsBytes(requestWithAddress);
                ByteBuffer buffer = ByteBuffer.allocate(4 + requestBytes.length);
                buffer.putInt(requestBytes.length);
                buffer.put(requestBytes);
                buffer.flip();
                connectionManager.serverSend(currentServer, buffer);
                logger.info("Запрос перенаправлен на {}", currentServer.getRemoteAddress());
            } catch (JsonProcessingException e) {
                logger.error("Ошибка маппинга", e);
            } catch (IOException e) {
                logger.error("Ошибка парсинга запроса", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.debug("Поток чтения {} закрылся", Thread.currentThread().getName());
    }
}
