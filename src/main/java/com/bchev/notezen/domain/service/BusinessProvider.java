package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.ReviewPage;

import java.util.List;
import java.util.Map;

public interface BusinessProvider {
    String fetchAccountId(String accessToken);
    List<Map<String, Object>> fetchLocations(String accountId, String accessToken);
    ReviewPage fetchReviews(String accountId, String locationId, String accessToken, String pageToken);
    void postReply(String accountId, String locationId, String reviewId, String text, String accessToken);
}
