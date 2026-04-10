package commands;

import core.CollectionManager;

import java.util.List;

public class ClearCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public ClearCommand() {
        super("clear", "clear - очистить коллекцию", List.of(ArgType.NONE));
    }

    @Override
    public CommandData execute(CommandContext context) {
        collectionManager.clearCollection(context.getCurrentUserId());
        return new CommandData("Коллекция очищена");
    }
}
