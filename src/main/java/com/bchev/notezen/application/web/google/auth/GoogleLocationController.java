package com.bchev.notezen.application.web.google.auth;

import com.bchev.notezen.core.google.GoogleReviewManager;
import com.bchev.notezen.repository.google.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
public class GoogleLocationController {

    private final GoogleAuthService googleAuthService;

    public GoogleLocationController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getLocations(@AuthenticationPrincipal User user) {
        // On récupère les tokens de l'utilisateur connecté
        String accessToken = user.getGoogleAccessToken();
        String accountId = user.getGoogleAccountId();

        List<Map<String, Object>> locations = googleAuthService.getGoogleLocations(accountId, accessToken);
        return ResponseEntity.ok(locations);
    }

}