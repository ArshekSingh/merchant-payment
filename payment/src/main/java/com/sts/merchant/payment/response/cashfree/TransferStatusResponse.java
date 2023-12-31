package com.sts.merchant.payment.response.cashfree;

import com.sts.merchant.payment.response.cashfree.CashfreeTransferStatus;
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
public class TransferStatusResponse {
    private String status;
    private String subCode;
    private String message;
    private CashfreeTransferStatus data;
}
