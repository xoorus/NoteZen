package com.bchev.notezen.core;

import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.core.objects.ReviewReply;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
/*
@Service
@ConditionalOnProperty(name = "google.api.mode", havingValue = "mock", matchIfMissing = true)
public class ReviewMock  {

    public ReviewMock() {
    }

    @Override
    public List<Review> listReviews(String accountId, String locationId) {
        return List.of();
    }

    @Override
    public Review getReview(String accountId, String locationId, String reviewId) {
        return null;
    }

    @Override
    public ReviewReply updateReply(String accountId, String locationId, String reviewId, ReviewReply reply) {
        return null;
    }
}
*/