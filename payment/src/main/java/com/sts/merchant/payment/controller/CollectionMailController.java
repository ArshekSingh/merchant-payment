package com.sts.merchant.payment.controller;


import com.sts.merchant.payment.service.CollectionMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/payment")
@Slf4j
public class CollectionMailController {

    @Autowired
    private CollectionMailService collectionMailService;

    @PostMapping("/downloadCollectionDetailExcel")
    public void generateExcelOfCollectionDetail() {
        collectionMailService.generateExcelForCollectionDetail();
    }
}
