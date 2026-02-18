package com.bchev.notezen.application.web.google.review.DTO;

import lombok.Getter;

import java.util.List;

@Getter
public class ListReviewsResponseDTO {
    public List<ReviewDTO> reviews;
    public Double averageRating;
    public Integer totalReviewCount;
    public String nextPageToken;
}
