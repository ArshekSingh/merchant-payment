package com.sts.merchant.payment.request.razorpay.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TransactionTransfer {
    private String account;
    private Integer amount;
    private String currency;
    private Boolean on_hold;
}
