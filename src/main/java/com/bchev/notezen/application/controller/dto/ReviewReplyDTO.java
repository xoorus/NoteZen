package com.bchev.notezen.application.controller.dto;

import com.bchev.notezen.domain.model.ReviewReply;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReviewReplyDTO {
    private String comment;
    private String updateTime; // Format RFC 3339

    public static ReviewReply toReviewReply(ReviewReplyDTO reviewReplyDTO) {
        if (reviewReplyDTO == null) {
            return null; // Retourne null proprement si aucune réponse n'existe
        }
        return new ReviewReply(reviewReplyDTO.getComment(), reviewReplyDTO.getUpdateTime());
    }
}
