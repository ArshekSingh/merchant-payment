package com.sts.merchant.payment.response;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Slf4j
public class Item {
    private String id;
    private String entity;
    private String status;
    private String source;
    private String recipient;
    private Integer amount;
    private String currency;
    private String method;
    private String bank;
    private String email;
    private String contact;
    private Integer fee;
    private Integer tax;
    private Boolean captured;
    private String orderId;
    private Integer amountReversed;
    private String description;
    private Notes notes;
    private List<String> linkedAccountNotes = new ArrayList<>();
    private Boolean onHold;
    private Object onHoldUntil;
    private Object recipientSettlementId;
    private Date createdAt;
    private Object processedAt;
    private Error error;
}
