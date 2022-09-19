package com.sts.merchant.payment.request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class CashfreeTransferRequest {
    private String beneId;
    private String amount;
    private String transferId;
}
