package com.sts.merchant.payment.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RzpTransfer {
    private String id;
    private String entity;
    private String transferStatus;
    //private String status;
    private String settlementStatus;
    private String source;
    private String recipient;
    private Integer amount;
    private String currency;
    private Integer amountReversed;
    private Notes notes;
    private Error error;
    private List<String> linkedAccountNotes = new ArrayList<>();
    private Boolean onHold;
    private Integer onHoldUntil;
    private String recipientSettlementId;
    private String createdAt;
    private Integer fees;
    private Integer tax;
    private Integer processed_at;

}
