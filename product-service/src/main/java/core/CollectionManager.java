package core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import exceptions.CommandExecutionException;
import exceptions.DBExecuteException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import models.Product;
import db.DBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionManager {
    private static final Logger logger = LoggerFactory.getLogger(CollectionManager.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final DBManager dbManager;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, String> redisConnection; // потокобезопасный, лок не нужен
    private final RedisCommands<String, String> redisCommands;

    private final String PRODUCTS_KEY = "products:all";
    private final String USER_STATS_KEY = "user:stats:";

    public CollectionManager(DBManager dbManager, RedisClient redisClient) {
        this.dbManager = dbManager;
        this.redisClient = redisClient;
        this.redisConnection = redisClient.connect();
        this.redisCommands = redisConnection.sync();
    }

    public String sort() {
        List<Product> productList = getCollection();
        Collections.sort(productList);
        return collectionToString(productList);
    }

    public String randomSort() {
        List<Product> productList = getCollection();
        Collections.shuffle(productList);
        return collectionToString(productList);
    }

    public void addProduct(Product product, int userId) {
        Product newProduct = dbManager.createProduct(product, userId);
        if (newProduct == null) throw new CommandExecutionException("Не удалось добавить продукт");

        try {
            String id = String.valueOf(newProduct.getId());
            String json = mapper.writeValueAsString(newProduct);

            redisCommands.multi();
            redisCommands.hset(PRODUCTS_KEY, id, json);
            redisCommands.hincrby(USER_STATS_KEY + userId, "total_price", newProduct.getPrice());
            redisCommands.hincrby(USER_STATS_KEY + userId, "count", 1);
            redisCommands.exec();

        } catch (JsonProcessingException e) {
            logger.error("Ошибка маппинга полей", e);
        }
        logger.info("Продукт с id={} добавлен в кэш, user_id={}", newProduct.getId(), userId);
    }

    public void removeProductById(int id, int userId) {
        try {
            dbManager.deleteProduct(id, userId);
        } catch (DBExecuteException e) {
            throw new CommandExecutionException(e.getMessage());
        }

        try {
            Product deletedProduct = mapper.readValue(redisCommands.hget(PRODUCTS_KEY, String.valueOf(id)), Product.class);
            redisCommands.multi();
            redisCommands.hdel(PRODUCTS_KEY, String.valueOf(id));
            redisCommands.hincrby(USER_STATS_KEY + userId, "total_price", -deletedProduct.getPrice());
            redisCommands.hincrby(USER_STATS_KEY + userId, "count", -1);
            redisCommands.exec();

        } catch (JsonProcessingException e) {
            logger.error("Ошибка чтения", e);
        }
        logger.info("Продукт с id={} удален из кэша, user_id={}", id, userId);
    }

    public void updateProductById(int id, Product newProduct, int userId) {
        try {
            dbManager.updateProduct(id, newProduct, userId);
        } catch (DBExecuteException e) {
            throw new CommandExecutionException(e.getMessage());
        }

        try {
            Product updatedProduct = mapper.readValue(redisCommands.hget(PRODUCTS_KEY, String.valueOf(id)), Product.class);
            updatedProduct.update(newProduct);
            String json = mapper.writeValueAsString(updatedProduct);
            int deltaPrice = newProduct.getPrice() - updatedProduct.getPrice();
            
            redisCommands.multi();
            redisCommands.hset(PRODUCTS_KEY, String.valueOf(id), json);
            redisCommands.hincrby(USER_STATS_KEY + userId, "total_price", deltaPrice);
            redisCommands.exec();
        } catch (JsonProcessingException e) {
            logger.error("Ошибка маппинга полей", e);
        }
        logger.info("Продукт с id={} обновлен в кэше, user_id={}", newProduct.getId(), userId);
    }

    public void clearCollection(int userId) {
        if (!dbManager.clearProducts(userId)) throw new CommandExecutionException("Ваша коллекция уже пуста");

        List<Product> productList = getCollection();
        String[] idsToDelete = productList.stream().filter(product -> product.getUserId() == userId)
                .map(product -> String.valueOf(product.getId())).toArray(String[]::new);

        if (idsToDelete.length == 0) {
            return;
        }

        redisCommands.multi();
        redisCommands.hdel(PRODUCTS_KEY, idsToDelete);
        redisCommands.del(USER_STATS_KEY + userId);
        redisCommands.exec();

        logger.info("Продукты user_id={} удалены из кэша", userId);
    }

    public int getSumOfPrice(int userId) {
        String sum = redisCommands.hget(USER_STATS_KEY + userId, "total_price");

        if (sum == null) return 0;
        return Integer.parseInt(sum);
    }

    public double getAvgOfPrice(int userId) {
        Map<String, String> stats = redisCommands.hgetall(USER_STATS_KEY + userId);
        long total = Long.parseLong(stats.getOrDefault("total_price", "0"));
        long count = Long.parseLong(stats.getOrDefault("count", "0"));

        if (count == 0) return 0;
        return (double) total / count;
    }

    public String getFormattedCollection(Predicate<Product> filter) {
        List<Product> productList = getCollection();
        return collectionToString(productList.stream().filter(filter).toList());
    }

    private String collectionToString(List<Product> products) {
        if (products.isEmpty()) {
            return "Коллекция пуста";
        }
        return products.stream().map(Product::toFormattedString).collect(Collectors.joining("\n"));
    }

    private List<Product> getCollection() {
        Map<String, String> products = redisCommands.hgetall(PRODUCTS_KEY);

        return new ArrayList<>(products.values().stream().map(json -> {
            try {
                return mapper.readValue(json, Product.class);
            } catch (JsonProcessingException e) {
                logger.error("Ошибка при чтении из Redis", e);
                return null;
            }
        }).filter(Objects::nonNull).toList());
    }

    public String getCollectionInfo() {
        return """
                  Информация о коллекции:
                  Тип: %s
                  Количество элементов: %s""".formatted("Redis HSET", redisCommands.hlen(PRODUCTS_KEY));
    }

    public void close() {
        logger.debug("Закрытие Redis-соединения");
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }
}
