package com.sts.merchant.payment.response.razorpay.fetchTransaction;

import com.sts.merchant.payment.response.razorpay.dto.Item;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class TransactionFetchResponse {
    private String entity;
    private Integer count;
    private List<Item> items;
}
