package com.bchev.notezen.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Review {
    private String name;
    private String reviewId;
    private Reviewer reviewer;
    private StarRating starRating;
    private String comment;
    private String createTime;
    private String updateTime;
    private ReviewReply reviewReply;
}
