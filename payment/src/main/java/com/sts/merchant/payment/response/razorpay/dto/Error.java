package com.sts.merchant.payment.response.razorpay.dto;

import lombok.Data;

@Data
public class Error {
    private String code;
    private String description;
    private String reason;
    private String field;
    private String step;
    private String id;
    private String source;
}
