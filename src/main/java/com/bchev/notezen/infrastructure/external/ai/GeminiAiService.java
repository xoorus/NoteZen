package com.bchev.notezen.infrastructure.external.ai;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiAiService implements AiService {

    @Value("${google.ai.api.key}")
    private String apiKey;

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

        // 2. Appel à l'API (Simulé ici pour ton POC, à remplacer par l'appel HTTP réel)
        return callGeminiApi(prompt);
    }

    private String callGeminiApi(String prompt) {
        // En mode "Local/Mock", on peut renvoyer une réponse type pour tester le front
        return "[Suggestion IA] : Merci beaucoup pour votre retour ! Nous sommes ravis que votre expérience vous ait plu. Au plaisir de vous revoir !";
    }
}