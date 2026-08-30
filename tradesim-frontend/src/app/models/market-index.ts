export interface MarketIndex {
    id: string;
    name: string;
    symbol: string;
    exchangeId: string;
    baseValue: number;
    currentValue: number;
    change: number;
    changePercent: number;
    dayOpen: number;
    dayHigh: number;
    dayLow: number;
    previousClose: number;
}

export interface MarketIndexConstituent {
    stockId: string;
    symbol: string;
    companyName: string;
}