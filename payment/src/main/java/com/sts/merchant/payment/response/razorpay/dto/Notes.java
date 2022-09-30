package com.sts.merchant.payment.response.razorpay.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Notes {
    private String email;
    private String phone;
}
