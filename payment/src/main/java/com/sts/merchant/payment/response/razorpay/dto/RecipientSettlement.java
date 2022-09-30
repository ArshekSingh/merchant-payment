package com.sts.merchant.payment.response.razorpay.dto;


import lombok.Data;

@Data
public class RecipientSettlement {
    private String id;
    private String entity;
    private String amount;
    private String status;
    private Integer fee;
    private Integer tax;
    private String utr;
    private Long createdAt;
}
