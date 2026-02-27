package com.bchev.notezen.application.controller.DTO;

import com.bchev.notezen.domain.model.Review;
import com.bchev.notezen.domain.model.StarRating;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDTO {
    private String name;
    private String reviewId;
    private ReviewerDTO reviewer;
    private StarRatingDTO starRating;
    private String comment;
    private String createTime;
    private String updateTime;
    private ReviewReplyDTO reviewReply;

    public static Review toReview(ReviewDTO reviewDTO) {
        return new Review(
                reviewDTO.name,
                reviewDTO.getReviewId(),
                ReviewerDTO.toReviewer(reviewDTO.reviewer),
                StarRating.valueOf(reviewDTO.getStarRating().toString()),
                reviewDTO.getComment(),
                reviewDTO.getCreateTime(),
                reviewDTO.getUpdateTime(),
                reviewDTO.getReviewReply() != null ? ReviewReplyDTO.toReviewReply(reviewDTO.getReviewReply()) : null
        );
    }
}
