package com.sts.merchant.payment.mapper;

import com.sts.merchant.payment.request.RazorpayTransferRequest;
import com.sts.merchant.payment.request.Transfers;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RazorpayTransferRequestMapper {

    public RazorpayTransferRequest razorpayTransferRequestMapperFromWebHookPayload(RazorpayTransferMap map) {
        RazorpayTransferRequest request = new RazorpayTransferRequest();
        Transfers transfers = new Transfers();
        transfers.setAmount(map.getAmount());
        transfers.setAccount(map.getAccount());
        transfers.setCurrency(map.getCurrency());
        request.setTransfers(List.of(transfers));
        return request;
    }
}
