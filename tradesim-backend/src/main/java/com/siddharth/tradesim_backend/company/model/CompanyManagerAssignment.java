package com.siddharth.tradesim_backend.company.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "company_manager_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_manager_company_user",
                        columnNames = {"company_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_company_manager_company", columnList = "company_id"),
                @Index(name = "idx_company_manager_user", columnList = "user_id"),
                @Index(name = "idx_company_manager_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyManagerAssignment extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assigned_by_admin_id", nullable = false)
    private UUID assignedByAdminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyManagerAssignmentStatus status;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}