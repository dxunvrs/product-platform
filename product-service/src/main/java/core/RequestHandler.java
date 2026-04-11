package core;

import commands.CommandContext;
import network.Request;
import network.Response;
import network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private final CommandManager commandManager;

    public RequestHandler(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    public Response handle(Request request) {
        switch (request.getType()) {
            case SYNC -> {
                return commandManager.syncCommands();
            }
            case SERVER_COMMAND -> {
                // int userId = authService.validateToken(request.getToken());
                return commandManager.executeCommand(new CommandContext(request, request.getUserId()));
            }
            default -> {
                return new Response.Builder().type(ResponseType.ERROR).message("Неизвестный тип запроса").build();
            }
        }
    }
}