package com.sts.merchant.payment.response.cashfree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class CashfreeBeneficiary {
    private String beneId;
    private String name;
    private String email;
    private String phone;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String pincode;
    private String bankAccount;
    private String ifsc;
    private String status;
    private String maskedCard = null;
    private String vpa;
    private String addedOn;
}
