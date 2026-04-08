package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.service.ReviewManager;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.model.Review;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class noteZenReviewController {

    private final ReviewManager reviewManager;
    private final GoogleAuthManager googleAuthManager;

    public noteZenReviewController(ReviewManager reviewManager, GoogleAuthManager googleAuthManager) {
        this.reviewManager = reviewManager;
        this.googleAuthManager = googleAuthManager;
    }

    public record ReplyRequest(String text) {}

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<Void> replyToReview(
            @RequestHeader("Authorization") String jwt,
            @RequestParam String locationId,
            @PathVariable String reviewId,
            @RequestBody ReplyRequest request) {
        log.info("noteZenReviewController /{reviewId}/reply API");
        Long userId = TokenUtils.getUserIdFrom(jwt);
        reviewManager.replyToReview(userId, locationId, reviewId, request.text(), this.googleAuthManager);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/reviews")
    public List<Review> getReviews(@RequestHeader("Authorization") String jwt, @RequestParam String locationId) {
        log.info("noteZenReviewController reviews API");
        Long userId = TokenUtils.getUserIdFrom(jwt);
        return reviewManager.getReviewsForUser(userId, locationId, this.googleAuthManager);
    }

    @GetMapping("/locations")
    public List<Map<String, Object>> getLocations(@RequestHeader("Authorization") String jwt) {
        log.info("noteZenReviewController locations API");
        Long userId = TokenUtils.getUserIdFrom(jwt);
        return reviewManager.getLocationsForUser(userId, this.googleAuthManager);
    }
}
