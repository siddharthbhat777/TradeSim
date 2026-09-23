export interface CompanyResponse {
    id: string;
    name: string;
    code: string;
    country: string;
    status: string;
}

export interface CompanyRepresentativeAssignmentResponse {
    id: string;
    companyId: string;
    userId: string;
    assignedByUserId: string;
    assignmentRole: 'PRIMARY_CONTACT' | 'MANAGER';
    status: 'ACTIVE' | 'REVOKED';
    revokedAt: string | null;
    revokedByUserId: string | null;
}

export interface PrimaryContactTransferResponse {
    companyId: string;
    previousPrimaryContactUserId: string;
    newPrimaryContactUserId: string;
    changedByUserId: string;
    transferredAt: string;
}

export interface CompanyOnboardingResponse {
    company: CompanyResponse;
    representative: any;
    assignment: CompanyRepresentativeAssignmentResponse;
}