package commands;

public enum ArgType {
    STR(1),
    INT(1),
    OBJECT(0),
    NONE(0);

    private final int weight;

    ArgType(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
