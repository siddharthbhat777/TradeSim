export interface Exchange {
    id: string;
    name: string;
    code: string;
    countryCode: string;
    timezone: string;
    currency: string;
    marketOpenTime: string;
    marketCloseTime: string;
    status: string;
}

export interface ExchangeMarketClock {
    exchangeId: string;
    exchangeCode: string;
    exchangeName: string;
    timezone: string;
    localDate: string;
    localTime: string;
    localDayOfWeek: string;
    marketOpenTime: string;
    marketCloseTime: string;
    tradingDay: boolean;
    marketOpenNow: boolean;
    currentInstant: string;
    todayMarketOpenAt: string;
    todayMarketCloseAt: string;
}