package multithread;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import network.Response;
import network.TCPConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class SenderThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SenderThread.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final BlockingQueue<Response> responseQueue;
    private final TCPConnectionManager connectionManager;

    public SenderThread(BlockingQueue<Response> responseQueue, TCPConnectionManager connectionManager) {
        this.responseQueue = responseQueue;
        this.connectionManager = connectionManager;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Response response = responseQueue.take();
                logger.debug("Поток {} берет задачу отправки из очереди", Thread.currentThread().getName());
                byte[] responseBytes = mapper.writeValueAsBytes(response);

                ByteBuffer buffer = ByteBuffer.allocate(4 + responseBytes.length);
                buffer.putInt(responseBytes.length);
                buffer.put(responseBytes);
                buffer.flip();

                connectionManager.send(buffer);
                logger.info("Отправлен ответ, вес: {} байт, сообщение: {}", responseBytes.length, new String(responseBytes));
            } catch (JsonProcessingException e) {
                logger.error("Ошибка маппинга", e);
            } catch (IOException e) {
                logger.error("Ошибка отправки", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.debug("Поток отправки {} закрылся", Thread.currentThread().getName());
    }
}
