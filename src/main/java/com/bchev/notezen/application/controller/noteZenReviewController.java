package com.bchev.notezen.application.controller;

import com.bchev.notezen.domain.service.ReviewManager;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.model.Review;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class noteZenReviewController {

    ReviewManager reviewManager;

    public noteZenReviewController(ReviewManager reviewManager) {
        this.reviewManager = reviewManager;
    }

    @RequestMapping("/reviews")
    @GetMapping
    public List<Review> getReviews(@RequestHeader("Authorization") String jwt, @RequestParam String locationId) {
        Long userId = TokenUtils.getUserIdFrom(jwt);
        // On appelle le manager GÉNÉRAL, pas le manager Google
        return reviewManager.getReviewsForUser(userId, locationId);
    }

    @RequestMapping("/locations")
    @GetMapping
    public List<Map<String, Object>> getLocations(@RequestHeader("Authorization") String jwt) {
        Long userId = TokenUtils.getUserIdFrom(jwt);
        return reviewManager.getLocationsForUser(userId);
    }
}
