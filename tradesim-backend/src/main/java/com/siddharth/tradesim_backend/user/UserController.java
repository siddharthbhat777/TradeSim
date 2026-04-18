package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleRequest;
import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleResponse;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusRequest;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

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