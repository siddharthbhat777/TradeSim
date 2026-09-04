export type IpoOfferStatus = 'PENDING_APPROVAL' | 'SUBSCRIPTION_OPEN' | 'REJECTED' | 'ALLOTTED';

export type IpoSubscriptionStatus = 'SUBMITTED' | 'ALLOTTED' | 'NOT_ALLOTTED';

export interface IpoOfferResponse {
    id: string;
    companyId: string;
    stockId: string;
    submittedByUserId: string;
    issuePrice: number;
    sharesPerAllottee: number;
    maxAllottees: number;
    totalSharesOffered: number;
    subscriptionStartAt: string;
    subscriptionEndAt: string;
    status: IpoOfferStatus;
    reviewedByUserId: string | null;
    reviewedAt: string | null;
    finalizedByUserId: string | null;
    finalizedAt: string | null;
    rejectionReason: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface IpoSubscriptionResponse {
    id: string;
    ipoOfferId: string;
    stockId: string;
    userId: string;
    issuePrice: number;
    lockedAmount: number;
    allottedShares: number;
    status: IpoSubscriptionStatus;
    createdAt: string;
    updatedAt: string;
}