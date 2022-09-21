package com.sts.merchant.payment.response.razorpay.dto;

import com.sts.merchant.core.dto.razorpayDto.AcquirerData;
import com.sts.merchant.payment.response.Error;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Data
@Slf4j
public class Item {
    private String id;
    private String entity;
    private Integer amount;
    private String currency;
    private String status;
    private Integer base_amount;
    private String base_currency;
    private String method;
    private String order_id;
    private String description;
    private String refund_status;
    private Integer amount_refunded;
    private Boolean captured;
    private String email;
    private String contact;
    private Integer fee;
    private Integer tax;
    private String error_code;
    private String error_description;
    private String error_source;
    private String error_step;
    private String error_reason;
    private String network;
    private String bank;
    private String type;
    private Long created_at;
    private Error error;
}
