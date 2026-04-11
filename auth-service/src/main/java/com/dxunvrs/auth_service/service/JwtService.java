package com.dxunvrs.auth_service.service;

import com.dxunvrs.auth_service.config.JwtKeyProvider;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.Date;

@Service
public class JwtService {
    private final PrivateKey privateKey;
    private final long expirationTime = 1000 * 60 * 5; // 15 минут

    public JwtService(JwtKeyProvider keyProvider) {
        this.privateKey = keyProvider.getPrivateKey();
    }

    public String generateToken(int userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
