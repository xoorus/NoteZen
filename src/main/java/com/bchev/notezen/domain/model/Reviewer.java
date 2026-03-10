package com.bchev.notezen.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Reviewer {
    private String displayName;
    private String profilePhotoUrl;
    private boolean isAnonymous;
}
