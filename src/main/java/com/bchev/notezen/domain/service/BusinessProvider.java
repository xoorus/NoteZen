package com.bchev.notezen.domain.service;


import com.bchev.notezen.application.controller.DTO.ReviewDTO;
import com.bchev.notezen.domain.model.Review;

import java.util.List;
import java.util.Map;

public interface BusinessProvider {
    String fetchAccountId(String accessToken);
    List<Map<String, Object>> fetchLocations(String accountId, String accessToken);
    List<Review> fetchReviews(String accountId, String locationId, String accessToken);
    String refreshAccessToken(String refreshToken);
}
