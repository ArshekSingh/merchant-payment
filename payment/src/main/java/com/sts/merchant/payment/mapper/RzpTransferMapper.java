package com.sts.merchant.payment.mapper;

import com.razorpay.Transfer;
import com.sts.merchant.payment.response.RzpTransfer;
import org.springframework.stereotype.Component;

@Component
public class RzpTransferMapper {

    public RzpTransfer mapTransfer(Transfer transfer) {

        RzpTransfer rzpTransfer = new RzpTransfer();

        if (!transfer.toJson().isNull("id"))
            rzpTransfer.setId(transfer.get("id"));
        if (!transfer.toJson().isNull("entity"))
            rzpTransfer.setEntity(transfer.get("entity"));
//        if(!transfer.toJson().isNull("status"))
//            rzpTransfer.setStatus(transfer.get("status"));
        if (!transfer.toJson().isNull("status"))
            rzpTransfer.setTransferStatus(transfer.get("status"));
        if (!transfer.toJson().isNull("settlement_status"))
            rzpTransfer.setSettlementStatus(transfer.get("settlement_status"));
        if (!transfer.toJson().isNull("source"))
            rzpTransfer.setSource(transfer.get("source"));
        if (!transfer.toJson().isNull("recipient"))
            rzpTransfer.setRecipient(transfer.get("recipient"));
        if (!transfer.toJson().isNull("amount"))
            rzpTransfer.setAmount(transfer.get("amount"));
        if (!transfer.toJson().isNull("currency"))
            rzpTransfer.setCurrency(transfer.get("currency"));
        if (!transfer.toJson().isNull("amount_reversed"))
            rzpTransfer.setAmountReversed(transfer.get("amount_reversed"));
//        if (!transfer.toJson().isNull("linked_account_notes"))
//            rzpTransfer.setLinkedAccountNotes(transfer.get("linked_account_notes"));
        if (!transfer.toJson().isNull("on_hold"))
            rzpTransfer.setOnHold(transfer.get("on_hold"));
        if (!transfer.toJson().isNull("on_hold_until"))
            rzpTransfer.setOnHoldUntil(transfer.get("on_hold_until"));
        if (!transfer.toJson().isNull("recipient_settlement_id"))
            rzpTransfer.setRecipientSettlementId(transfer.get("recipient_settlement_id"));
        if(!transfer.toJson().isNull("tax"))
            rzpTransfer.setTax(transfer.get("tax"));
        if(!transfer.toJson().isNull("fees"))
            rzpTransfer.setFees(transfer.get("fees"));

        return rzpTransfer;

    }
}
