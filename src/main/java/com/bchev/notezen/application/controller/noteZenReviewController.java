package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.service.ReviewManager;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.model.Review;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class noteZenReviewController {

    ReviewManager reviewManager;

    public noteZenReviewController(ReviewManager reviewManager) {
        this.reviewManager = reviewManager;
    }

    @GetMapping
    public List<Review> getReviews(@RequestHeader("Authorization") String jwt, @RequestParam String locationId) {
        Long userId = TokenUtils.getUserIdFrom(jwt);
        // On appelle le manager GÉNÉRAL, pas le manager Google
        return reviewManager.getReviewsForUser(userId, locationId);
    }
}
