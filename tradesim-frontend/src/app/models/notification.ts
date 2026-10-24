export interface AppNotification {
    id: string;
    message: string;
    referenceId: string;
    type: 'WALLET_REQUEST' | 'IPO_REQUEST' | 'LISTING_REQUEST';
    isRead: boolean;
    createdAt: string;
}