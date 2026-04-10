package commands;

import java.util.List;

public abstract class Command {
    private final String name;
    private final String description;
    private final List<ArgType> expectedArgs;

    public Command(String name, String description, List<ArgType> expectedArgs) {
        this.name = name;
        this.description = description;
        this.expectedArgs = expectedArgs;
    }

    public abstract CommandData execute(CommandContext context);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ArgType> getExpectedArgs() {
        return expectedArgs;
    }
}
