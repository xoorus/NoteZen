package com.bchev.notezen.infrastructure.external.ai;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiAiService implements AiService {

    @Value("${google.ai.api.key}")
    private String API_KEY;

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";

    @Override
    public String suggestResponse(Review review) {
        // 1. Construction du Prompt (le secret d'une bonne IA)
        String prompt = String.format(
                "Tu es le gérant d'un établissement. Réponds de manière professionnelle, courte et courtoise à l'avis suivant :\n" +
                        "Client : %s\n" +
                        "Note : %s étoiles\n" +
                        "Commentaire : %s\n\n" +
                        "Consignes : Si l'avis est positif, remercie. Si l'avis est négatif, sois empathique et propose de discuter. Ne dépasse pas 3 phrases.",
                review.getReviewer().getDisplayName(),
                review.getStarRating(),
                review.getComment() != null ? review.getComment() : "L'utilisateur n'a pas laissé de texte, remercie juste pour la note."
        );

        // 2. Appel à l'API
        return callGeminiApi(prompt);
    }

    private String callGeminiApi(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_API_URL + API_KEY;

        // 1. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Structure JSON (La seule que Google accepte en v1)
        Map<String, Object> textMap = Map.of("text", prompt);
        Map<String, Object> partsMap = Map.of("parts", List.of(textMap));
        Map<String, Object> body = Map.of("contents", List.of(partsMap));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // Log de debug pour voir si l'URL est bien formée (sans espaces, etc.)
            System.out.println("Tentative d'appel sur : " + url);

            // On utilise postForEntity pour avoir plus de détails sur la réponse
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> response = responseEntity.getBody();

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.error("ERREUR 404 : Ce modèle n'existe pas ou l'URL est mal formée.");
            log.error("Réponse de Google : {}", e.getResponseBodyAsString());
        }

        return "Erreur lors de la génération.";
    }

}