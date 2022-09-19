package com.sts.merchant.payment.service.serviceImpl;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.sts.merchant.core.entity.CollectionDetail;
import com.sts.merchant.core.entity.SettlementDetail;
import com.sts.merchant.core.enums.Transaction;
import com.sts.merchant.core.repository.SettlementRepository;
import com.sts.merchant.payment.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettlementServiceImpl implements SettlementService {
    @Autowired
    SettlementRepository settlementRepository;

    @Override
    public SettlementDetail saveSettlement(Settlement settlement, Integer loanId, Integer accountId) {
        SettlementDetail settlementDetail = new SettlementDetail();
        settlementDetail.setSettlementAmount(settlement.getSettlementAmount());
        settlementDetail.setLoanId(loanId);
        settlementDetail.setLoanAccountMapId(accountId);
        settlementDetail.setStatus(Transaction.CAPTURED.toString());
        settlementDetail.setSettledOn(settlement.getSettledOn());
        settlementDetail.setTotalTxAmount(settlement.getTotalTxAmount());
        settlementDetail.setTransactionFrom(settlement.getTransactionFrom());
        settlementDetail.setTransactionTill(settlement.getTransactionTill());
        settlementDetail.setUtr(settlement.getUtr());
        settlementDetail.setSettlementId(settlement.getId());
        settlementDetail.setAmountSettled(settlement.getAmountSettled());
        settlementDetail.setAdjustment(settlement.getAdjustment());
        return settlementRepository.save(settlementDetail);
    }
}
