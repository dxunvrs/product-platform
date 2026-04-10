package core;

import commands.CommandContext;
import exceptions.AuthExpiredException;
import exceptions.InvalidAuthorizeException;
import network.Request;
import network.Response;
import network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import auth.AuthService;

public class RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final CommandManager commandManager;
    private final AuthService authService;

    public RequestHandler(CommandManager commandManager, AuthService authService) {
        this.commandManager = commandManager;
        this.authService = authService;
    }

    public Response handle(Request request) {
        try {
            switch (request.getType()) {
                case SYNC -> {
                    return commandManager.syncCommands();
                }
                case SERVER_COMMAND -> {
                    int userId = authService.validateToken(request.getToken());
                    return commandManager.executeCommand(new CommandContext(request, userId));
                }
                case LOGIN -> {
                    int userId = authService.login(request.getUsername(), request.getPassword());

                    return new Response.Builder().type(ResponseType.AUTH_SUCCESS)
                            .message("Вы успешно вошли")
                            .token(authService.createToken(request.getUsername(), userId))
                            .build();
                }
                case REGISTER -> {
                    int userId = authService.register(request.getUsername(), request.getPassword());

                    return new Response.Builder().type(ResponseType.AUTH_SUCCESS)
                            .message("Вы успешно зарегистрировались")
                            .token(authService.createToken(request.getUsername(), userId))
                            .build();
                }
                default -> {
                    return new Response.Builder().type(ResponseType.ERROR).message("Неизвестный тип запроса").build();
                }
            }
        } catch (InvalidAuthorizeException e) {
            logger.error("Ошибка авторизации", e);
            return new Response.Builder().type(ResponseType.AUTH_FAILED).message(e.getMessage()).build();
        } catch (AuthExpiredException e) {
            logger.error("Ошибка валидации токена", e);
            return new Response.Builder().type(ResponseType.AUTH_REQUIRED).message(e.getMessage()).build();
        }
    }
}