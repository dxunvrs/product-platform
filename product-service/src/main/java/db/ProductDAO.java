package db;

import models.Coordinates;
import models.Person;
import models.Product;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(Product.class)
public interface ProductDAO {

    @SqlQuery("SELECT * FROM products")
    List<Product> loadCollection();

    @SqlQuery("INSERT INTO products (name, x, y, price, unit_of_measure, owner_name, owner_birthday, owner_height, user_id) " +
            "VALUES (:name, :coordinates.x, :coordinates.y, :price, :unitOfMeasure::unit_of_measures, :owner.name, :owner.birthday, :owner.height, :userId) " +
            "RETURNING *")
    Product insert(@BindBean Product product,
                   @BindBean("coordinates") Coordinates coordinates,
                   @BindBean("owner") Person owner,
                   @Bind("userId") int userId);

    @SqlQuery("SELECT user_id FROM products WHERE id = :id")
    Optional<Integer> getOwnerId(@Bind("id") int id);

    @SqlUpdate("UPDATE products SET name = :name, x = :coordinates.x, y = :coordinates.y, price = :price, unit_of_measure = :unitOfMeasure::unit_of_measures," +
            "owner_name = :owner.name, owner_birthday = :owner.birthday, owner_height = :owner.height WHERE id = :id")
    int update(@BindBean Product product,
               @BindBean("coordinates") Coordinates coordinates,
               @BindBean("owner") Person owner,
               @Bind("id") int id);

    @SqlUpdate("DELETE FROM products WHERE id = :id")
    int delete(@Bind("id") int id);

    @SqlUpdate("DELETE FROM products WHERE user_id = :userId")
    int clear(@Bind("userId") int userId);
}
