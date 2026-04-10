package commands;

import core.CollectionManager;

import java.util.List;

public class SumOfPriceCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public SumOfPriceCommand() {
        super("sum_of_price", "sum_of_price - вывести сумму цен всех элементов коллекции",
                List.of(ArgType.NONE));
    }

    @Override
    public CommandData execute(CommandContext context) {
        String responseMessage = "Сумма цен ваших элементов коллекции: " +
                collectionManager.getSumOfPrice(context.getCurrentUserId());
        return new CommandData(responseMessage);
    }
}
