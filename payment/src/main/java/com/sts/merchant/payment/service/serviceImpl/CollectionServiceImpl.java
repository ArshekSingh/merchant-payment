package com.sts.merchant.payment.service.serviceImpl;

import com.sts.merchant.core.entity.*;
import com.sts.merchant.core.enums.Collection;
import com.sts.merchant.core.repository.CollectionRepository;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
public class CollectionServiceImpl implements CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    //For Razorpay
    @Transactional
    @Override
    public CollectionDetail saveCollection(LoanDetail loanDetail, Integer collectionSequence, TransactionDetail transaction, BigDecimal amountToBeCollected) {
        Optional<CollectionDetail> existingCollection = collectionRepository.findExistingCollectionByTxn(loanDetail.getLoanId(), transaction.getTransactionId());
        if (existingCollection.isEmpty()) {
            log.info("Saving Transaction Id : {}", transaction.getTransactionId());
            CollectionDetail collectionDetail = new CollectionDetail();
            collectionDetail.setCollectionAmount(amountToBeCollected);
            collectionDetail.setCollectionDate(DateTimeUtil.formatLocalDateTime(LocalDateTime.now()));
            collectionDetail.setCollectionDetailPK(new CollectionDetailPK(collectionSequence, loanDetail.getLoanId()));
            collectionDetail.setStatus(Collection.PENDING.toString());
            collectionDetail.setCollectionType(transaction.getTransactionMode());
            collectionDetail.setTransactionId(transaction.getTransactionId());
            collectionDetail.setCreatedBy("JOB");
            collectionDetail.setCreatedOn(DateTimeUtil.formatLocalDateTime(LocalDateTime.now()));
            collectionDetail = collectionRepository.save(collectionDetail);
            return collectionDetail;
        } else {
            return existingCollection.get();
        }
    }

    //For CashFree
    @Transactional
    @Override
    public CollectionDetail saveCollection(LoanDetail loanDetail, Integer collectionSequence, SettlementDetail settlementDetail, BigDecimal amountToBeCollected) {
        Optional<CollectionDetail> existingCollection = collectionRepository.findExistingCollectionBySettlementId(loanDetail.getLoanId(), settlementDetail.getSettlementId());
        if (existingCollection.isEmpty()) {
            log.info("Saving Settlement detail Id : {}", settlementDetail.getSettlementId());
            CollectionDetail collectionDetail = new CollectionDetail();
            collectionDetail.setCollectionAmount(amountToBeCollected);
            collectionDetail.setCollectionDate(LocalDateTime.ofInstant(LocalDateTime.now().toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
            collectionDetail.setCollectionDetailPK(new CollectionDetailPK(collectionSequence, loanDetail.getLoanId()));
            collectionDetail.setStatus(Collection.PENDING.toString());
            collectionDetail.setSettlementId(settlementDetail.getSettlementId());
            collectionDetail.setCreatedBy("JOB");
            collectionDetail.setCreatedOn(LocalDateTime.ofInstant(LocalDateTime.now().toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
            collectionDetail = collectionRepository.save(collectionDetail);
            return collectionDetail;
        } else {
            return existingCollection.get();
        }
    }
}
