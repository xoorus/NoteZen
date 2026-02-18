package com.bchev.notezen.application.web.google.review;


import com.bchev.notezen.application.web.google.review.DTO.ListReviewsResponseDTO;
import com.bchev.notezen.application.web.google.review.DTO.ReviewDTO;
import com.bchev.notezen.application.web.google.review.DTO.ReviewReplyDTO;
import com.bchev.notezen.core.objects.Review;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GoogleReviewApi {

    RestTemplate restTemplate;

    public GoogleReviewApi(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public List<Review> getReviewsForUser(String accountId, String locationId, String googleToken) {
        String url = String.format(
                "https://mybusiness.googleapis.com/v4/accounts/%s/locations/%s/reviews",
                accountId, locationId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ListReviewsResponseDTO ReviewsResponseDTO = restTemplate.exchange(url, HttpMethod.GET, entity, ListReviewsResponseDTO.class).getBody();
        List<ReviewDTO> reviews = ReviewsResponseDTO.getReviews();

        return null;
        //return reviews.stream().map(ReviewDTO:toReview);

    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReview(
            @PathVariable String accountId,
            @PathVariable String locationId,
            @PathVariable String reviewId) {

        return null;
    }

    @PutMapping("/{reviewId}/reply")
    public ResponseEntity<ReviewReplyDTO> replyReview(
            @PathVariable String accountId,
            @PathVariable String locationId,
            @PathVariable String reviewId,
            @RequestBody ReviewReplyDTO newReply) {

        return null;
    }

}
