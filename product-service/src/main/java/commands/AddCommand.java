package commands;

import core.CollectionManager;

import java.util.List;

public class AddCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public AddCommand() {
        super("add", "add - добавление нового элемента", List.of(ArgType.OBJECT));
    }

    @Override
    public CommandData execute(CommandContext context) {
        collectionManager.addProduct(context.getObjectArgs().get(0), context.getCurrentUserId());
        return new CommandData("Продукт добавлен");
    }
}
