package com.sts.merchant.payment.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sts.merchant.payment.request.cashfree.CashfreeTransferRequest;
import com.sts.merchant.payment.response.cashfree.*;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;

public class CashFreePayouts {
    private final String clientId;
    private final String clientSecret;

    public CashFreePayouts(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Autowired
    private static ObjectMapper objectMapper;

    public CashfreeBeneficiaryResponse getBeneficiaryDetails(String token, String beneficiaryId) {
        CashfreeBeneficiaryResponse beneficiaryResponse;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("https")
                    .host("payout-api.cashfree.com")
                    .addPathSegment("payout")
                    .addPathSegment("v1")
                    .addPathSegment("getBeneficiary")
                    .addPathSegment(beneficiaryId)
                    .build();
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .header("Authorization", "Bearer " + token)
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            beneficiaryResponse = objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException ioException) {
            beneficiaryResponse = new CashfreeBeneficiaryResponse();
            beneficiaryResponse.setSubCode("400");
            beneficiaryResponse.setMessage(beneficiaryResponse.getMessage());
            return beneficiaryResponse;
        } catch (Exception exception) {
            beneficiaryResponse = new CashfreeBeneficiaryResponse();
            beneficiaryResponse.setSubCode("400");
            beneficiaryResponse.setMessage(beneficiaryResponse.getMessage());
        }
        return beneficiaryResponse;
    }

    public CashfreeTransferResponse transferMoneyAsync(String token, CashfreeTransferRequest transferRequest) throws IOException {
        CashfreeTransferResponse transferResponse;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Gson gson = new Gson();
            String requestString = gson.toJson(transferRequest);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(requestString, JSON);
            Request request = new Request.Builder()
                    .url(Constants.CashFreeConstants.CASH_FREE_PAYOUT_LIVE_URL + "requestAsyncTransfer")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            transferResponse = objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException ioException) {
            transferResponse = new CashfreeTransferResponse();
            transferResponse.setSubCode("400");
            transferResponse.setMessage(transferResponse.getMessage());
            return transferResponse;
        } catch (Exception exception) {
            transferResponse = new CashfreeTransferResponse();
            transferResponse.setSubCode("400");
            transferResponse.setMessage(transferResponse.getMessage());
        }
        return transferResponse;
    }

    public CashfreeBalanceResponse getPayoutsBalance(String token) {
        CashfreeBalanceResponse balanceResponse;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder()
                    .url(Constants.CashFreeConstants.CASH_FREE_PAYOUT_LIVE_URL + "getBalance")
                    .header("Authorization", "Bearer " + token)
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            balanceResponse = objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException ioException) {
            balanceResponse = new CashfreeBalanceResponse();
            balanceResponse.setSubCode("400");
            balanceResponse.setMessage(balanceResponse.getMessage());
            return balanceResponse;
        } catch (Exception exception) {
            balanceResponse = new CashfreeBalanceResponse();
            balanceResponse.setSubCode("400");
            balanceResponse.setMessage(balanceResponse.getMessage());
        }
        return balanceResponse;
    }

    public TransferStatusResponse getTransferStatus(String token, String transferId) {
        TransferStatusResponse transferStatusResponse;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder()
                    .url(Constants.CashFreeConstants.CASH_FREE_PAYOUT_LIVE_URL + "getTransferStatus?transferId=" + transferId)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            transferStatusResponse = objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException ioException) {
            transferStatusResponse = new TransferStatusResponse();
            transferStatusResponse.setSubCode("400");
            transferStatusResponse.setMessage(transferStatusResponse.getMessage());
            return transferStatusResponse;
        } catch (Exception exception) {
            transferStatusResponse = new TransferStatusResponse();
            transferStatusResponse.setSubCode("400");
            transferStatusResponse.setMessage(transferStatusResponse.getMessage());
        }
        return transferStatusResponse;
    }

    public CashfreeAuthorizeResponse authorizePayouts() {
        CashfreeAuthorizeResponse tokenResponse;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            RequestBody requestBody = new FormBody.Builder().build();
            Request request = new Request.Builder()
                    .url(Constants.CashFreeConstants.CASH_FREE_PAYOUT_LIVE_URL + "authorize")
                    .addHeader(Constants.CashFreeConstants.PAYOUTS_CLIENT_ID, clientId)
                    .addHeader(Constants.CashFreeConstants.PAYOUTS_CLIENT_SECRET, clientSecret)
                    .post(requestBody)
                    .build();
            okhttp3.Response response = client.newCall(request).execute();
            tokenResponse = objectMapper.readValue(response.body().string(), new TypeReference<>() {
            });
        } catch (IOException ioException) {
            tokenResponse = new CashfreeAuthorizeResponse();
            tokenResponse.setSubCode("400");
            tokenResponse.setMessage(tokenResponse.getMessage());
            return tokenResponse;
        } catch (Exception exception) {
            tokenResponse = new CashfreeAuthorizeResponse();
            tokenResponse.setSubCode("400");
            tokenResponse.setMessage(tokenResponse.getMessage());
        }
        return tokenResponse;
    }
}
