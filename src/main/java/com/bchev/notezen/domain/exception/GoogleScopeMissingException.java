package com.bchev.notezen.domain.exception;

/**
 * Levée quand Google refuse un appel API Business Profile (403) parce que
 * l'utilisateur n'a pas coché la permission business.manage lors du login OAuth.
 * Distincte de UnauthorizedUserAccess : ici le token et l'abonnement sont valides,
 * il manque juste ce scope Google précis.
 */
public class GoogleScopeMissingException extends RuntimeException {

    public GoogleScopeMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
