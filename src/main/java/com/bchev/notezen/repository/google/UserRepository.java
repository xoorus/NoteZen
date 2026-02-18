package com.bchev.notezen.repository.google;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring génère la requête SQL tout seul : "SELECT * FROM users WHERE email = ..."
    Optional<User> findByEmail(String email);
}