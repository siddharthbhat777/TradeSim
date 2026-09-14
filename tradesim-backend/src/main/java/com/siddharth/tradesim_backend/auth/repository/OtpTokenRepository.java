package com.siddharth.tradesim_backend.auth.repository;

import com.siddharth.tradesim_backend.auth.enums.OtpPurpose;
import com.siddharth.tradesim_backend.auth.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    List<OtpToken> findByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, OtpPurpose purpose);
}