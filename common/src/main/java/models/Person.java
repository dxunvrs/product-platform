package models;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.LocalDate;

@JsonPropertyOrder({"name", "birthday", "height"})
public class Person {
    private String name;
    private LocalDate birthday;
    private Long height;

    public Person() {}

    public Person(String name, LocalDate birthday, Long height) {
        this.name = name;
        this.birthday = birthday;
        this.height = height;
    }

    @ColumnName("name")
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    @ColumnName("birthday")
    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }
    public LocalDate getBirthday() {
        return birthday;
    }

    @ColumnName("height")
    public void setHeight(Long height) {
        this.height = height;
    }
    public Long getHeight() {
        return height;
    }
}