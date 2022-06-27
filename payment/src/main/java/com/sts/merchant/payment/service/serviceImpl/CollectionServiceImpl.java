package com.sts.merchant.payment.service.serviceImpl;

import com.sts.merchant.core.entity.CollectionDetail;
import com.sts.merchant.core.entity.CollectionDetailPK;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.entity.TransactionDetail;
import com.sts.merchant.core.repository.CollectionRepository;
import com.sts.merchant.payment.service.CollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Slf4j
@Service
public class CollectionServiceImpl implements CollectionService {
    @Autowired
    private CollectionRepository collectionRepository;


    @Transactional
    @Override
    public CollectionDetail saveCollection(LoanDetail loanDetail, Integer collectionSequence, TransactionDetail transaction, BigDecimal amountToBeCollected) {
        CollectionDetail collectionDetail = new CollectionDetail();
        collectionDetail.setCollectionAmount(amountToBeCollected);
        collectionDetail.setCollectionDate(LocalDateTime.ofInstant(LocalDateTime.now().toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
        collectionDetail.setCollectionDetailPK(new CollectionDetailPK(collectionSequence, loanDetail.getLoanId()));
        collectionDetail.setStatus("P");
        collectionDetail.setCollectionType(transaction.getTransactionMode());
        collectionDetail.setTransactionId(transaction.getTransactionId());
        collectionDetail.setCreatedBy("JOB");
        collectionDetail.setCreatedOn(LocalDateTime.ofInstant(LocalDateTime.now().toInstant(ZoneOffset.UTC), ZoneId.systemDefault()));
        collectionDetail = collectionRepository.save(collectionDetail);
        return collectionDetail;
    }
}
