package com.sts.merchant.payment.service;

public interface CollectionMailService {

    void generateExcelForCollectionDetail(String loanId);

    void findCollectionDetailByLoanId(String loanId);

    void sendMailWithAttachment(String to, String subject, String body, String fileName, byte[] ba);

    void sendMail(byte[] bytes, String loanId);
}
