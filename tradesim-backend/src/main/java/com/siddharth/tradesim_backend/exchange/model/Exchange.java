package com.siddharth.tradesim_backend.exchange.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "exchanges",
        indexes = {
                @Index(name = "idx_exchange_code", columnList = "code", unique = true),
                @Index(name = "idx_exchange_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exchange extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 60)
    private String country;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private LocalTime marketOpenTime;

    @Column(nullable = false)
    private LocalTime marketCloseTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeStatus status;
}