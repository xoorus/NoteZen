package com.bchev.notezen.application.web.testController;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String home(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
                       @AuthenticationPrincipal OAuth2User oauth2User) {

        // C'est CA que tu utiliseras pour tes appels API Reviews :
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        return "Bravo " + oauth2User.getAttribute("name") + " ! <br>" +
                "Ton Token (à ne pas afficher en prod) : " + accessToken;
    }
}