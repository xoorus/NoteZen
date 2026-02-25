package com.bchev.notezen.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Reviewer {
    public String displayName;
    public String profilePhotoUrl;
    public boolean isAnonymous;
}
