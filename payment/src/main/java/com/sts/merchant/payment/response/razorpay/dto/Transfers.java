package com.sts.merchant.payment.response.razorpay.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Transfers {

    private String id;
    private String entity;
    private String status;
    private String source;
    private String recipient;
    private Integer amount;
    private String currency;
    private Integer amount_reversed;
    private Error error;
    private Integer fees;
    private Integer tax;
    private Boolean on_hold;
    private Integer on_hold_until;
    private String recipient_settlement_id;
    private Long created_at;

}
