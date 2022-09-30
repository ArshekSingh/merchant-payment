package com.sts.merchant.payment.request.razorpay.fetchTransaction;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TransactionFetchRequest {
    private Long from;
    private Long to;
    private int count;
}
