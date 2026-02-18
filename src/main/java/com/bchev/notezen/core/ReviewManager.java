package com.bchev.notezen.core;

import com.bchev.notezen.core.google.GoogleReviewService;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.repository.google.User;
import com.bchev.notezen.repository.google.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "google.api.mode", havingValue = "real", matchIfMissing = true)
public class ReviewManager {

    UserRepository userRepository;
    GoogleReviewService googleReviewService;

    public ReviewManager(UserRepository userRepository, GoogleReviewService googleReviewService) {
        this.userRepository = userRepository;
        this.googleReviewService = googleReviewService;
    }

    public List<Review> getReviewsForUser(Long userId, String locationId) {
        User user = userRepository.findById(userId).orElseThrow();
        String googleToken = user.getGoogleAccessToken();
        String googleAccountId = user.getGoogleAccountId();
        return googleReviewService.getReviewsForUser(googleAccountId, locationId, googleToken);
    }

}
