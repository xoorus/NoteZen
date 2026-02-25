package com.bchev.notezen.application.controller.DTO;

import com.bchev.notezen.domain.model.ReviewReply;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class ReviewReplyDTO {
    public String comment;
    public String updateTime; // Format RFC 3339

    public static ReviewReply toReviewReply(ReviewReplyDTO reviewReplyDTO) {
        return new ReviewReply(reviewReplyDTO.getComment(), reviewReplyDTO.getUpdateTime());
    }
}
