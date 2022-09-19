package com.sts.merchant.payment.service;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.sts.merchant.core.entity.SettlementDetail;

public interface SettlementService {
    SettlementDetail saveSettlement(Settlement settlement, Integer loanId, Integer accountId) throws Exception;
}
