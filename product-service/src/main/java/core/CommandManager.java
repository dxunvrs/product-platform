package core;

import commands.*;
import exceptions.CommandExecutionException;
import network.Response;
import network.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final Map<String, Command> commands = new HashMap<>();

    private final CollectionManager collectionManager;

    public CommandManager(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
        registerAllCommands();
    }

    public Response executeCommand(CommandContext commandContext) {
        Command command = commands.get(commandContext.getCommandName());
        if (command == null) {
            logger.warn("Команда не найдена");
            return new Response.Builder().type(ResponseType.OUTDATED).message("Данная команда не поддерживается").build();
        }
        try {
            logger.debug("Выполнение команды {}", command.getName());
            CommandData commandData = command.execute(commandContext);
            return new Response.Builder().type(ResponseType.OK).message(commandData.message()).build();
        } catch (CommandExecutionException e) {
            return handleError(e.getMessage(), e);
        } catch (Exception e) {
            return handleError("Неизвестная ошибка", e);
        }
    }

    private Response handleError(String message, Exception e) {
        logger.error(message, e);
        return new Response.Builder().type(ResponseType.ERROR).message(e.getMessage()).build();
    }

    public Response syncCommands() {
        Map<String, CommandDef> commandDefMap = new HashMap<>();
        commands.forEach((name, serverCommand) ->
                commandDefMap.put(name, new CommandDef(serverCommand.getName(), serverCommand.getDescription(), serverCommand.getExpectedArgs()))
        );
        return new Response.Builder().type(ResponseType.SYNC_DATA)
                .message("Актуальные команды")
                .syncData(commandDefMap).build();
    }

    public void addCommand(Command command) {
        logger.debug("Регистрация новой команды: {}", command.getName());
        Field[] fields = command.getClass().getDeclaredFields();

        for (Field field: fields) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object toInject = resolveDependency(field.getType());
                if (toInject == null) {
                    continue;
                }
                field.set(command, toInject);
                logger.debug("В команду {} внедрен {}", command.getName(), field.getType().getSimpleName());

            } catch (IllegalAccessException e) {
                logger.error("Не удалось внедрить зависимость в поле {}", field.getName(), e);
            }
        }
        commands.put(command.getName(), command);
        logger.info("Команда {} зарегистрирована", command.getName());
    }

    private Object resolveDependency(Class<?> type) {
        return switch (type.getSimpleName()) {
            case "CollectionManager" -> collectionManager;
            case "CommandManager" -> this;
            default -> null;
        };
    }

    private void registerAllCommands() {
        addCommand(new AddCommand());
        addCommand(new AverageOfPriceCommand());
        addCommand(new ClearCommand());
        addCommand(new FilterStartsWithNameCommand());
        addCommand(new InfoCommand());
        addCommand(new RemoveCommand());
        addCommand(new ShowCommand());
        addCommand(new ShuffleCommand());
        addCommand(new SortCommand());
        addCommand(new SumOfPriceCommand());
        addCommand(new UpdateCommand());
    }
}
