package com.siddharth.tradesim_backend.wallet;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.wallet.model.dto.CurrencyConversionRequest;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletResponse;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletTransactionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("wallet")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.fetchMyWallet(principal.getUserId()));
    }

    @PostMapping("deposit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> depositFromBank(@Valid @RequestBody WalletTransactionRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.depositFromBank(principal.getUserId(), request.amount()));
    }

    @PostMapping("withdraw")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> withdrawToBank(@Valid @RequestBody WalletTransactionRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.withdrawToBank(principal.getUserId(), request.amount()));
    }

    @PostMapping("convert")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WalletResponse> convertCurrency(@Valid @RequestBody CurrencyConversionRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.convertCurrency(principal.getUserId(), request));
    }
}