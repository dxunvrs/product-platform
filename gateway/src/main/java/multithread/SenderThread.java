package multithread;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import network.ConnectionManager;
import network.RawResponse;
import network.Response;
import network.ResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;

public class SenderThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SenderThread.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final BlockingQueue<RawResponse> responseQueue;
    private final ConnectionManager connectionManager;

    public SenderThread(BlockingQueue<RawResponse> responseQueue, ConnectionManager connectionManager) {
        this.responseQueue = responseQueue;
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RawResponse raw = responseQueue.take();
                logger.debug("Поток {} берет задачу отправки из очереди", Thread.currentThread().getName());
                Response response = mapper.readValue(raw.data(), Response.class);
                InetSocketAddress clientAddress = new InetSocketAddress(response.getHost(), response.getPort());
                connectionManager.clientSend(new ResponsePacket(clientAddress, raw.data()));
                logger.info("От сервера получен ответ, перенаправление клиенту {}", clientAddress);
            } catch (IOException e) {
                logger.error("Ошибка отправки", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.debug("Поток чтения {} закрылся", Thread.currentThread().getName());
    }
}
