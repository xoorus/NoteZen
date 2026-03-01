package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import com.bchev.notezen.domain.service.AiService;
import com.bchev.notezen.domain.service.ReviewManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Value("${google.ai.api.key}")
    private String apiKey;
    private final AiService aiService;
    private final ReviewManager reviewManager;
    private final UserRepository userRepository;

    public AiController(AiService aiService, ReviewManager reviewManager, UserRepository userRepository) {
        this.aiService = aiService;
        this.reviewManager = reviewManager;
        this.userRepository = userRepository;
    }

    @PostMapping("/suggest")
    public SuggestionResponse suggestReply(
            @RequestParam String email,
            @RequestParam String locationId,
            @RequestParam String reviewId) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email : " + email));

        List<Review> reviews = reviewManager.getReviewsForUser(user.getId(), locationId);

        Review review = reviews.stream()
                .filter(r -> r.getReviewId().equals(reviewId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Avis non trouvé"));

        String suggestion = aiService.suggestResponse(review);
        return new SuggestionResponse(suggestion);
    }

    @GetMapping("/debug-models")
    public List<String> listAvailableModels() {
        RestTemplate restTemplate = new RestTemplate();
        // On utilise l'URL listModels de Google
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");

            return models.stream()
                    .map(m -> (String) m.get("name")) // Récupère le nom technique (ex: models/gemini-1.5-flash)
                    .toList();
        } catch (Exception e) {
            return List.of("Erreur lors de la récupération des modèles : " + e.getMessage());
        }
    }

    public record SuggestionResponse(String text) {}
}

