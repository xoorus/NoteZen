package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.repository.UserEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * En profil "local", realBusinessProvider n'existe pas (absent de Spring) : on retombe toujours sur le mock.
 * En prod, seul l'utilisateur dont l'email correspond à app.mock-user-email reçoit les données mockées ;
 * tous les autres passent par l'API Google réelle.
 */
@Component
public class BusinessProviderResolver {

    private final BusinessProvider mockBusinessProvider;
    private final ObjectProvider<BusinessProvider> realBusinessProvider;
    private final String mockUserEmail;

    public BusinessProviderResolver(
            @Qualifier("mockBusinessProvider") BusinessProvider mockBusinessProvider,
            @Qualifier("realBusinessProvider") ObjectProvider<BusinessProvider> realBusinessProvider,
            @Value("${app.mock-user-email:}") String mockUserEmail) {
        this.mockBusinessProvider = mockBusinessProvider;
        this.realBusinessProvider = realBusinessProvider;
        this.mockUserEmail = mockUserEmail;
    }

    public BusinessProvider resolve(UserEntity user) {
        try {
            // En prod, si email correspond, utiliser le mock
            if (mockUserEmail != null && !mockUserEmail.isBlank()
                    && mockUserEmail.equalsIgnoreCase(user.getEmail())) {
                return mockBusinessProvider;
            }
            // Sinon utiliser le provider réel s'il existe
            return realBusinessProvider.getObject();
        } catch (Exception e) {
            // En profil "local", realBusinessProvider n'existe pas : retomber sur le mock
            return mockBusinessProvider;
        }
    }
}
