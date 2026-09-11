package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.AuthException;
import com.siddharth.tradesim_backend.auth.enums.OtpPurpose;
import com.siddharth.tradesim_backend.auth.repository.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.service.OtpService;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthRepository authRepository;
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional(readOnly = true)
    public UserProfileResponse fetchUserProfile(UUID userId) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getLinkedBankName(),
                user.getRole(),
                user.getAccountStatus(),
                user.getThemePreference(),
                user.getCountryCode(),
                user.getLastLogin()
        );
    }

    @Transactional
    public UserProfileResponse editProfile(UUID userId, EditProfileRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        user.setFullName(request.fullName());
        user.setLinkedBankName(request.linkedBankName());

        User saved = authRepository.save(user);

        return fetchUserProfile(saved.getId());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw AuthException.unauthorized("Invalid current password");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw UserException.conflict("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        authRepository.save(user);
    }

    @Transactional
    public void initiateEmailChange(InitiateEmailChangeRequest request) {
        if (authRepository.existsByEmail(request.newEmail())) {
            throw UserException.conflict("Email is already registered");
        }

        otpService.generateAndSendOtp(request.newEmail(), OtpPurpose.CHANGE_EMAIL);
    }

    @Transactional
    public UserProfileResponse verifyEmailChange(UUID userId, VerifyEmailChangeRequest request) {
        if (authRepository.existsByEmail(request.newEmail())) {
            throw UserException.conflict("Email is already registered");
        }

        otpService.verifyOtp(request.newEmail(), request.otp(), OtpPurpose.CHANGE_EMAIL);

        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));
        user.setEmail(request.newEmail());
        User saved = authRepository.save(user);

        return fetchUserProfile(saved.getId());
    }

    @Transactional(readOnly = true)
    public BankBalanceResponse fetchBankBalance(UUID userId, BankBalanceRequest request) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw AuthException.unauthorized("Invalid password");
        }

        return new BankBalanceResponse(user.getBankBalance());
    }

    @Transactional
    public ChangeUserStatusResponse changeStatus(UUID userId, AccountStatus status) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        if (user.getAccountStatus() == AccountStatus.BANNED) {
            throw UserException.conflict("Cannot change status of a BANNED user");
        }

        if (status == AccountStatus.DEACTIVATED) {
            throw UserException.conflict("Only users can deactivate their own accounts");
        }

        if (status == AccountStatus.BANNED) {
            List<Order> openOrders = orderRepository.findByUserIdAndStatusIn(userId, List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED));
            for (Order order : openOrders) {
                orderLifecycleService.cancelOrder(order);
            }
        }

        user.setAccountStatus(status);
        User saved = authRepository.save(user);

        return new ChangeUserStatusResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole(), saved.getAccountStatus());
    }

    @Transactional
    public ChangeUserRoleResponse changeRole(UUID userId, Role role) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));

        if (role == Role.ADMIN) {
            throw UserException.conflict("Admin role assignment is forbidden via API");
        }

        if (user.getRole() == Role.ADMIN) {
            throw UserException.conflict("Cannot change role of an Admin user");
        }

        if (user.getRole() == Role.COMPANY_REPRESENTATIVE && role == Role.USER) {
            boolean hasActiveAssignments = companyRepresentativeAssignmentRepository.existsByUserIdAndStatus(userId, CompanyRepresentativeAssignmentStatus.ACTIVE);
            if (hasActiveAssignments) {
                throw UserException.conflict("Cannot demote Company Representative with active company assignments");
            }
        }

        user.setRole(role);
        User saved = authRepository.save(user);

        return new ChangeUserRoleResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole(), saved.getAccountStatus());
    }
}