package com.bchev.notezen.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReviewReply {
    private String comment;
    private String updateTime; // Format RFC 3339
}
