package com.siddharth.tradesim_backend.holding;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.holding.model.dto.HoldingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("holdings")
@RequiredArgsConstructor
public class HoldingController {
    private final HoldingService holdingService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<HoldingResponse>> getHoldings(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(holdingService.fetchHoldings(user.getUserId()));
    }
}