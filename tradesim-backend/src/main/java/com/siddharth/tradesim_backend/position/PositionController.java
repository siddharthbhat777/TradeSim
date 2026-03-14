package com.siddharth.tradesim_backend.position;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.position.model.dto.PositionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("positions")
@RequiredArgsConstructor
public class PositionController {
    private final PositionService positionService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<PositionResponse>> getPositions(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(positionService.fetchPositions(principal.getUserId()));
    }
}