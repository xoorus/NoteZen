package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.controller.dto.StarRatingDTO;
import com.bchev.notezen.domain.model.LightReview;
import com.bchev.notezen.domain.model.StarRating;
import com.bchev.notezen.domain.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = { "http://localhost:4200", "https://www.notezen.fr", "https://notezen.fr" })
public class AiController {

    @Value("${google.ai.api.key}")
    private String apiKey;
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/suggest")
    public SuggestionResponse suggestReply(
            @RequestParam String name,
            @RequestParam String reviewerName,
            @RequestParam StarRatingDTO starRating,
            @RequestParam String comment) {

        String suggestion = aiService.suggestResponse(new LightReview(name, reviewerName, StarRating.valueOf(starRating.name()), comment));
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
                    .map(m -> (String) m.get("name"))
                    .toList();
        } catch (Exception e) {
            return List.of("Erreur lors de la récupération des modèles : " + e.getMessage());
        }
    }

    public record SuggestionResponse(String text) {}
}
