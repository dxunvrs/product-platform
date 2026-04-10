package commands;

import core.CollectionManager;

import java.util.List;

public class UpdateCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public UpdateCommand() {
        super("update", "update id - обновить значение элемента по заданному id",
                List.of(ArgType.INT, ArgType.OBJECT));
    }

    @Override
    public CommandData execute(CommandContext context) {
        collectionManager.updateProductById(context.getIntArgs().get(0), context.getObjectArgs().get(0), context.getCurrentUserId());
        String responseMessage = "Продукт с id=" + context.getIntArgs().get(0) + " обновлен";
        return new CommandData(responseMessage);
    }
}
