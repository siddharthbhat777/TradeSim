package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.ledger.model.dto.LedgerEntryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("ledger")
@RequiredArgsConstructor
public class LedgerController {
    private final LedgerService ledgerService;

    @GetMapping
    public ResponseEntity<List<LedgerEntryResponse>> getMyLedger(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ledgerService.fetchMyLedger(principal.getUserId()));
    }
}