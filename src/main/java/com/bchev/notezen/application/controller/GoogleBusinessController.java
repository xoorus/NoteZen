package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.infrastructure.external.google.GoogleBusinessService;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.User;
import com.bchev.notezen.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GoogleBusinessController {

    private final GoogleAuthManager authManager;
    private final GoogleBusinessService googleClient;
    private final UserRepository userRepository;

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        String email = authManager.linkAccount(code);
        return ResponseEntity.ok("Compte lié : " + email);
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getLocations(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = authManager.getValidToken(user);
        return ResponseEntity.ok(googleClient.fetchLocations(user.getGoogleAccountId(), token));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getReviews(@RequestParam String email, @RequestParam String locationId) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = authManager.getValidToken(user);
        List<ReviewDTO> dtos = googleClient.fetchReviews(user.getGoogleAccountId(), locationId, token);
        return ResponseEntity.ok(dtos.stream().map(ReviewDTO::toReview).toList());
    }
}