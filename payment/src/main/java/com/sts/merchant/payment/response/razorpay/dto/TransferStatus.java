package com.sts.merchant.payment.response.razorpay.dto;

import com.google.gson.JsonObject;
import com.sts.merchant.payment.response.Error;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TransferStatus {
    private String id;
    private String entity;
    private String status;
    private String source;
    private String recipient;
    private Integer amount;
    private String currency;
    private Integer amount_reversed;
    private Integer fees;
    private Integer tax;
    private Boolean on_hold;
    private Integer on_hold_until;
    private String settlement_status;
    private String recipient_settlement_id;
    private RecipientSettlement recipient_settlement;
    private Long created_at;
    private Error error;

}
