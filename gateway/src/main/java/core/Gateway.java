package core;

import io.github.cdimascio.dotenv.Dotenv;
import multithread.ReaderThread;
import multithread.SenderThread;
import network.ConnectionManager;
import network.RawResponse;
import network.RawUDPRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Gateway {
    private static final Logger logger = LoggerFactory.getLogger(Gateway.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().systemProperties().load();

    private volatile boolean isWorking = true;
    private volatile boolean isShuttingDown = false;

    private ConnectionManager connectionManager;

    private final BlockingQueue<RawUDPRequest> requestQueue = new LinkedBlockingQueue<>(10);
    private final BlockingQueue<RawResponse> responseQueue = new LinkedBlockingQueue<>(10);

    private final ForkJoinPool readPool = new ForkJoinPool();
    private final ForkJoinPool sendPool = new ForkJoinPool();

    public void launch() {
        try {
            connectionManager = new ConnectionManager(Integer.parseInt(dotenv.get("GATEWAY_CLIENT_PORT")),
                    Integer.parseInt(dotenv.get("GATEWAY_SERVER_PORT")),
                    responseQueue);

            // инициализируем потоки-воркеры
            for (int i = 0; i < 10; i++) {
                readPool.execute(new ReaderThread(requestQueue, connectionManager));
            }
            for (int i = 0; i < 10; i++) {
                sendPool.execute(new SenderThread(responseQueue, connectionManager));
            }

            startConsoleThread();
            Runtime.getRuntime().addShutdownHook(new Thread(this::gatewayShutdown));

            runMainLoop();
        } catch (IOException e) {
            logger.error("Ошибка", e);
        }
    }

    private void runMainLoop() {
        logger.info("Gateway запущен");

        while (isWorking) {
            try {
                RawUDPRequest raw = connectionManager.receive();
                if (raw == null) continue;

                requestQueue.put(raw);

            } catch (InterruptedException e) {
                logger.error("Не удалось создать задачу чтения", e);
                isWorking = false;
                Thread.currentThread().interrupt();
            }
            catch (IOException e) {
                logger.error("Ошибка", e);
            }
        }
        gatewayShutdown();
    }

    private void startConsoleThread() {
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (!Thread.currentThread().isInterrupted()) {
                if (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.equals("exit")) {
                        stop();
                    } else {
                        System.out.println("Gateway поддерживает только exit");
                    }
                }
            }
        });
        consoleThread.setDaemon(true); // Чтобы поток не мешал закрытию программы
        consoleThread.start();
    }

    private void stop() {
        logger.debug("Закрытие gateway");
        isWorking = false;

        connectionManager.selectorWakeUp();
    }

    private void gatewayShutdown() {
        if (isShuttingDown) {
            return;
        }
        isShuttingDown = true;

        connectionManager.close();

        readPool.shutdown();
        sendPool.shutdown();

        try {
            if (!readPool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Пул чтения не успел закрыться, принудительная остановка");
                readPool.shutdownNow();
            }
            if (!sendPool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Пул отправки не успел закрыться, принудительная остановка");
                sendPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            readPool.shutdownNow();
            sendPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.debug("Все пулы потоков закрыты");
    }
}