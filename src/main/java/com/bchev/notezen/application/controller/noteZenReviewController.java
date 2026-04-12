package com.bchev.notezen.application.controller;

import com.bchev.notezen.application.web.google.GoogleAuthManager;
import com.bchev.notezen.domain.exception.UnauthorizedUserAccess;
import com.bchev.notezen.domain.service.ReviewManager;
import com.bchev.notezen.domain.helpers.TokenUtils;
import com.bchev.notezen.domain.model.Review;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@CrossOrigin(origins = { "http://localhost:4200", "https://www.notezen.fr", "https://notezen.fr" })
public class noteZenReviewController {

    private final ReviewManager reviewManager;
    private final GoogleAuthManager googleAuthManager;
    @Value("${app.version}")
    private String appVersion;

    @Value("${front.url}")
    private String frontUrl;

    public noteZenReviewController(ReviewManager reviewManager, GoogleAuthManager googleAuthManager) {
        this.reviewManager = reviewManager;
        this.googleAuthManager = googleAuthManager;
    }

    public record ReplyRequest(String text) {}

    @GetMapping("/version")
    public ResponseEntity<String> getVersion() {
        return ResponseEntity.ok(appVersion);
    }

    @PostMapping("/{reviewId}/reply")
    public ResponseEntity<Void> replyToReview(
            @RequestHeader("Authorization") String jwt,
            @RequestParam String locationId,
            @PathVariable String reviewId,
            @RequestBody ReplyRequest request,
            HttpServletResponse response) throws IOException {
        log.info("noteZenReviewController /{reviewId}/reply API");
        Long userId = TokenUtils.getUserIdFrom(jwt);
        try {
            reviewManager.replyToReview(userId, locationId, reviewId, request.text(), this.googleAuthManager);
            return ResponseEntity.ok().build();
        } catch (UnauthorizedUserAccess e) {
            log.error("replyToReview - Accès refusé pour {}, redirection vers le front", e.getEmail());
            response.sendRedirect(frontUrl+"/unauthorized?email=" + e.getEmail());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getReviews(@RequestHeader("Authorization") String jwt,
                                                   @RequestParam String locationId,
                                                    HttpServletResponse response) throws IOException {
        log.info("noteZenReviewController reviews API");
        Long userId = TokenUtils.getUserIdFrom(jwt);
        try {
            return ResponseEntity.ok(reviewManager.getReviewsForUser(userId, locationId, this.googleAuthManager));
        } catch (UnauthorizedUserAccess e) {
            log.error("getReviews - Accès refusé pour {}, redirection vers le front", e.getEmail());
            response.sendRedirect(frontUrl+"unauthorized?email=" + e.getEmail());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Map<String, Object>>> getLocations(@RequestHeader("Authorization") String jwt,
                                                  HttpServletResponse response) throws IOException {
        log.info("noteZenReviewController locations API");
        Long userId = TokenUtils.getUserIdFrom(jwt);
        try {
            return ResponseEntity.ok(reviewManager.getLocationsForUser(userId, this.googleAuthManager));
        } catch (UnauthorizedUserAccess e) {
            log.error("getLocations - Accès refusé pour {}, redirection vers le front", e.getEmail());
            response.sendRedirect(frontUrl+"unauthorized?email=" + e.getEmail());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.FORBIDDEN);
        }
    }
}
