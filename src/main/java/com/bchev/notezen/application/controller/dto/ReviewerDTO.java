package com.bchev.notezen.application.controller.dto;

import com.bchev.notezen.domain.model.Reviewer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReviewerDTO {
    private String displayName;
    private String profilePhotoUrl;
    private boolean isAnonymous;

    public static Reviewer toReviewer(ReviewerDTO reviewerDTO) {
        return new Reviewer(reviewerDTO.getDisplayName(), reviewerDTO.getProfilePhotoUrl(), reviewerDTO.isAnonymous);

    }
}
