package com.sts.merchant.payment.request.razorpay.transferTransaction;

import com.sts.merchant.payment.request.razorpay.model.TransactionTransfer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class TransactionTransferRequest {

    private List<TransactionTransfer> transfers;


}
