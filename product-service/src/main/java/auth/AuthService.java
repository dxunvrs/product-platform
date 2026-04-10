package auth;

import exceptions.InvalidAuthorizeException;
import db.DBManager;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final DBManager dbManager;
    private final JWTProvider jwtProvider = new JWTProvider();

    public AuthService(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public int register(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            logger.warn("Пользователь ввел пустое имя или пароль");
            throw new InvalidAuthorizeException("Имя или пароль не могут быть пустыми");
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        return dbManager.registerUser(username, hash); // throws InvalidAuthorizeException
    }

    public int login(String username, String password) {
        String hashFromDB = dbManager.getUserHash(username); // throws InvalidAuthorizeException

        if (!BCrypt.checkpw(password, hashFromDB)) {
            throw new InvalidAuthorizeException("Введен неверный пароль");
        }

        return dbManager.getUserId(username); // throws InvalidAuthorizeException
    }

    public String createToken(String username, int userId) {
        return jwtProvider.createToken(username, userId);
    }

    public int validateToken(String token) {
        return jwtProvider.validateToken(token);
    }
}
