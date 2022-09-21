package com.sts.merchant.payment.response.razorpay.transferTransaction;

import com.sts.merchant.payment.response.razorpay.dto.Transfers;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class TransactionTransferResponse {
    private String entity;
    private Integer count;
    private List<Transfers> items;
}
