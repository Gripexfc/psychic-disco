package com.openspec.blog.controller;

import com.openspec.blog.dto.ApiResponse;
import com.openspec.blog.dto.AuthToken;
import com.openspec.blog.dto.LoginRequest;
import com.openspec.blog.model.User;
import com.openspec.blog.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponse<AuthToken>> login(@RequestBody LoginRequest request) {
        AuthToken token = authService.login(
                request.getUsername(),
                request.getPassword()
        );
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("INVALID_CREDENTIALS", "账号或密码错误。"));
        }
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<ApiResponse<User>> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        User user = authService.getCurrentUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("UNAUTHORIZED", "登录态无效，请重新登录。"));
        }
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Object> logout() {
        return ApiResponse.ok(Map.of("success", true));
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "";
        }
        return authHeader.substring(7).trim();
    }
}
