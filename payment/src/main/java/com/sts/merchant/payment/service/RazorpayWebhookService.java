package com.sts.merchant.payment.service;

import com.sts.merchant.core.dto.razorpayDto.Root;
import com.sts.merchant.core.response.Response;

public interface RazorpayWebhookService {
    Response capturePayment(Root request);
}
