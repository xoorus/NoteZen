package com.bchev.notezen.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessControlServiceTest {

    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        accessControlService = new AccessControlService();
    }

    @Test
    void isAuthorized_withAuthorizedEmail_shouldReturnTrue() {
        // When & Then
        assertTrue(accessControlService.isAuthorized("admin@notezen.fr"));
        assertTrue(accessControlService.isAuthorized("dev@notezen.fr"));
        assertTrue(accessControlService.isAuthorized("bchevriaut@gmail.com"));
    }

    @Test
    void isAuthorized_withUnauthorizedEmail_shouldReturnFalse() {
        // When & Then
        assertFalse(accessControlService.isAuthorized("hacker@example.com"));
        assertFalse(accessControlService.isAuthorized("unknown@example.com"));
    }

    @Test
    void isAuthorized_caseInsensitive_shouldReturnTrue() {
        // When & Then
        assertTrue(accessControlService.isAuthorized("ADMIN@NOTEZEN.FR"));
        assertTrue(accessControlService.isAuthorized("Admin@NoteZen.fr"));
        assertTrue(accessControlService.isAuthorized("BCHEVRIAUT@GMAIL.COM"));
    }

    @Test
    void isAuthorized_withWhitespace_shouldReturnFalse() {
        // When & Then
        assertFalse(accessControlService.isAuthorized(" admin@notezen.fr "));
        assertFalse(accessControlService.isAuthorized("admin@notezen.fr "));
    }
}
