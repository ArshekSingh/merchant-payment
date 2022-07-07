package com.sts.merchant.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sts.merchant.payment.response.Item;

public interface PaymentTransactionService {
    void savePaymentAsTransaction(Item item, String accountId, Integer loanId, Integer loanAccountMapId) throws JsonProcessingException;
}
