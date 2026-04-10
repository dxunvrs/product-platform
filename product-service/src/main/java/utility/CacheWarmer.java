package utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import db.DBManager;
import io.github.cdimascio.dotenv.Dotenv;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import models.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CacheWarmer {
    private static final Logger logger = LoggerFactory.getLogger(CacheWarmer.class);
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().systemProperties().load();
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String PRODUCTS_KEY = "products:all";
    private static final String USER_STATS_KEY = "user:stats:";

    public static void main(String[] args) {
        try {
            DBManager dbManager = new DBManager(1, dotenv.get("DB_HOST"), Integer.parseInt(dotenv.get("DB_PORT")),
                    dotenv.get("DB_NAME"), dotenv.get("DB_USER"), dotenv.get("DB_PASS"));

            String redisUrl = String.format("redis://%s:%d", dotenv.get("REDIS_HOST"), Integer.parseInt(dotenv.get("REDIS_PORT")));
            RedisClient redisClient = RedisClient.create(redisUrl);

            StatefulRedisConnection<String, String> redisConnection = redisClient.connect();
            RedisCommands<String, String> redisCommands = redisConnection.sync();

            redisCommands.flushdb();
            logger.info("Кэш redis очищен");

            List<Product> productList = dbManager.loadCollection();
            if (productList == null || productList.isEmpty()) {
                return;
            }

            redisCommands.multi();
            for (Product product: productList) {
                String json = mapper.writeValueAsString(product);
                String id = String.valueOf(product.getId());
                String userId = String.valueOf(product.getUserId());

                redisCommands.hset(PRODUCTS_KEY, id, json);
                redisCommands.hincrby(USER_STATS_KEY + userId, "total_price", product.getPrice());
                redisCommands.hincrby(USER_STATS_KEY + userId, "count", 1);
            }
            redisCommands.exec();
            logger.info("Коллекция из {} элементов загружена в кэш", productList.size());

        } catch (Exception e) {
            System.exit(1);
        }
    }
}
