package com.sts.merchant.payment.utils;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.response.SettlementResponse;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

public class CashFreePG {
    private final String clientId;
    private final String clientSecret;

    @Autowired
    private ObjectMapper objectMapper;

    public CashFreePG(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Response<List<Settlement>> fetchCashFreeSettlements(String startDate, String endDate) {
        com.sts.merchant.core.response.Response<List<Settlement>> response = new com.sts.merchant.core.response.Response<>();
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(Constants.CashFreeConstants.APP_ID, clientId)
                    .addFormDataPart(Constants.CashFreeConstants.SECRET_KEY, clientSecret)
                    .addFormDataPart(Constants.CashFreeConstants.START_DATE, startDate)
                    .addFormDataPart(Constants.CashFreeConstants.END_DATE, endDate)
                    .build();
            Request request = new Request.Builder()
                    .url(Constants.CashFreeConstants.CASH_FREE_PG_LIVE_URL + "settlements")
                    .method(Constants.CashFreeConstants.METHOD_POST, body)
                    .build();
            okhttp3.Response networkResponse = client.newCall(request).execute();
            SettlementResponse settlementResponse = objectMapper.readValue(networkResponse.body().string(), new TypeReference<>() {
            });
            response.setData(settlementResponse.getSettlements());
            response.setCode(HttpStatus.OK.value());
            return response;
        } catch (IOException ioException) {
            response.setMessage(ioException.getMessage());
            response.setCode(HttpStatus.BAD_REQUEST.value());
            return response;
        } catch (Exception exception) {
            response.setMessage(exception.getMessage());
            response.setCode(HttpStatus.BAD_REQUEST.value());
            return response;
        }
    }
}
