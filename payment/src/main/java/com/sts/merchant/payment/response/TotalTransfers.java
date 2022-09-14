package com.sts.merchant.payment.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TotalTransfers {

    private String id;
    private String entity;
    private String transferStatus;
    private String settlementStatus;
    private String source;
    private String recipient;
    private Integer amount;
    private String currency;
    private Integer amountReversed;
    private String recipientSettlementId;
    private RecipientSettlement recipientSettlement;
   // private String status;
    private Integer fees;
    private Integer tax;
    private String utr;
    //private Date createdAt;
}
