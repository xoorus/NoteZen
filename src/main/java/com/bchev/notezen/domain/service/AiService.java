package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.model.LightReview;

public interface AiService {
    /**
     * Génère une suggestion de réponse basée sur le contenu de l'avis et sa note.
     */
    String suggestResponse(LightReview review);
}