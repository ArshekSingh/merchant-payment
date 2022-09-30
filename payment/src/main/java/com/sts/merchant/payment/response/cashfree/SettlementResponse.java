package com.sts.merchant.payment.response.cashfree;

import com.cashfree.lib.pg.domains.response.Settlement;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SettlementResponse {
    private String status;
    private String message;
    private String lastId;
    private List<Settlement> settlements;
}
