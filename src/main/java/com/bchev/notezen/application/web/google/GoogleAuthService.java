package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.dto.GoogleTokenResponseDTO;

public interface GoogleAuthService {
    String getAuthorizationUrl();
    GoogleTokenResponseDTO exchangeCodeForTokens(String code);
    String refreshAccessToken(String refreshToken);
    String extractEmailFromToken(String idToken);
}
