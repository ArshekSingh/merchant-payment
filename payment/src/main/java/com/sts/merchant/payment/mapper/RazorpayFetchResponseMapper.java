package com.sts.merchant.payment.mapper;

import com.google.gson.JsonObject;
import com.razorpay.Payment;
import com.sts.merchant.payment.response.Item;
import com.sts.merchant.payment.response.RazorpayFetchPaymentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RazorpayFetchResponseMapper {

    public RazorpayFetchPaymentResponse mapFromPaymentResponse(List<Payment> payments) {
        RazorpayFetchPaymentResponse response = new RazorpayFetchPaymentResponse();

        payments.forEach(payment -> {
            Item item = new Item();
            if (!payment.toJson().isNull("id"))
                item.setId(payment.get("id"));
            if (!payment.toJson().isNull("entity"))
                item.setEntity(payment.get("entity"));
            if (!payment.toJson().isNull("amount"))
                item.setAmount(payment.get("amount"));
            if (!payment.toJson().isNull("currency"))
                item.setCurrency(payment.get("currency"));
            if (!payment.toJson().isNull("status"))
                item.setStatus(payment.get("status"));
            if (!payment.toJson().isNull("order_id"))
                item.setOrderId(payment.get("order_id"));
            if (!payment.toJson().isNull("method"))
                item.setMethod(payment.get("method"));
            if (!payment.toJson().isNull("description"))
                item.setDescription(payment.get("description"));
            if (!payment.toJson().isNull("amount_reversed"))
                item.setAmountReversed(payment.get("amount_reversed"));
            if (!payment.toJson().isNull("captured"))
                item.setCaptured(payment.get("captured"));
            if (!payment.toJson().isNull("bank"))
                item.setBank(payment.get("bank"));
            if (!payment.toJson().isNull("email"))
                item.setEmail(payment.get("email"));
            if (!payment.toJson().isNull("contact"))
                item.setContact(payment.get("contact"));
            if (!payment.toJson().isNull("fee"))
                item.setFee(payment.get("fee"));
            if (!payment.toJson().isNull("tax"))
                item.setTax(payment.get("tax"));
            if (!payment.toJson().isNull("created_at"))
                item.setCreatedAt(payment.get("created_at"));
            response.getItems().add(item);
        });
        return response;
    }

}
