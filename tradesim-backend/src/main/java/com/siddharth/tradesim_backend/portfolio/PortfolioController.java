package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.portfolio.dto.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PortfolioResponse> getPortfolio(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(portfolioService.fetchPortfolio(user.getUserId()));
    }
}