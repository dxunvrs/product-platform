package com.dxunvrs.auth_service.controller;

import com.dxunvrs.auth_service.dto.AuthRequest;
import com.dxunvrs.auth_service.dto.AuthResponse;
import com.dxunvrs.auth_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest authRequest) {
        String token = authService.register(authRequest.getUsername(), authRequest.getPassword());

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        String token = authService.login(authRequest.getUsername(), authRequest.getPassword());

        return ResponseEntity.ok(new AuthResponse(token));
    }
}
