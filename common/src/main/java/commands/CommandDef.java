package commands;

import java.util.List;

public record CommandDef(String name, String description, List<ArgType> expectedArgs) {
}