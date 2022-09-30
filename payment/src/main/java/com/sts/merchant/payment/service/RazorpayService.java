package com.sts.merchant.payment.service;


public interface RazorpayService {
    void fetchRazorpayPayments();

    void transferEnquiry();

    void transferMoney(String transactionStatus);

}
