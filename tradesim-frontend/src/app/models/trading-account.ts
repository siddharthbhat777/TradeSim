export interface TradingAccountResponse {
    id: string;
    userId: string;
    baseCurrency: string;
    marginLoan: number;
    leverage: number;
    maintenanceMarginPercent: number;
}