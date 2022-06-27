package com.sts.merchant.payment.mapper;

import com.sts.merchant.core.dto.razorpayDto.Root;
import com.sts.merchant.core.entity.TransactionDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentDetailsMapper {

    public TransactionDetail dtoToEntity(Root request) {
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setCurrency(request.getPayload().getPayment().getEntity().getCurrency());
//        transactionDetail.setMerchantId(merchant.getMerchantId());
        transactionDetail.setDescription(request.getPayload().getPayment().getEntity().getDescription());
//        transactionDetail.setPaymentType(request.getPayload().getPayment().getEntity().getMethod());
        transactionDetail.setTransactionId(request.getPayload().getPayment().getEntity().getId());
//        transactionDetail.setInvoiceId(request.getPayload().getPayment().getEntity().getInvoice_id());
        transactionDetail.setTransactionAmount(BigDecimal.valueOf(request.getPayload().getPayment().getEntity().getAmount()));
//        transactionDetail.setTransactionDate(LocalDate.now());
        transactionDetail.setTransactionStatus("C");
        return transactionDetail;
    }
}
