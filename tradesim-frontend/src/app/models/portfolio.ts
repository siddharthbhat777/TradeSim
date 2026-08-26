export interface PortfolioHoldingResponse {
    stockId: string;
    symbol: string;
    quantity: number;
    averageBuyPrice: number;
    currentPrice: number;
    currentValue: number;
    unrealizedPnl: number;
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
}