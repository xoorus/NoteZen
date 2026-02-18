package com.bchev.notezen.web.google.DTO;

import java.util.List;

public class ListReviewsResponseDTO {
    public List<ReviewDTO> reviews;
    public Double averageRating;
    public Integer totalReviewCount;
    public String nextPageToken;
}
