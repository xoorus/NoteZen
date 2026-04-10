package com.bchev.notezen.infrastructure.external.ai;

import com.bchev.notezen.domain.model.LightReview;
import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiAiService implements AiService {

    @Value("${google.ai.api.key}")
    private String API_KEY;

    private final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash-lite:generateContent?key=";
    @Override
    public String suggestResponse(LightReview review) {
        String prompt = String.format(
                "Tu es le gérant d'un établissement. Réponds de manière professionnelle, courte et courtoise à l'avis suivant :\n" +
                        "Client : %s\n" +
                        "Note : %s étoiles\n" +
                        "Commentaire : %s\n\n" +
                        "Consignes : Si l'avis est positif, remercie. Si l'avis est négatif, sois empathique et propose de discuter. Ne dépasse pas 3 phrases.",
                review.getReviewerName(),
                review.getStarRating(),
                review.getComment() != null ? review.getComment() : "L'utilisateur n'a pas laissé de texte, remercie juste pour la note."
        );

        return callGeminiWithRetry(prompt, 3);
    }

    private String callGeminiWithRetry(String prompt, int maxAttempts) {
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                return callGeminiApi(prompt);
            } catch (HttpServerErrorException.ServiceUnavailable e) {
                attempt++;
                log.warn("Tentative {}/{} : Gemini est indisponible (503). Nouvel essai dans 2s...", attempt, maxAttempts);
                try {
                    Thread.sleep(2000); // Pause de 2 secondes avant de réessayer
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                log.error("Erreur critique lors de l'appel Gemini : ", e);
                break;
            }
        }
        return "L'assistant IA est temporairement surchargé. Veuillez réessayer dans un instant.";
    }

    private String callGeminiApi(String prompt) {
        RestTemplate restTemplate = new RestTemplate();
        String url = GEMINI_API_URL + API_KEY;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Construction du corps de la requête
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // postForEntity lancera une HttpServerErrorException.ServiceUnavailable en cas de 503
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);
        Map<String, Object> response = responseEntity.getBody();

        if (response != null && response.containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (!candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            }
        }

        return "Erreur lors de la génération.";
    }
}