package com.bchev.notezen.core.helpers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public  class TokenUtils {

    @Value("${jwt.secret}")
    private static String secretKey;

    public static Long getUserIdFrom(String token) {
        // On retire le préfixe "Bearer " si présent
        String cleanToken = token.replace("Bearer ", "");

        // On décode le token avec la clé secrète
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(cleanToken)
                .getBody();

        // On récupère l'ID (stocké dans le "subject" ou un claim custom)
        return Long.parseLong(claims.getSubject());
    }
}