package com.sts.merchant.payment.mapper;

import com.razorpay.Transfer;
import com.sts.merchant.payment.response.TotalTransfers;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RzpSettlementMapper {

    public List<TotalTransfers> mapSettlement(List<Transfer> transferList) {


        List<TotalTransfers> rzpSettlementList = new ArrayList<>();
        transferList.forEach(transfer -> {
            TotalTransfers rzpSettlement = new TotalTransfers();

            if (!transfer.toJson().isNull("id"))
                rzpSettlement.setId(transfer.get("id"));
            if (!transfer.toJson().isNull("entity"))
                rzpSettlement.setEntity(transfer.get("entity"));
            if (!transfer.toJson().isNull("amount"))
                rzpSettlement.setAmount(transfer.get("amount"));
//            if (!transfer.toJson().isNull("status"))
//                rzpSettlement.setStatus(transfer.get("status"));
            if (!transfer.toJson().isNull("utr"))
                rzpSettlement.setUtr(transfer.get("utr"));
            if (!transfer.toJson().isNull("status"))
                rzpSettlement.setTransferStatus(transfer.get("status"));
            if (!transfer.toJson().isNull("settlement_status"))
                rzpSettlement.setSettlementStatus(transfer.get("settlement_status"));
            if (!transfer.toJson().isNull("source"))
                rzpSettlement.setSource(transfer.get("source"));
            if (!transfer.toJson().isNull("recipient"))
                rzpSettlement.setRecipient("recipient");
            if (!transfer.toJson().isNull("currency"))
                rzpSettlement.setCurrency(transfer.get("currency"));
            if (!transfer.toJson().isNull("amount_reversed"))
                rzpSettlement.setAmountReversed(transfer.get("amount_reversed"));
            if (!transfer.toJson().isNull("recipient_settlement_id"))
                rzpSettlement.setRecipientSettlementId(transfer.get("recipient_settlement_id"));
            if (!transfer.toJson().isNull("recipient_settlement"))
                rzpSettlement.setRecipientSettlement(transfer.get("recipient_settlement"));
            if (!transfer.toJson().isNull("fees"))
                rzpSettlement.setFees(transfer.get("fees"));
            if (!transfer.toJson().isNull("tax"))
                rzpSettlement.setTax(transfer.get("tax"));


            rzpSettlementList.add(rzpSettlement);

        });

        return rzpSettlementList;

    }

//    public RzpSettlement mapSettlement(List<Transfer> transferList) {
//
//        RzpSettlement settlement = new RzpSettlement();
//
//        transferList.forEach(transfer -> {
//            if (!transfer.toJson().isNull("id"))
//                settlement.setId(transfer.get("id"));
//            if (!transfer.toJson().isNull("entity"))
//                settlement.setEntity(transfer.get("entity"));
//            if (!transfer.toJson().isNull("amount"))
//                settlement.setAmount(transfer.get("amount"));
//            if (!transfer.toJson().isNull("status"))
//                settlement.setStatus(transfer.get("status"));
//            if (!transfer.toJson().isNull("utr"))
//                settlement.setUtr(transfer.get("utr"));
//            if (!transfer.toJson().isNull("transfer_status"))
//                settlement.setTransferStatus(transfer.get("transfer_status"));
//            if (!transfer.toJson().isNull("settlement_status"))
//                settlement.setSettlementStatus(transfer.get("settlement_status"));
//            if (!transfer.toJson().isNull("source"))
//                settlement.setSource(transfer.get("source"));
//            if (!transfer.toJson().isNull("recipient"))
//                settlement.setRecipient("recipient");
//            if (!transfer.toJson().isNull("currency"))
//                settlement.setCurrency(transfer.get("currency"));
//            if (!transfer.toJson().isNull("amount_reversed"))
//                settlement.setAmountReversed(transfer.get("amount_reversed"));
//            if (!transfer.toJson().isNull("recipient_settlement_id"))
//                settlement.setRecipientSettlementId(transfer.get("recipient_settlement_id"));
//            if (!transfer.toJson().isNull("recipient_settlement"))
//                settlement.setRecipientSettlement(transfer.get("recipient_settlement"));
//            if (!transfer.toJson().isNull("fees"))
//                settlement.setFees(transfer.get("fees"));
//            if (!transfer.toJson().isNull("tax"))
//                settlement.setTax(transfer.get("tax"));
//
//
//        });
//        return settlement;
//
//    }
}
