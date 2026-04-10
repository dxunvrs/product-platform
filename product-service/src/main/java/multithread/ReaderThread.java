package multithread;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import network.RawTCPRequest;
import network.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class ReaderThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ReaderThread.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final BlockingQueue<RawTCPRequest> requestQueue;
    private final BlockingQueue<Request> processQueue;

    public ReaderThread(BlockingQueue<RawTCPRequest> requestQueue, BlockingQueue<Request> processQueue) {
        this.requestQueue = requestQueue;
        this.processQueue = processQueue;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RawTCPRequest raw = requestQueue.take();
                logger.debug("Поток {} берет задачу чтения из очереди", Thread.currentThread().getName());
                Request request = mapper.readValue(raw.data(), Request.class);
                processQueue.put(request);
                logger.info("Получен новый запрос, вес: {} байт, сообщение: {}", raw.data().length, new String(raw.data()));
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
