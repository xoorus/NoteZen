package com.bchev.notezen.application.web.google.DTO;

import com.bchev.notezen.core.objects.Review;
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

    // TODO
    public static Review toReview(ReviewDTO reviewDTO) {
        return new Review(
                reviewDTO.name,
                reviewDTO.getReviewId(),
                null,
                null,
                reviewDTO.getComment(),
                reviewDTO.getCreateTime(),
                reviewDTO.getUpdateTime(),
                null
        );
    }
}
