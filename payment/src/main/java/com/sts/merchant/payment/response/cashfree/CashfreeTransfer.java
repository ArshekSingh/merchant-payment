package com.sts.merchant.payment.response.cashfree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class CashfreeTransfer {
    private String transferId;
    private String bankAccount;
    private String ifsc;
    private String beneId;
    private String amount;
    private String status;
    private String utr;
    private String addedOn;
    private String processedOn;
    private String transferMode;
    private float acknowledged;
    private String phone;
}
