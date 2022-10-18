package com.sts.merchant.payment.scheduler;

import com.sts.merchant.core.enums.Transaction;
import com.sts.merchant.payment.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RazorpayScheduler {
    @Autowired
    RazorpayService razorpayService;

    @Scheduled(fixedDelayString = "${app.scheduler.time}")
    public void scheduleFetchingRazorpayPayments() {
        razorpayService.fetchRazorpayPayments();
    }

    @Scheduled(fixedDelayString = "${app.scheduler.time}")
    public void scheduleRazorpayPaymentsTransfer() {
        razorpayService.transferMoney(Transaction.CAPTURED.toString());
    }

    @Scheduled(fixedDelayString = "${app.scheduler.time}")
    public void scheduleRazorpayTransferEnquiry() {
        razorpayService.transferEnquiry();
    }

    @Scheduled(fixedDelayString = "${app.scheduler.failed.time}")
    public void scheduleFailedRazorpayPaymentsTransfer() {
        razorpayService.transferMoney(Transaction.FAILED.toString());
    }

}
