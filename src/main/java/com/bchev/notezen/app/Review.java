package com.bchev.notezen.app;

import com.bchev.notezen.web.google.DTO.ReviewReplyDTO;
import com.bchev.notezen.web.google.DTO.ReviewerDTO;
import com.bchev.notezen.web.google.DTO.StarRatingDTO;
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
