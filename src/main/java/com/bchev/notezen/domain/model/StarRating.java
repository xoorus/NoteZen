package com.bchev.notezen.domain.model;

import lombok.Getter;

@Getter
public enum StarRating {
    STAR_RATING_UNSPECIFIED(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int value;

    StarRating(int value) {
        this.value = value;
    }

    // Optionnel : Pour récupérer le chiffre facilement dans ton front
    public int getNumericValue() {
        return this.value;
    }
}