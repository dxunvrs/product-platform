package com.dxunvrs.auth_service.service;

import com.dxunvrs.auth_service.exception.InvalidAuthorizeException;
import com.dxunvrs.auth_service.repository.UserDao;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserDao userDao;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserDao userDao, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String username, String password) {
        String hash = passwordEncoder.encode(password);

        try {
            int userId = userDao.register(username, hash);
            return jwtService.generateToken(userId, username);
        } catch (Exception e) {
            throw new InvalidAuthorizeException("Данное имя уже занято");
        }
    }

    public String login(String username, String password) {
        String hash = userDao.getUserHashByUsername(username)
                .orElseThrow(() -> new InvalidAuthorizeException("Нет такого пользователя"));

        if (!passwordEncoder.matches(password, hash)) {
            throw new InvalidAuthorizeException("Неверный пароль");
        }

        int userId = userDao.getIdByUsername(username)
                .orElseThrow(() -> new InvalidAuthorizeException("Нет такого пользователя"));
        return jwtService.generateToken(userId, username);
    }
}
