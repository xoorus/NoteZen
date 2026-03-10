package com.bchev.notezen.application.controller.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ListReviewsResponseDTO {
    private List<ReviewDTO> reviews;
    private Double averageRating;
    private Integer totalReviewCount;
    private String nextPageToken;
}
