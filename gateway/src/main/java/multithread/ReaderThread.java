package multithread;

import auth.AuthClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import exceptions.InvalidAuthorizeException;
import io.jsonwebtoken.Claims;
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
    private final AuthClient authClient;


    public ReaderThread(BlockingQueue<RawUDPRequest> requestQueue, ConnectionManager connectionManager, AuthClient authClient) {
        this.requestQueue = requestQueue;
        this.connectionManager = connectionManager;
        this.authClient = authClient;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RawUDPRequest raw = requestQueue.take();
                logger.debug("Поток {} берет задачу чтения из очереди", Thread.currentThread().getName());
                Request request = mapper.readValue(raw.data(), Request.class);

                InetSocketAddress clientAddress = new InetSocketAddress(raw.address().getHostName(), raw.address().getPort());
                Request fullRequest = new Request.Builder(request)
                        .host(clientAddress.getHostName())
                        .port(clientAddress.getPort()).build();

                try {
                    if (request.getType() == RequestType.LOGIN) {
                        String token = authClient.auth("/login", request.getUsername(), request.getPassword());
                        if (token == null) {
                            return;
                        }
                        connectionManager.clientSend(clientAddress, mapper.writeValueAsBytes(
                                new Response.Builder().type(ResponseType.AUTH_SUCCESS).message("Вы успешно вошли").token(token).build()
                        ));
                        return;
                    } else if (request.getType() == RequestType.REGISTER) {
                        String token = authClient.auth("/register", request.getUsername(), request.getPassword());
                        if (token == null) {
                            return;
                        }
                        connectionManager.clientSend(clientAddress, mapper.writeValueAsBytes(
                                new Response.Builder().type(ResponseType.AUTH_SUCCESS).message("Вы успешно зарегистрировались").token(token).build()
                        ));
                        return;
                    } else if (request.getType() == RequestType.SERVER_COMMAND) {
                        try {
                            Claims claims = authClient.validate(fullRequest.getToken());
                            fullRequest = new Request.Builder(fullRequest).userId(Integer.parseInt(claims.getSubject())).build();
                        } catch (Exception e) {
                            connectionManager.clientSend(clientAddress, mapper.writeValueAsBytes(
                                    new Response.Builder().type(ResponseType.AUTH_REQUIRED).message("Пройдите авторизацию снова").build()
                            ));
                            return;
                        }
                    }
                } catch (InvalidAuthorizeException e) {
                    connectionManager.clientSend(clientAddress, mapper.writeValueAsBytes(
                            new Response.Builder().type(ResponseType.AUTH_FAILED).message(e.getMessage()).build()
                    ));
                    return;
                }

                SocketChannel currentServer = connectionManager.getNextServer();
                if (currentServer == null) {
                    logger.error("Нет доступных серверов");
                    connectionManager.clientSend(clientAddress,
                            mapper.writeValueAsBytes(new Response.Builder().
                                    type(ResponseType.ERROR).
                                    message("Нет доступных серверов").build()));
                    return;
                }
                byte[] requestBytes = mapper.writeValueAsBytes(fullRequest);
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
