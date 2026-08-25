export interface WalletBucketResponse {
    id: string;
    currency: string;
    balance: number;
    lockedBalance: number;
    availableBalance: number;
}

export interface WalletResponse {
    id: string;
    userId: string;
    multiCurrencyStatus: string;
    buckets: WalletBucketResponse[];
}