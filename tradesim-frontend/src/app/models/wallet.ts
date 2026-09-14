export interface WalletBucket {
    id: string;
    currency: string;
    balance: number;
    lockedBalance: number;
    availableBalance: number;
}

export interface Wallet {
    id: string;
    userId: string;
    multiCurrencyStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'UNREQUESTED';
    buckets: WalletBucket[];
}

export interface WalletTransactionRequest {
    amount: number;
}

export interface CurrencyConversionRequest {
    sourceCurrencyCode: string;
    targetCurrencyCode: string;
    amountToConvert: number;
}