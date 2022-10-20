package com.sts.merchant.payment.service;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

public interface CollectionMailService {

    void generateExcelForCollectionDetail(HttpServletResponse httpServletResponse);

    void findCollectionDetailByDate(HttpServletResponse httpServerResponse, LocalDateTime collectionStartDate, LocalDateTime collectionEndDate);
}
