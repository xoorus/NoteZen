package com.bchev.notezen.application.controller.DTO;

import lombok.Getter;

import java.util.List;

@Getter
public class ListReviewsResponseDTO {
    public List<ReviewDTO> reviews;
    public Double averageRating;
    public Integer totalReviewCount;
    public String nextPageToken;
}
