package com.sts.merchant.payment.response;

import java.util.ArrayList;
import java.util.List;

public class RazorpayTransferResponse {
    private String entity;
    private Integer count;
    private List<Item> items = new ArrayList<>();
}
