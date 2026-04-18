package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.trading_account.model.dto.TradingAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("trading-account")
@RequiredArgsConstructor
public class TradingAccountController {
    private final TradingAccountService tradingAccountService;

    @GetMapping
    public ResponseEntity<TradingAccountResponse> getMyTradingAccount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tradingAccountService.fetchMyTradingAccount(principal.getUserId()));
    }
}