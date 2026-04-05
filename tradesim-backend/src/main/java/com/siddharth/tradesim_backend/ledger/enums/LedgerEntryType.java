package com.siddharth.tradesim_backend.ledger.enums;

public enum LedgerEntryType {
    INITIAL_CREDIT,
    BUY_LIMIT_MARGIN_LOCK,
    BUY_LIMIT_MARGIN_UNLOCK,
    TRADE_MARGIN_DEBIT,
    TRADE_PROCEEDS_CREDIT,
    MARGIN_LOAN_INCREASE,
    MARGIN_LOAN_REPAYMENT
}