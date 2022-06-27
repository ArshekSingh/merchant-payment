package com.sts.merchant.payment.scheduler;

import com.sts.merchant.payment.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RazorpayScheduler {
    @Autowired
    RazorpayService razorpayService;

    @Scheduled(fixedDelay = 600000)
    public void fetchRazorpayPaymentsAndCollect(){
        razorpayService.fetchPaymentsAndRecord();
    }
}
