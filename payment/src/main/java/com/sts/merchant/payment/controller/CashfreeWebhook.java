package com.sts.merchant.payment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "api")
@Slf4j
public class CashfreeWebhook {

    @RequestMapping(value = "/cashfree", method = RequestMethod.POST)
    private void captureCashFreePayment(HttpServletRequest request, HttpServletResponse httpServletResponse) throws IOException {
        log.info("Request: {}", request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
    }

}
