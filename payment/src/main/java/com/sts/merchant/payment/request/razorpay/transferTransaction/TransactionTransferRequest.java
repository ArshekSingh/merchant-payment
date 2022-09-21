package com.sts.merchant.payment.request.razorpay.transferTransaction;

import com.google.gson.JsonObject;
import com.sts.merchant.payment.request.razorpay.dto.TransactionTransfer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class TransactionTransferRequest {

    private List<TransactionTransfer> transfers;


}
