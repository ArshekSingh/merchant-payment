package com.sts.merchant.payment.response;


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
    private Integer createdAt;
}
