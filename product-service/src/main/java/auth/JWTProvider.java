package auth;

import exceptions.AuthExpiredException;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JWTProvider {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().systemProperties().load();

    private static final long EXPIRATION_TIME = 60_000 * 5; // 5 минут
    private static final SecretKey key = Keys.hmacShaKeyFor(dotenv.get("JWT_SECRET").getBytes(StandardCharsets.UTF_8));

    public String createToken(String username, int userId) {
        return Jwts.builder().subject(username).claim("userId", userId)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public int validateToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.get("userId", Integer.class);
        } catch (Exception e){
            throw new AuthExpiredException("Пройдите авторизацию снова");
        }
    }
}
