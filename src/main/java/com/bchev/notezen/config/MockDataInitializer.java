package com.bchev.notezen.config;

import com.bchev.notezen.domain.repository.UserEntity;
import com.bchev.notezen.domain.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Configuration
@Profile("local")
public class MockDataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            userRepository.deleteAllInBatch();

            UserEntity mockUser = new UserEntity();
            mockUser.setEmail("dev@notezen.fr");
            mockUser.setGoogleAccountId("accounts/mock-user-123");
            mockUser.setGoogleAccessToken("fake-access-token");
            mockUser.setTokenExpiration(LocalDateTime.now().plusHours(10));

            userRepository.saveAndFlush(mockUser);
            System.out.println(">> [MOCK] Utilisateur de test créé avec ID: accounts/mock-user-123");
        };
    }
}