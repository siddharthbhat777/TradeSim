export interface OrderRequest {
    stockId: string;
    quantity: number;
    side: 'BUY' | 'SELL';
    orderType: 'MARKET' | 'LIMIT';
    timeInForce: 'DAY' | 'IOC' | 'GTC';
    limitPrice?: number | null;
    fundingCurrency?: string;
}

export interface OrderHistoryResponse {
    orderId: string;
    stockId: string;
    symbol: string;
    side: 'BUY' | 'SELL';
    orderType: 'MARKET' | 'LIMIT';
    quantity: number;
    filledQuantity: number;
    limitPrice: number | null;
    status: 'OPEN' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED';
    createdAt: string;
}

export interface OrderEstimateResponse {
    subtotalInFundingCurrency: number;
    safetyBufferInFundingCurrency: number;
    fxFee: number;
    finalTotal: number;
    hasFunds: boolean;
    fundingCurrency: string;
}