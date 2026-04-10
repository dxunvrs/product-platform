package commands;

import core.CollectionManager;

import java.util.List;

public class RemoveCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public RemoveCommand() {
        super("remove_by_id", "remove_by_id - удалить элемент из коллекции по id",
                List.of(ArgType.INT));
    }

    @Override
    public CommandData execute(CommandContext context) {
        collectionManager.removeProductById(context.getIntArgs().get(0), context.getCurrentUserId());
        String responseMessage = "Продукт с id=" + context.getIntArgs().get(0) + " удален";
        return new CommandData(responseMessage);
    }
}
