package com.bchev.notezen.application.google.web.DTO;

import java.util.List;

public class ListReviewsResponseDTO {
    public List<ReviewDTO> reviews;
    public Double averageRating;
    public Integer totalReviewCount;
    public String nextPageToken;
}
