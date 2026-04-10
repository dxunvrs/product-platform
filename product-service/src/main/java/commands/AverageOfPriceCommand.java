package commands;

import core.CollectionManager;

import java.util.List;

public class AverageOfPriceCommand extends Command {
    @Inject
    private CollectionManager collectionManager;

    public AverageOfPriceCommand() {
        super("average_of_price", "average_of_price - вывести среднее значение цены для всех элементов коллекции", List.of(ArgType.NONE));
    }

    @Override
    public CommandData execute(CommandContext context) {
        String responseMessage = String.format("Среднее значение цены для ваших элементов коллекции: %.2f",
                collectionManager.getAvgOfPrice(context.getCurrentUserId()));
        return new CommandData(responseMessage);
    }
}
