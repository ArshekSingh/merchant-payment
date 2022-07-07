package com.sts.merchant.payment.service;


import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.mapper.RazorpayTransferMap;
import com.sts.merchant.payment.response.RazorpayTransferResponse;

import java.util.List;

public interface RazorpayService {
    void fetchPaymentsAndRecord();

    void fetchTransactionsAndRoute(List<LoanDetail> loans, List<LoanAccountMapping> loanAccountMappings);

    Response<RazorpayTransferResponse> transferPayment(RazorpayTransferMap map, String transactionId, LoanAccountMapping loanAccountMapping);
}
