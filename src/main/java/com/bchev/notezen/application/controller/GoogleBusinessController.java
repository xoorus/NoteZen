package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.application.web.google.GoogleAuthService;
import com.bchev.notezen.domain.exception.UnauthorizedUserAccess;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.repository.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/google")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class GoogleBusinessController {

    private final GoogleAuthManager googleAuthManager;
    private final GoogleAuthService googleAuthService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {

        log.info("callback");
        try {
            UserEntity user = googleAuthManager.linkAccount(code);
            String noteZenToken = TokenUtils.generateToken(user.getId());
            String frontendUrl = "http://localhost:4200/dashboard?token=" + noteZenToken;
            response.sendRedirect(frontendUrl);
            return;
        } catch (UnauthorizedUserAccess e) {
            response.sendRedirect("http://localhost:4200/unauthorized?email=" + e.getEmail());
            return;
        }
        catch (Exception e) {
            log.error(e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
            response.sendRedirect("http://localhost:4200/login?error=auth_failed");
            return;
        }
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl() {
        if (isLocalProfileActive()) {
            // En local, on renvoie une URL qui pointe directement vers notre callback
            // avec un faux code, pour simuler la fin du processus
            String mockCallbackUrl = "http://localhost:8080/api/google/callback?code=mock-code";
            return ResponseEntity.ok(Map.of("url", mockCallbackUrl));
        }
        return ResponseEntity.ok(Map.of("url", googleAuthService.getAuthorizationUrl()));
    }

    private boolean isLocalProfileActive() {
        return "local".equals(activeProfile);
    }

}