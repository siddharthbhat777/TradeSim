package com.siddharth.tradesim_backend.auth.service;

import com.siddharth.tradesim_backend.auth.AuthException;
import com.siddharth.tradesim_backend.auth.enums.OtpPurpose;
import com.siddharth.tradesim_backend.auth.model.OtpToken;
import com.siddharth.tradesim_backend.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int OTP_EXPIRATION_MINUTES = 2;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        String hashedOtp = passwordEncoder.encode(otp);

        OtpToken token = OtpToken.builder()
                .email(email)
                .otpHash(hashedOtp)
                .purpose(purpose)
                .expiresAt(Instant.now().plusSeconds(OTP_EXPIRATION_MINUTES * 60))
                .used(false)
                .build();

        otpTokenRepository.save(token);

        String subject = switch (purpose) {
            case REGISTRATION -> "TradeSim - Complete Your Registration";
            case FORGOT_PASSWORD -> "TradeSim - Password Reset Request";
            case CHANGE_EMAIL -> "TradeSim - Verify Your New Email";
        };

        emailService.sendOtpEmail(email, otp, subject);
    }

    @Transactional
    public void verifyOtp(String email, String rawOtp, OtpPurpose purpose) {
        List<OtpToken> tokens = otpTokenRepository.findByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose);

        if (tokens.isEmpty()) {
            throw AuthException.unauthorized("Invalid or expired OTP");
        }

        OtpToken latestToken = tokens.getFirst();

        if (latestToken.getExpiresAt().isBefore(Instant.now())) {
            throw AuthException.unauthorized("OTP has expired");
        }

        if (!passwordEncoder.matches(rawOtp, latestToken.getOtpHash())) {
            throw AuthException.unauthorized("Invalid OTP");
        }

        latestToken.setUsed(true);
        otpTokenRepository.save(latestToken);
    }
}