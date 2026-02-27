package com.bchev.notezen.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    // Spring génère la requête SQL tout seul : "SELECT * FROM users WHERE email = ..."
    Optional<UserEntity> findByEmail(String email);
}