package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.user.dto.BankBalanceRequest;
import com.siddharth.tradesim_backend.user.dto.BankBalanceResponse;
import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleRequest;
import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleResponse;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusRequest;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusResponse;
import com.siddharth.tradesim_backend.user.dto.EditProfileRequest;
import com.siddharth.tradesim_backend.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.fetchUserProfile(principal.getUserId()));
    }

    @PutMapping("profile/edit")
    public ResponseEntity<UserProfileResponse> editProfile(@Valid @RequestBody EditProfileRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.editProfile(principal.getUserId(), request));
    }

    @PostMapping("profile/bank-balance")
    public ResponseEntity<BankBalanceResponse> getBankBalance(@Valid @RequestBody BankBalanceRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.fetchBankBalance(principal.getUserId(), request));
    }

    @PutMapping("change/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChangeUserStatusResponse> changeStatus(@PathVariable UUID userId, @Valid @RequestBody ChangeUserStatusRequest request) {
        return ResponseEntity.ok(userService.changeStatus(userId, request.status()));
    }

    @PutMapping("change/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChangeUserRoleResponse> changeRole(@PathVariable UUID userId, @Valid @RequestBody ChangeUserRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(userId, request.role()));
    }
}