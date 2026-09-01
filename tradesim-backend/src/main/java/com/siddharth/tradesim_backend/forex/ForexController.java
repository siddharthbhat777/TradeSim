package com.siddharth.tradesim_backend.forex;

import com.siddharth.tradesim_backend.forex.service.ForexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("forex")
@RequiredArgsConstructor
public class ForexController {
    private final ForexService forexService;

    @GetMapping("currencies")
    public ResponseEntity<List<String>> getSupportedCurrencies() {
        return ResponseEntity.ok(forexService.fetchActiveSupportedCurrencies());
    }
}