package com.sts.merchant.payment.controller;

import com.sts.merchant.core.dto.razorpayDto.Root;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.service.RazorpayWebhookService;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "api")
@Slf4j
public class RazorpayWebhook {

    private final RazorpayWebhookService paymentService;

    @Autowired
    public RazorpayWebhook(RazorpayWebhookService paymentService) {
        this.paymentService = paymentService;
    }

    @RequestMapping(value = "/razorpay", method = RequestMethod.POST)
    public void captureWebhook(@RequestBody Root request, HttpServletResponse httpServletResponse) {
        log.info("razorpay webhook notification received , for accountId : {}", request.getAccount_id());
        Response response = paymentService.capturePayment(request);
        httpServletResponse.setStatus(response.getCode());
    }
}
