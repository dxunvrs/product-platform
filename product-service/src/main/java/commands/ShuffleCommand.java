package commands;

import core.CollectionManager;

import java.util.List;

public class ShuffleCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public ShuffleCommand() {
        super("shuffle", "shuffle - перемешать коллекцию в случайном порядке",
                List.of(ArgType.NONE));
    }

    @Override
    public CommandData execute(CommandContext context) {
        String responseMessage = "Перемешанная коллекция:\n" + collectionManager.randomSort();
        return new CommandData(responseMessage);
    }
}
