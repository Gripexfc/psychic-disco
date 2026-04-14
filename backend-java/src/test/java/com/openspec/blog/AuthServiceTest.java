package com.openspec.blog;

import com.openspec.blog.dto.AuthToken;
import com.openspec.blog.model.User;
import com.openspec.blog.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "username", "testuser");
        ReflectionTestUtils.setField(authService, "password", "testpass");
        ReflectionTestUtils.setField(authService, "authToken", "test-token");
    }

    @Test
    void validateCredentials_correctCredentials() {
        assertTrue(authService.validateCredentials("testuser", "testpass"));
    }

    @Test
    void validateCredentials_wrongUsername() {
        assertFalse(authService.validateCredentials("wronguser", "testpass"));
    }

    @Test
    void validateCredentials_wrongPassword() {
        assertFalse(authService.validateCredentials("testuser", "wrongpass"));
    }

    @Test
    void validateToken_correctToken() {
        assertTrue(authService.validateToken("test-token"));
    }

    @Test
    void validateToken_wrongToken() {
        assertFalse(authService.validateToken("wrong-token"));
    }

    @Test
    void validateToken_emptyToken() {
        assertFalse(authService.validateToken(""));
    }

    @Test
    void login_correctCredentials_returnsToken() {
        AuthToken result = authService.login("testuser", "testpass");

        assertNotNull(result);
        assertEquals("test-token", result.getToken());
        assertEquals("testuser", result.getUser().getUsername());
    }

    @Test
    void login_wrongCredentials_returnsNull() {
        AuthToken result = authService.login("wronguser", "wrongpass");

        assertNull(result);
    }

    @Test
    void getCurrentUser_validToken_returnsUser() {
        User result = authService.getCurrentUser("test-token");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getCurrentUser_invalidToken_returnsNull() {
        User result = authService.getCurrentUser("invalid-token");

        assertNull(result);
    }
}
