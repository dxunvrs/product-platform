package db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exceptions.DBExecuteException;
import models.Product;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DBManager {
    private static final Logger logger = LoggerFactory.getLogger(DBManager.class);

    private final HikariDataSource dataSource;
    private final Jdbi orm;

    public DBManager(int connectionCount, String host, int port, String db, String user, String pass) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, db));
        config.setUsername(user);
        config.setPassword(pass);

        config.setMaximumPoolSize(connectionCount);

        dataSource = new HikariDataSource(config);
        orm = Jdbi.create(dataSource); // интеграция HikariCP в ORM
        orm.installPlugin(new SqlObjectPlugin());
    }

    public List<Product> loadCollection() {
        try {
            return orm.withExtension(ProductDAO.class, ProductDAO::loadCollection);
        } catch (Exception e) {
            logger.error("Ошибка загрузки коллекции из БД", e);
            return null;
        }
    }

    // возвращаем product с назначенными id, creationDate и userId
    public Product createProduct(Product product, int userId) {
        try {
            Product created = orm.withExtension(ProductDAO.class, dao -> dao.insert(product, product.getCoordinates(), product.getOwner(), userId));
            logger.info("В БД добавлен продукт {}", product);
            return created;
        } catch (Exception e) {
            logger.error("Ошибка при добавлении продукта", e);
            return null;
        }
    }

    public void updateProduct(int id, Product product, int userId) {
        orm.useTransaction(handle -> {
            ProductDAO dao = handle.attach(ProductDAO.class);

            Integer actualUserId = dao.getOwnerId(id).orElseThrow(() -> new DBExecuteException("Нет такого id"));

            if (actualUserId != userId) {
                throw new DBExecuteException("Недостаточно прав");
            }

            if (dao.update(product, product.getCoordinates(), product.getOwner(), id) > 0) {
                logger.info("Продукт с id {} обновлен в БД", id);
            } else {
                throw new DBExecuteException("Не удалось обновить продукт");
            }
        });
    }

    public void deleteProduct(int id, int userId) {
        orm.useTransaction(handle -> {
            ProductDAO dao = handle.attach(ProductDAO.class);

            Integer actualUserId = dao.getOwnerId(id).orElseThrow(() -> new DBExecuteException("Нет такого id"));

            if (actualUserId != userId) {
                throw new DBExecuteException("Недостаточно прав");
            }

            if (dao.delete(id) > 0) {
                logger.info("Продукт с id {} удален из БД", id);
            } else {
                throw new DBExecuteException("Не удалось удалить продукт");
            }
        });
    }

    public boolean clearProducts(int userId) {
        int deleted = orm.withExtension(ProductDAO.class, dao -> dao.clear(userId));
        if (deleted > 0) {
            logger.info("Продукты пользователя {} удалены", userId);
            return true;
        }
        return false;
    }

    public void close() {
        dataSource.close();
    }
}