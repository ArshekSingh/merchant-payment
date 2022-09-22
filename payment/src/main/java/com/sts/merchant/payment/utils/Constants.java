package com.sts.merchant.payment.utils;

import java.math.BigDecimal;

public class Constants {
    public static final String ZONE_ID = "Asia/Kolkata";
    public static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
    public static final Integer FIRST_ELEMENT = 0;

    public static BigDecimal percentage(BigDecimal base, BigDecimal pct) {
        return base.multiply(pct).divide(ONE_HUNDRED);
    }

    public static class CashFreeConstants {
        public static final String APP_ID = "appId";
        public static final String SECRET_KEY = "secretKey";
        public static final String PAYOUTS_CLIENT_ID = "X-Client-Id";
        public static final String PAYOUTS_CLIENT_SECRET = "X-Client-Secret";
        public static final String START_DATE = "startDate";
        public static final String END_DATE = "endDate";
        public static final String CASH_FREE_PG_LIVE_URL = "https://api.cashfree.com/api/v1/";
        public static final String CASH_FREE_PAYOUT_LIVE_URL = "https://payout-api.cashfree.com/payout/v1/";
        public static final String METHOD_POST = "POST";
    }


    public static class RazorpayConstants{
        public static final String PENDING = "pending";
        public static final String PROCESSED = "processed";
        public static final String SETTLED = "settled";

        public static final String FAILED = "failed";

        public static final String REVERSED = "reversed";

        public static final String EXPAND = "expand[]";

        public static final String RECIPIENT_SETTLEMENT = "recipient_settlement";

    }
}