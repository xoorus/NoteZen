package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.model.Review;

public interface AiService {
    /**
     * Génère une suggestion de réponse basée sur le contenu de l'avis et sa note.
     */
    String suggestResponse(Review review);
}