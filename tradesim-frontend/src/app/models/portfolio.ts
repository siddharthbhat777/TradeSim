export interface PortfolioHoldingResponse {
    stockId: string;
    symbol: string;
    quantity: number;
    averageBuyPrice: number;
    currentPrice: number;
    currentValue: number;
    unrealizedPnl: number;
    nativeAverageBuyPrice: number;
    nativeCurrentPrice: number;
    nativeCurrentValue: number;
    nativeUnrealizedPnl: number;
    totalInvested: number;
    currency: string;
    fxRate: number;
}

export interface PortfolioResponse {
    holdings: PortfolioHoldingResponse[];
    totalCashValue: number;
    marginLoan: number;
    totalValue: number;
    totalInvested: number;
    totalUnrealizedPnl: number;
    totalRealizedPnl: number;
    totalPnl: number;
    equity: number;
    baseCurrency: string;
}

export interface PortfolioHistoryResponse {
    snapshotDate: string;
    totalValue: number;
    unrealizedPnl: number;
    realizedPnl: number;
    equity: number;
}

export interface PortfolioExposureResponse {
    stockId: string;
    symbol: string;
    positionValue: number;
    exposurePercent: number;
}