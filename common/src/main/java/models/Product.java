package models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.util.Date;

/**
 * Модель для продукта, данная по заданию
 */
@JsonPropertyOrder({"id", "name", "coordinates", "creationDate", "price", "unitOfMeasure", "owner"})
public class Product implements Comparable<Product> {
    private Integer id; //Поле не может быть null, Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private String name; //Поле не может быть null, Строка не может быть пустой

    @JsonUnwrapped(prefix = "coordinates_")
    private Coordinates coordinates; //Поле не может быть null

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss")
    private Date creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически

    private int price; //Значение поля должно быть больше 0
    private UnitOfMeasure unitOfMeasure; //Поле не может быть null

    @JsonUnwrapped(prefix = "owner_")
    private Person owner; //Поле не может быть null

    private int userId;

    public Product() {}

    public Product(String name, Coordinates coordinates, int price, UnitOfMeasure unitOfMeasure, Person owner) {
        this.name = name;
        this.coordinates = coordinates;
        this.price = price;
        this.unitOfMeasure = unitOfMeasure;
        this.owner = owner;
    }

    public Product(int id, String name, Coordinates coordinates, Date creationDate, int price, UnitOfMeasure unitOfMeasure, Person owner) {
        this(name, coordinates, price, unitOfMeasure, owner);
        this.id = id;
        this.creationDate = creationDate;
    }

    @ColumnName("id")
    public void setId(Integer id) {
        this.id = id;
    }
    public Integer getId() {
        return id;
    }

    @ColumnName("name")
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    @ColumnName("creation_date")
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public Date getCreationDate() {
        return creationDate;
    }

    @Nested
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }
    public Coordinates getCoordinates() {
        return coordinates;
    }

    @ColumnName("price")
    public void setPrice(int price) {
        this.price = price;
    }
    public int getPrice() {
        return price;
    }

    @ColumnName("unit_of_measure")
    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    @Nested("owner")
    public void setOwner(Person owner) {
        this.owner = owner;
    }
    public Person getOwner() {
        return owner;
    }

    @ColumnName("user_id")
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public int getUserId() {
        return userId;
    }

    public void update(Product product) {
        this.name = product.getName();
        this.coordinates = product.getCoordinates();
        this.price = product.getPrice();
        this.unitOfMeasure = product.getUnitOfMeasure();
        this.owner = product.getOwner();
    }

    @Override
    public int compareTo(Product other) {
        return Integer.compare(this.id, other.getId());
    }

    public String toFormattedString() {
        return """
               Продукт №%d
                 Название: %s
                 Координаты: (%d, %d)
                 Дата создания: %s
                 Цена: %d
                 Единица измерения: %s
                 Имя владельца: %s
                 День рождения владельца: %s
                 Рост владельца: %d""".formatted(id, name, coordinates.getX(), coordinates.getY(),
                creationDate, price, unitOfMeasure.name(), owner.getName(), owner.getBirthday(), owner.getHeight());
    }
}