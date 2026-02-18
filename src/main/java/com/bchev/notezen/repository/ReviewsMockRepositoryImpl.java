package com.bchev.notezen.repository;

import com.bchev.notezen.core.ReviewRepository;
import com.bchev.notezen.core.objects.Review;
import com.bchev.notezen.core.objects.ReviewReply;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReviewsMockRepositoryImpl implements ReviewRepository {

    private final Map<String, Review> mockDatabase = new ConcurrentHashMap<>();

    @Override
    public List<Review> listReviews(String acc, String loc) {
        return new ArrayList<>(mockDatabase.values());
    }

    @Override
    public Review getReview(String acc, String loc, String id) {
        return mockDatabase.get(id);
    }

    @Override
    public ReviewReply updateReply(String acc, String loc, String id, ReviewReply reply) {
        reply.updateTime = java.time.Instant.now().toString();
        if(mockDatabase.containsKey(id)) mockDatabase.get(id).reviewReply = reply;
        return reply;
    }

}
