export type LedgerEntryType =
    | 'INITIAL_CREDIT'
    | 'DEPOSIT'
    | 'WITHDRAWAL'
    | 'BUY_LIMIT_MARGIN_LOCK'
    | 'BUY_LIMIT_MARGIN_UNLOCK'
    | 'BUY_ORDER_MARGIN_LOCK'
    | 'BUY_ORDER_MARGIN_UNLOCK'
    | 'TRADE_MARGIN_DEBIT'
    | 'TRADE_PROCEEDS_CREDIT'
    | 'MARGIN_LOAN_INCREASE'
    | 'MARGIN_LOAN_REPAYMENT'
    | 'IPO_SUBSCRIPTION_LOCK'
    | 'IPO_SUBSCRIPTION_UNLOCK'
    | 'IPO_ALLOTMENT_DEBIT'
    | 'FX_CONVERSION_FEE';

export interface LedgerEntryResponse {
    id: string;
    tradingAccountId: string;
    userId: string;
    stockId: string | null;
    orderId: string | null;
    ipoOfferId: string | null;
    type: LedgerEntryType;
    amount: number;
    currency: string;
    balanceAfter: number;
    lockedBalanceAfter: number;
    marginLoanAfter: number;
    description: string;
    createdAt: string;
}