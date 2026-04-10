package commands;

import models.Product;
import network.Request;

import java.util.List;

public class CommandContext {
    private final int currentUserId;
    private final String commandName;

    private final List<String> stringArgs;
    private final List<Integer> intArgs;
    private final List<Product> objectArgs;

    public CommandContext(Request request, int currentUserId) {
        this.currentUserId = currentUserId;
        this.commandName = request.getCommandName();
        this.stringArgs = request.getStringArgs();
        this.intArgs = request.getIntArgs();
        this.objectArgs = request.getObjectArgs();
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getStringArgs() {
        return stringArgs;
    }

    public List<Integer> getIntArgs() {
        return intArgs;
    }

    public List<Product> getObjectArgs() {
        return objectArgs;
    }
}