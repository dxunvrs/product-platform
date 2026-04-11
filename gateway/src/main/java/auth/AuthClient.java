package auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import exceptions.InvalidAuthorizeException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AuthClient {
    private static final Logger logger = LoggerFactory.getLogger(AuthClient.class);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String authServiceUrl;

    private final PublicKey publicKey;

    public AuthClient(String authServiceUrl, String publicKeyPem) throws Exception {
        this.authServiceUrl = authServiceUrl;
        this.client = new OkHttpClient();
        this.publicKey = loadPublicKey(publicKeyPem);
    }

    public String auth(String path, String username, String password) {
        String jsonPayload;
        try {
            jsonPayload = mapper.writeValueAsString(new AuthRequest(username, password));
        } catch (JsonProcessingException e) {
            return null;
        }
        RequestBody requestBody = RequestBody.create(jsonPayload, JSON);

        Request request = new Request.Builder().url(authServiceUrl + path).post(requestBody).build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody;
            responseBody = response.body().string();

            if (response.isSuccessful()) {
                AuthResponse res = mapper.readValue(responseBody, AuthResponse.class);
                return res.token();
            } else {
                AuthResponse errorRes = mapper.readValue(responseBody, AuthResponse.class);
                throw new InvalidAuthorizeException(errorRes.error());
            }
        } catch (IOException e) {
            return null;
        }
    }

    private PublicKey loadPublicKey(String key) throws Exception {
        String cleanKey = key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public Claims validate(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
