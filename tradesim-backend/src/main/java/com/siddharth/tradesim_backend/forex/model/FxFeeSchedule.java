package com.siddharth.tradesim_backend.forex.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.forex.enums.CurrencyCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "fx_fee_schedules",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"source_category", "target_category"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxFeeSchedule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCategory sourceCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCategory targetCategory;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal feePercentage;
}