package com.sts.merchant.payment.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sts.merchant.core.entity.TransactionDetail;
import com.sts.merchant.core.repository.TransactionRepository;
import com.sts.merchant.payment.response.Item;
import com.sts.merchant.payment.service.PaymentTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
public class PaymentTransactionServiceImpl implements PaymentTransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Transactional
    @Override
    public void savePaymentAsTransaction(Item item, String accountId, Integer loanId, Integer loanAccountMapId) throws JsonProcessingException {
        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setTransactionId(item.getId());
        transactionDetail.setTransactionAmount(BigDecimal.valueOf(item.getAmount()));
        transactionDetail.setTransactionStatus("C");
        transactionDetail.setLoanAccountMapId(loanAccountMapId);
        transactionDetail.setTransactionTax(item.getTax());
        transactionDetail.setTransactionFee(item.getFee());
        transactionDetail.setTransactionDate(LocalDateTime.ofInstant(item.getCreatedAt().toInstant(), ZoneId.systemDefault()));
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
}
