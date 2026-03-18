package com.siddharth.tradesim_backend.risk;

import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.risk.dto.RiskResponse;
import com.siddharth.tradesim_backend.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("risk")
@RequiredArgsConstructor
public class RiskController {
    private final RiskService riskService;

    @GetMapping
    public RiskResponse getMyRisk(@AuthenticationPrincipal UserPrincipal user) {
        return riskService.getUserRisk(user.getUserId());
    }
}