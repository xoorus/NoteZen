package com.bchev.notezen.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LightReview {
    private String name;
    private String reviewerName ;
    private StarRating starRating;
    private String comment;

}
