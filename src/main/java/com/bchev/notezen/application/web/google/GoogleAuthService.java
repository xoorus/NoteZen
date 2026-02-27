package com.bchev.notezen.application.web.google;

import com.bchev.notezen.application.controller.DTO.GoogleTokenResponseDTO;

import java.util.List;
import java.util.Map;

public interface GoogleAuthService {
    String getAuthorizationUrl();
    GoogleTokenResponseDTO exchangeCodeForTokens(String code);
    String refreshAccessToken(String refreshToken);
}
