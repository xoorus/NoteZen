package com.bchev.notezen.application.web.noteZen;

import com.bchev.notezen.application.web.noteZen.DTO.ReviewDTO;
import com.bchev.notezen.core.ReviewManager;
import com.bchev.notezen.core.helpers.TokenUtils;
import com.bchev.notezen.core.objects.Review;
import org.springframework.http.ResponseEntity;
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
        Long userId = TokenUtils.getUserIdFrom(jwt); // Argument: JWT -> Result: ID
        return reviewManager.getReviewsForUser(userId, locationId);
    }
}
