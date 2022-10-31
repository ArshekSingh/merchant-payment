package com.sts.merchant.payment.service;

import java.time.LocalDateTime;

public interface CollectionMailService {

    void generateExcelForCollectionDetail();

    void findCollectionDetailByDate(LocalDateTime collectionStartDate, LocalDateTime collectionEndDate);

    void sendMailWithAttachment(String to, String subject, String body, String fileName, byte[] ba);

    void sendMail(byte[] bytes);
}
