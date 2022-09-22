package com.sts.merchant.payment.response.razorpay.transferStatus;

import com.sts.merchant.payment.response.razorpay.dto.TransferStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Data
@Slf4j
public class TransferStatusResponseRzp {
    private String entity;
    private Integer count;
    private List<TransferStatus> items;

}
