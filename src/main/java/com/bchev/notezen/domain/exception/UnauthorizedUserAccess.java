package com.bchev.notezen.domain.exception;

import lombok.Getter;

/**
 * Exception levée lorsqu'un utilisateur tente de se connecter
 * sans être présent dans la liste blanche (allowlist).
 */
@Getter
public class UnauthorizedUserAccess extends RuntimeException {

    private final String email;

    public UnauthorizedUserAccess(String message, String email) {
        super(message);
        this.email = email;
    }
}