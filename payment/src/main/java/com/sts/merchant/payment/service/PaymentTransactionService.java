package com.sts.merchant.payment.service;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sts.merchant.payment.response.razorpay.dto.Item;

public interface PaymentTransactionService {
    void saveRazorpayPaymentAsTransaction(Item item, String accountId, Integer loanId, Integer loanAccountMapId) throws JsonProcessingException;

    void saveCashFreePaymentAsTransaction(Settlement item, String accountId, Integer loanId, Integer loanAccountMapId) throws JsonProcessingException;
}
