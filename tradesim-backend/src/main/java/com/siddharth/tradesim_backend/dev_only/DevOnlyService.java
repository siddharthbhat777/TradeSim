package com.siddharth.tradesim_backend.dev_only;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.dev_only.dto.PositionRequest;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevOnlyService {
    private final AuthRepository authRepository;
    private final PositionRepository positionRepository;
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
    public String seedPosition(UUID userId, PositionRequest positionRequest) {
        if (positionRequest.quantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }
        authRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Stock stock = stockRepository.findById(positionRequest.stockId()).orElseThrow(() -> new RuntimeException("Stock not found"));

        Position position = positionRepository.findByUserIdAndStockId(userId, positionRequest.stockId()).orElse(null);

        if (position == null) {
            position = Position.builder()
                    .userId(userId)
                    .stockId(positionRequest.stockId())
                    .averageBuyPrice(stock.getLastTradedPrice())
                    .realizedPnl(BigDecimal.ZERO)
                    .build();
            position.increaseQuantity(positionRequest.quantity());
        } else {
            position.increaseQuantity(positionRequest.quantity());
        }

        positionRepository.save(position);

        return "Seeded " + positionRequest.quantity() + " shares successfully";
    }
}