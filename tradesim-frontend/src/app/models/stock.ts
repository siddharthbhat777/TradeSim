export type MarketCapCategory = 'LARGE' | 'MID' | 'SMALL' | 'UNKNOWN';

export interface Stock {
    id: string;
    symbol: string;
    companyName: string;
    currentPrice: number;
    sector: string;
    status: string;
    dayVolume: number;
    marketCap: number;
    marketCapCategory: MarketCapCategory;
}