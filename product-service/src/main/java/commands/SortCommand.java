package commands;

import core.CollectionManager;

import java.util.List;

public class SortCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public SortCommand() {
        super("sort", "sort - сортировка коллекции в естественном порядке (по id)",
                List.of(ArgType.NONE));
    }

    @Override
    public CommandData execute(CommandContext context) {
        String responseMessage = "Отсортированная коллекция:\n" + collectionManager.sort();
        return new CommandData(responseMessage);
    }
}
