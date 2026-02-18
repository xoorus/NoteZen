package com.bchev.notezen.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Review {
    public String name;
    public String reviewId;
    public Reviewer reviewer;
    public StarRating starRating;
    public String comment;
    public String createTime;
    public String updateTime;
    public ReviewReply reviewReply;
}
