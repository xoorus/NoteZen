package com.bchev.notezen.core;

import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.core.objects.ReviewReply;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository {

    List<Review> listReviews(String accountId, String locationId);
    Review getReview(String accountId, String locationId, String reviewId);
    ReviewReply updateReply(String accountId, String locationId, String reviewId, ReviewReply reply);

}
