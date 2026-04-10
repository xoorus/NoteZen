package com.bchev.notezen.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AccessControlService {

    // Pour l'instant en dur, mais pourra être injecté via @Value ou DB
    private final List<String> authorizedEmails = List.of(
            "admin@notezen.fr",
            "bchevriaut@gmail.ciw"
    );

    public boolean isAuthorized(String email) {
        boolean authorized = authorizedEmails.contains(email.toLowerCase());
        if (!authorized) {
            log.warn("[Security] Tentative d'accès refusée pour l'email : {}", email);
        }
        return authorized;
    }
}