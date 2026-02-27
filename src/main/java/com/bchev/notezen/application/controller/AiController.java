package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.AiService;
import com.bchev.notezen.domain.service.ReviewManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiService aiService;
    private final ReviewManager reviewManager;

    public AiController(AiService aiService, ReviewManager reviewManager) {
        this.aiService = aiService;
        this.reviewManager = reviewManager;
    }

    @PostMapping("/suggest")
    public SuggestionResponse suggestReply(
            @RequestParam Long userId,
            @RequestParam String locationId,
            @RequestParam String reviewId) {

        // 1. Récupérer l'avis complet (pour avoir le texte et la note)
        Review review = reviewManager.getReviewsForUser(userId, locationId).stream()
                .filter(r -> r.getReviewId().equals(reviewId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Avis non trouvé"));

        // 2. Demander à l'IA de générer la réponse
        String suggestion = aiService.suggestResponse(review);

        return new SuggestionResponse(suggestion);
    }

    public record SuggestionResponse(String text) {}
}