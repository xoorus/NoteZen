package com.bchev.notezen.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Conteneur pour une page d'avis Google Business Profile.
 * Permet de gérer la pagination via le nextPageToken.
 */
@AllArgsConstructor
@Getter
@Setter
public class ReviewPage {
    private final List<Review> reviews;
    private final String nextPageToken;
}