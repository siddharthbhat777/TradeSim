package com.siddharth.tradesim_backend.company.model;

import com.siddharth.tradesim_backend.common.auditing.AuditableEntity;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "company_representative_assignments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_representative_company_user",
                        columnNames = {"company_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_company_representative_company", columnList = "company_id"),
                @Index(name = "idx_company_representative_user", columnList = "user_id"),
                @Index(name = "idx_company_representative_status", columnList = "status"),
                @Index(name = "idx_company_representative_assignment_role", columnList = "assignment_role")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRepresentativeAssignment extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID assignedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRepresentativeAssignmentRole assignmentRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRepresentativeAssignmentStatus status;

    private Instant revokedAt;

    private UUID revokedByUserId;
}