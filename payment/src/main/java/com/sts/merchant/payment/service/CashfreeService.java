package com.sts.merchant.payment.service;

import java.util.List;

public interface CashfreeService {
    void captureCashFreeSettlements();

    void transferCapturedPaymentsByPayouts();

    void transferFailedAndIncompletePaymentsByPayouts();

    void checkPaymentTransferStatus();
}
