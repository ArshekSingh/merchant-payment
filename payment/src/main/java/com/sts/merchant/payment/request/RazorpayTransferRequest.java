package com.sts.merchant.payment.request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class RazorpayTransferRequest {
    private List<Transfers> transfers;
}
