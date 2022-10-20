package com.sts.merchant.payment.scheduler;

import com.sts.merchant.core.enums.Transaction;
import com.sts.merchant.payment.service.CollectionMailService;
import com.sts.merchant.payment.service.RazorpayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

//@Component
@RestController
@RequestMapping(value = "/api/payment")
@Slf4j
public class RazorpayScheduler {
//    @Autowired
//    private RazorpayService razorpayService;

    @Autowired
    private CollectionMailService collectionMailService;

//    @Scheduled(fixedDelayString = "${app.scheduler.time}")
//    public void scheduleFetchingRazorpayPayments() {
//        razorpayService.fetchRazorpayPayments();
//    }
//
//    @Scheduled(fixedDelayString = "${app.scheduler.time}")
//    public void scheduleRazorpayPaymentsTransfer() {
//        razorpayService.transferMoney(Transaction.CAPTURED.toString());
//    }
//
//    @Scheduled(fixedDelayString = "${app.scheduler.time}")
//    public void scheduleRazorpayTransferEnquiry() {
//        razorpayService.transferEnquiry();
//    }
//
//    @Scheduled(fixedDelayString = "${app.scheduler.failed.time}")
//    public void scheduleFailedRazorpayPaymentsTransfer() {
//        razorpayService.transferMoney(Transaction.FAILED.toString());
//    }
//

    @PostMapping("/downloadCollectionDetailExcel")
    public void generateExcelOfCollectionDetail(HttpServletResponse httpServletResponse) {
        collectionMailService.generateExcelForCollectionDetail(httpServletResponse);
    }

}
