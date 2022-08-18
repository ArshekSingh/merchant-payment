package com.sts.merchant.payment.scheduler;

import com.sts.merchant.payment.service.CashfreeService;
import com.sts.merchant.payment.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RazorpayScheduler {
//    @Autowired
//    RazorpayService razorpayService;

    @Autowired
    CashfreeService cashfreeService;

    @Scheduled(fixedDelayString = "${app.scheduler.time}")
    public void fetchRazorpayPaymentsAndCollect() {
//        cashfreeService.fetchPaymentsAndRecord();
//        razorpayService.fetchPaymentsAndRecord();
    }
}
