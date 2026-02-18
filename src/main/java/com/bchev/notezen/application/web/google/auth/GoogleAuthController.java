package com.bchev.notezen.application.web.google.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login/oauth2/code")
public class GoogleAuthController {
/*
    private final GoogleAuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/google")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        // 1. Échanger le code contre les tokens
        GoogleTokenResponse tokens = authService.exchangeCodeForTokens(code);

        // 2. Récupérer l'utilisateur actuel (via session ou autre ID passé en state)
        // Ici on simplifie en prenant l'utilisateur connecté
        User user = getCurrentUser();

        // 3. Sauvegarder en base de données
        user.setGoogleAccessToken(tokens.getAccessToken());
        if (tokens.getRefreshToken() != null) {
            user.setGoogleRefreshToken(tokens.getRefreshToken());
        }
        userRepository.save(user);

        // 4. Rediriger l'utilisateur vers le Front-end
        response.sendRedirect("http://localhost:3000/dashboard?auth=success");
    }*/
}