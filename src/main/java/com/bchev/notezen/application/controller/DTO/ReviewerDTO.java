package com.bchev.notezen.application.controller.DTO;

import com.bchev.notezen.domain.model.Reviewer;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReviewerDTO {
    public String displayName;
    public String profilePhotoUrl;
    public boolean isAnonymous;

    public static Reviewer toReviewer(ReviewerDTO reviewerDTO) {
        return new Reviewer(reviewerDTO.getDisplayName(), reviewerDTO.getProfilePhotoUrl(), reviewerDTO.isAnonymous);

    }
}
