package com.siddharth.tradesim_backend.dev_only;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.dev_only.dto.HoldingRequest;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevOnlyService {
    private final AuthRepository authRepository;
    private final HoldingRepository holdingRepository;
    private final StockRepository stockRepository;

    public String makeAdmin(UUID userId) {
        User user = authRepository.findById(userId).orElse(null);
        if (user == null) throw new RuntimeException("User not found");
        user.setRole(Role.ADMIN);
        authRepository.save(user);
        return "Made USER as ADMIN";
    }

    public List<User> fetchUsers() {
        return authRepository.findAll();
    }

    @Transactional
    public String seedHolding(UUID userId, HoldingRequest holdingRequest) {
        if (holdingRequest.quantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
        authRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        stockRepository.findById(holdingRequest.stockId()).orElseThrow(() -> new RuntimeException("Stock not found"));

        Holding holding = holdingRepository.findByUserIdAndStockId(userId, holdingRequest.stockId()).orElse(null);

        if (holding == null) {
            holding = Holding.builder()
                    .userId(userId)
                    .stockId(holdingRequest.stockId())
                    .quantity(holdingRequest.quantity())
                    .build();
        } else {
            holding.increaseQuantity(holdingRequest.quantity());
        }

        holdingRepository.save(holding);

        return "Seeded " + holdingRequest.quantity() + " shares successfully";
    }
}