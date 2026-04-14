package com.openspec.blog.service;

import com.openspec.blog.dto.AuthToken;
import com.openspec.blog.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Value("${auth.username:goodeat}")
    private String username;

    @Value("${auth.password:goodeat123456}")
    private String password;

    @Value("${auth.token:goodeat-session-token}")
    private String authToken;

    public boolean validateCredentials(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    public boolean validateToken(String token) {
        return authToken.equals(token);
    }

    public AuthToken login(String username, String password) {
        if (!validateCredentials(username, password)) {
            return null;
        }
        return new AuthToken(authToken, new User(this.username));
    }

    public User getCurrentUser(String token) {
        if (!validateToken(token)) {
            return null;
        }
        return new User(username);
    }
}
