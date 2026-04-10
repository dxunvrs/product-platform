package commands;

import core.CollectionManager;

import java.util.List;

public class ShowCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public ShowCommand() {
        super("show", "show - вывод элементов коллекции",
                List.of(ArgType.NONE));
    }

    @Override
    public CommandData execute(CommandContext context) {
        String responseMessage = collectionManager.getFormattedCollection(product -> true);
        return new CommandData(responseMessage);
    }
}
