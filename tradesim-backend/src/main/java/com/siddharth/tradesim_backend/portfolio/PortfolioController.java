package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioExposureResponse;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioHistoryResponse;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PortfolioHistoryResponse>> getPortfolioHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(portfolioService.fetchPortfolioHistory(principal.getUserId()));
    }

    @GetMapping("exposure")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PortfolioExposureResponse>> getExposure(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(portfolioService.fetchExposure(principal.getUserId()));
    }
}