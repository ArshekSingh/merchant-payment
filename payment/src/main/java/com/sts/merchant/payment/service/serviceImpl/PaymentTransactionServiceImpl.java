package com.sts.merchant.payment.service.serviceImpl;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sts.merchant.core.entity.TransactionDetail;
import com.sts.merchant.core.enums.Transaction;
import com.sts.merchant.core.repository.TransactionRepository;
import com.sts.merchant.payment.response.razorpay.dto.Item;
import com.sts.merchant.payment.service.PaymentTransactionService;
import com.sts.merchant.payment.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class PaymentTransactionServiceImpl implements PaymentTransactionService {
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    @Override
    public void saveRazorpayPaymentAsTransaction(Item item, String accountId, Integer loanId, Integer loanAccountMapId) throws JsonProcessingException {
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setTransactionId(item.getId());
        System.out.println("Transaction Amount " + BigDecimal.valueOf(item.getAmount() / 100));
        transactionDetail.setTransactionAmount(BigDecimal.valueOf(item.getAmount() / 100));
        transactionDetail.setTransactionStatus(Transaction.CAPTURED.toString());
        transactionDetail.setLoanAccountMapId(loanAccountMapId);
        transactionDetail.setTransactionTax(item.getTax());
        transactionDetail.setTransactionFee(item.getFee());
        transactionDetail.setTransactionDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(item.getCreated_at()), ZoneId.of(Constants.ZONE_ID)));
        transactionDetail.setCurrency(item.getCurrency());
        transactionDetail.setDescription(item.getDescription());
        transactionDetail.setAccountId(accountId);
        transactionDetail.setTransactionMode(item.getMethod());
        transactionDetail.setLoanId(loanId);
        transactionDetail.setBankName(item.getBank());
        transactionDetail.setContactNumber(item.getContact());
        transactionDetail.setEmailId(item.getEmail());
        transactionDetail.setResponse(objectMapper.writeValueAsString(item));
        transactionRepository.save(transactionDetail);
    }

    @Override
    public void saveCashFreePaymentAsTransaction(Settlement item, String accountId, Integer loanId, Integer loanAccountMapId) throws JsonProcessingException {
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setTransactionId(item.getId());
        transactionDetail.setTransactionAmount(item.getTotalTxAmount());
        transactionDetail.setTransactionStatus(Transaction.CAPTURED.toString());
        transactionDetail.setLoanAccountMapId(loanAccountMapId);
//        transactionDetail.setTransactionDate(item.getSettledOn());
        transactionDetail.setAccountId(accountId);
        transactionDetail.setLoanId(loanId);
//        transactionDetail.setContactNumber(item.getCustomerPhone());
//        transactionDetail.setEmailId(item.getCustomerEmail());
        transactionDetail.setResponse(objectMapper.writeValueAsString(item));
        transactionRepository.save(transactionDetail);
    }
}
