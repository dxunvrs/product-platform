package models;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class Coordinates {
    private Long x;
    private int y;

    public Coordinates() {}

    public Coordinates(Long x, int y) {
        this.x = x;
        this.y = y;
    }

    @ColumnName("x")
    public void setX(Long x) {
        this.x = x;
    }
    public Long getX() {
        return x;
    }

    @ColumnName("y")
    public void setY(int y) {
        this.y = y;
    }
    public int getY() {
        return y;
    }
}
