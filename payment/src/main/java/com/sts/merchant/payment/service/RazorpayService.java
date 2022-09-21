package com.sts.merchant.payment.service;


import com.razorpay.RazorpayException;
import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.request.razorpay.transferTransaction.TransactionTransferRequest;
import com.sts.merchant.payment.response.razorpay.transferTransaction.TransactionTransferResponse;

public interface RazorpayService {
    void fetchPaymentsAndRecord();

    Response<TransactionTransferResponse> initiatePayment(TransactionTransferRequest map, String transactionId, LoanAccountMapping loanAccountMapping);


    void checkTransferStatus() throws RazorpayException;

    void fetchTransactionsAndRoute(String transactionStatus);

}
