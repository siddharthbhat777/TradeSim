export interface CapTableEntryResponse {
    userId: string;
    quantity: number;
}

export interface ListingRequestResponse {
    id: string;
    companyId: string;
    submittedByUserId: string;
    symbol: string;
    exchangeId: string;
    referencePrice: number;
    sector: string;
    priceBandPercent: number;
    totalShares: number;
    capTable: CapTableEntryResponse[];
    status: string;
    reviewedByUserId: string | null;
    reviewedAt: string | null;
    approvedStockId: string | null;
    rejectionReason: string | null;
    currency: string;
    createdAt: string;
    updatedAt: string;
}