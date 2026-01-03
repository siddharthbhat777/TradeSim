package com.siddharth.tradesim_backend.dev_only;

import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("dev")
@RequiredArgsConstructor
public class DevOnlyController {
    private final DevOnlyService devOnlyService;

    @GetMapping("jwt/generate-key")
    public String generateJwtSecretKey() {
        byte[] keyBytes = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512)
                .getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    @PutMapping("make-admin")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> makeAdmin(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(devOnlyService.makeAdmin(user.getUserId()));
    }

    @GetMapping("users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(devOnlyService.fetchUsers());
    }
}