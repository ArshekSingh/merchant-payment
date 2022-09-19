package com.sts.merchant.payment.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RazorpayFetchPaymentResponse {
    private List<Item> items=new ArrayList<>();
}
