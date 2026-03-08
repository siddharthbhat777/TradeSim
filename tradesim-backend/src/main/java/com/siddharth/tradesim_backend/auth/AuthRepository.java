package com.siddharth.tradesim_backend.auth;

import com.siddharth.tradesim_backend.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
            SELECT u FROM User u
            WHERE u.username = :input OR u.email = :input
            """)
    Optional<User> findByUsernameOrEmail(@Param("input") String input);
}