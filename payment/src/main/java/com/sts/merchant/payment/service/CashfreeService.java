package com.sts.merchant.payment.service;

import com.cashfree.lib.pg.domains.response.Transaction;
import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.mapper.RazorpayTransferMap;
import com.sts.merchant.payment.response.RazorpayTransferResponse;

import java.util.List;

public interface CashfreeService {
    void captureCashFreeSettlements();
    void transferPaymentAndCollect();
}
