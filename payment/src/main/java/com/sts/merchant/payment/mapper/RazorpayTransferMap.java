package com.sts.merchant.payment.mapper;

import com.sts.merchant.payment.response.Notes;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class RazorpayTransferMap {
    private String account;
    private Integer amount;
    private String currency;
    private Notes notes;
    private List<String> linkedAccountNotes;
    private Boolean onHold;
    private Integer onHoldUntil;
}
