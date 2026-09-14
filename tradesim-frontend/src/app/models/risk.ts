export interface RiskResponse {
    equity: number;
    marginUsed: number;
    maintenanceMargin: number;
    unrealizedPnl: number;
    marginRatio: number;
    riskLevel: 'SAFE' | 'WARNING' | 'LIQUIDATION';
    isUnderLiquidation: boolean;
}