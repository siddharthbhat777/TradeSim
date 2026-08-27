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