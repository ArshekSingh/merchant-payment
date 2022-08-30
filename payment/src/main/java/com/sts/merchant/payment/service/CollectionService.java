package com.sts.merchant.payment.service;

import com.sts.merchant.core.entity.CollectionDetail;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.entity.SettlementDetail;
import com.sts.merchant.core.entity.TransactionDetail;

import java.math.BigDecimal;

public interface CollectionService {
    CollectionDetail saveCollection(LoanDetail loanDetail, Integer collectionSequence, TransactionDetail transaction, BigDecimal amountToBeCollected);

    CollectionDetail saveCollection(LoanDetail loanDetail, Integer collectionSequence, SettlementDetail settlementDetail, BigDecimal amountToBeCollected);
}
