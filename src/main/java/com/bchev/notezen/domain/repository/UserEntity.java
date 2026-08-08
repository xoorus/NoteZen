package com.bchev.notezen.domain.repository;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(length = 1000) // Les tokens peuvent être longs
    private String googleAccessToken;

    @Column(length = 1000)
    private String googleRefreshToken;

    private String googleAccountId;

    private LocalDateTime googleTokenExpiresAt;

    private LocalDateTime tokenExpiration;
}