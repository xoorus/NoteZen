package com.bchev.notezen.domain.service;

import com.bchev.notezen.domain.repository.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessProviderResolverTest {

    @Mock
    private BusinessProvider mockBusinessProvider;

    @Mock
    private ObjectProvider<BusinessProvider> realBusinessProvider;

    @Mock
    private BusinessProvider realBusinessProviderInstance;

    private BusinessProviderResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new BusinessProviderResolver(mockBusinessProvider, realBusinessProvider, "bchevriaut@gmail.com");
    }

    @Test
    void resolve_withMatchingMockEmail_shouldReturnMockProvider() {
        // Given
        UserEntity user = createUser("bchevriaut@gmail.com");

        // When
        BusinessProvider provider = resolver.resolve(user);

        // Then
        assertEquals(mockBusinessProvider, provider);
        verify(realBusinessProvider, never()).getObject();
    }

    @Test
    void resolve_withMatchingMockEmailDifferentCase_shouldReturnMockProvider() {
        // Given
        UserEntity user = createUser("BCHEVRIAUT@GMAIL.COM");

        // When
        BusinessProvider provider = resolver.resolve(user);

        // Then
        assertEquals(mockBusinessProvider, provider);
    }

    @Test
    void resolve_withDifferentEmail_shouldReturnRealProvider() {
        // Given
        UserEntity user = createUser("other@example.com");
        when(realBusinessProvider.getObject()).thenReturn(realBusinessProviderInstance);

        // When
        BusinessProvider provider = resolver.resolve(user);

        // Then
        assertEquals(realBusinessProviderInstance, provider);
        verify(realBusinessProvider).getObject();
    }

    @Test
    void resolve_withRealProviderNotAvailable_shouldFallbackToMock() {
        // Given
        UserEntity user = createUser("other@example.com");
        when(realBusinessProvider.getObject()).thenThrow(new RuntimeException("Provider not available"));

        // When
        BusinessProvider provider = resolver.resolve(user);

        // Then
        assertEquals(mockBusinessProvider, provider);
    }

    @Test
    void resolve_withEmptyMockEmail_shouldReturnRealProvider() {
        // Given
        resolver = new BusinessProviderResolver(mockBusinessProvider, realBusinessProvider, "");
        UserEntity user = createUser("any@example.com");
        when(realBusinessProvider.getObject()).thenReturn(realBusinessProviderInstance);

        // When
        BusinessProvider provider = resolver.resolve(user);

        // Then
        assertEquals(realBusinessProviderInstance, provider);
    }

    @Test
    void resolve_withNullMockEmail_shouldReturnRealProvider() {
        // Given
        resolver = new BusinessProviderResolver(mockBusinessProvider, realBusinessProvider, null);
        UserEntity user = createUser("any@example.com");
        when(realBusinessProvider.getObject()).thenReturn(realBusinessProviderInstance);

        // When
        BusinessProvider provider = resolver.resolve(user);

        // Then
        assertEquals(realBusinessProviderInstance, provider);
    }

    private UserEntity createUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        return user;
    }
}
