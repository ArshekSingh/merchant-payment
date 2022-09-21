package com.sts.merchant.payment.response;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class FetchResponse {
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
    private String order_id;
    private Integer amount_reversed;
    private String description;
    private Notes notes;
    private List<String> linked_account_notes = new ArrayList<>();
    private Boolean on_hold;
    private Object on_hold_until;
    private Object recipient_settlement_id;
    private Long created_at;
    private Object processed_at;
    private Error error;
}
