package com.sts.merchant.payment.service;

public interface CashfreeService {
    void captureCashFreeSettlements();
    void transferPaymentByPayouts();
    void checkPaymentTransferStatus();
}
