package com.sts.merchant.payment.scheduler;

import com.razorpay.RazorpayException;
import com.sts.merchant.core.enums.Transaction;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.service.CashfreeService;
import com.sts.merchant.payment.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RazorpayScheduler {
    @Autowired
    RazorpayService razorpayService;

//    @Autowired
//    CashfreeService cashfreeService;

    @Scheduled(fixedDelayString = "${app.scheduler.time}")
    public void fetchRazorpayPaymentsAndCollect() throws RazorpayException {
    //    cashfreeService.transferPaymentByPayouts();
  //    razorpayService.fetchPaymentsAndRecord();

        //transaction status CAPTURED and FAILED
//        String transactionStatus = Transaction.CAPTURED.toString();
//       razorpayService.fetchTransactionsAndRoute(transactionStatus);

       razorpayService.checkTransferStatus();




    }
}
