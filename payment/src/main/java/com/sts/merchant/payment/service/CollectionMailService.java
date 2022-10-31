package com.sts.merchant.payment.service;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

public interface CollectionMailService {

    void generateExcelForCollectionDetail(HttpServletResponse httpServletResponse);

    void findCollectionDetailByDate(HttpServletResponse httpServerResponse, LocalDateTime collectionStartDate, LocalDateTime collectionEndDate);

    void sendMailWithAttachment(String to, String subject, String body, String fileName, byte[] ba);

    // void sendPreConfiguredMail(String message);

    void sendMail(byte[] bytes);
    //void sendMail(String to, String subject, String body);
}
