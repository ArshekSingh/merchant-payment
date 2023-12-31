package com.sts.merchant.payment.utils;

import com.google.gson.Gson;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.request.razorpay.fetchTransaction.TransactionFetchRequest;
import com.sts.merchant.payment.request.razorpay.transferTransaction.TransactionTransferRequest;
import com.sts.merchant.payment.response.razorpay.dto.Item;
import com.sts.merchant.payment.response.razorpay.dto.TransferStatus;
import com.sts.merchant.payment.response.razorpay.dto.Transfers;
import com.sts.merchant.payment.response.razorpay.fetchTransaction.TransactionFetchResponse;
import com.sts.merchant.payment.response.razorpay.transferTransaction.TransactionTransferResponse;
import okhttp3.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RazorpayPG {
    private final String clientId;
    private final String clientSecret;

    public RazorpayPG(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public Response<TransactionFetchResponse> fetchRazorpayPayments(TransactionFetchRequest transactionFetchRequest, Integer skip) {
        Response<TransactionFetchResponse> paymentResponse = new Response<>();
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("https").host("api.razorpay.com")
                    .addPathSegment("v1")
                    .addPathSegment("payments").addQueryParameter("from", transactionFetchRequest.getFrom().toString()).addQueryParameter("to", transactionFetchRequest.getTo().toString()).addQueryParameter("skip", skip.toString()).addQueryParameter("count", String.valueOf(transactionFetchRequest.getCount())).build();

            Request request = new Request.Builder().url(httpUrl).header(HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth(clientId, clientSecret, StandardCharsets.ISO_8859_1)).build();


            okhttp3.Response response = client.newCall(request).execute();

            String responseBodyString = response.body().string();
            Gson gson = new Gson();
            if (response.isSuccessful()) {
                TransactionFetchResponse obj = gson.fromJson(responseBodyString, TransactionFetchResponse.class);
                paymentResponse.setCode(HttpStatus.OK.value());
                paymentResponse.setData(obj);
                paymentResponse.setStatus(HttpStatus.OK);
                paymentResponse.setMessage(HttpStatus.OK.toString());
            } else {
                Item payment = gson.fromJson(responseBodyString, Item.class);
                paymentResponse.setCode(HttpStatus.BAD_REQUEST.value());
                paymentResponse.setMessage(payment.getError().getDescription());
                paymentResponse.setStatus(HttpStatus.BAD_REQUEST);
            }

        } catch (IOException ioException) {
            paymentResponse = new Response<>();
            paymentResponse.setStatus(HttpStatus.BAD_REQUEST);
            paymentResponse.setCode(HttpStatus.BAD_REQUEST.value());
            paymentResponse.setMessage(ioException.getMessage());
            return paymentResponse;
        } catch (Exception exception) {
            paymentResponse = new Response<>();
            paymentResponse.setStatus(HttpStatus.BAD_REQUEST);
            paymentResponse.setCode(HttpStatus.BAD_REQUEST.value());
            paymentResponse.setMessage(exception.getMessage());
        }
        return paymentResponse;
    }


    public Response<TransactionTransferResponse> routeTransactionToLender(String transactionId, TransactionTransferRequest transferRequest) {
        Response<TransactionTransferResponse> transferResponse = new Response<>();
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            Gson gson = new Gson();
            String requestString = gson.toJson(transferRequest);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(requestString, JSON);

            HttpUrl httpUrl = new HttpUrl.Builder().scheme("https").host("api.razorpay.com").addPathSegment("v1").addPathSegment("payments").addPathSegment(transactionId).addPathSegment("transfers").build();

            Request request = new Request.Builder().url(httpUrl).header(HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth(clientId, clientSecret, StandardCharsets.ISO_8859_1)).post(requestBody).build();

            okhttp3.Response response = client.newCall(request).execute();
            String responseBodyString = response.body().string();
            if (response.isSuccessful()) {
                TransactionTransferResponse obj = gson.fromJson(responseBodyString, TransactionTransferResponse.class);
                transferResponse.setStatus(HttpStatus.OK);
                transferResponse.setMessage(HttpStatus.OK.toString());
                transferResponse.setCode(HttpStatus.OK.value());
                transferResponse.setData(obj);
            } else {
                Transfers transfer = gson.fromJson(responseBodyString, Transfers.class);
                transferResponse.setCode(HttpStatus.BAD_REQUEST.value());
                transferResponse.setMessage(transfer.getError().getDescription());
                transferResponse.setStatus(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException ioException) {
            transferResponse = new Response<>();
            transferResponse.setStatus(HttpStatus.BAD_REQUEST);
            transferResponse.setCode(HttpStatus.BAD_REQUEST.value());
            transferResponse.setMessage(ioException.getMessage());
            return transferResponse;
        } catch (Exception exception) {
            transferResponse = new Response<>();
            transferResponse.setStatus(HttpStatus.BAD_REQUEST);
            transferResponse.setCode(HttpStatus.BAD_REQUEST.value());
            transferResponse.setMessage(exception.getMessage());
        }
        return transferResponse;

    }

    public Response<TransferStatus> fetchSettlementDetails(String transferId) {

        Response<TransferStatus> transfer = new Response<>();
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            HttpUrl httpUrl = new HttpUrl.Builder().scheme("https").host("api.razorpay.com").addPathSegment("v1").addPathSegment("transfers").addPathSegment(transferId).addQueryParameter("expand[]", Constants.RazorpayConstants.RECIPIENT_SETTLEMENT).build();

            Request request = new Request.Builder().url(httpUrl).header(HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth(clientId, clientSecret, StandardCharsets.ISO_8859_1)).build();

            okhttp3.Response response = client.newCall(request).execute();

            String responseBodyString = response.body().string();
            Gson gson = new Gson();
            if (response.isSuccessful()) {
                TransferStatus obj = gson.fromJson(responseBodyString, TransferStatus.class);
                transfer.setCode(HttpStatus.OK.value());
                transfer.setData(obj);
                transfer.setMessage(HttpStatus.OK.toString());
                transfer.setStatus(HttpStatus.OK);
            } else {
                TransferStatus obj = gson.fromJson(responseBodyString, TransferStatus.class);
                transfer.setCode(HttpStatus.BAD_REQUEST.value());
                transfer.setMessage(obj.getError().getDescription());
                transfer.setStatus(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException ioException) {
            transfer = new Response<>();
            transfer.setStatus(HttpStatus.BAD_REQUEST);
            transfer.setCode(HttpStatus.BAD_REQUEST.value());
            transfer.setMessage(ioException.getMessage());
            return transfer;
        } catch (Exception exception) {
            transfer = new Response<>();
            transfer.setStatus(HttpStatus.BAD_REQUEST);
            transfer.setCode(HttpStatus.BAD_REQUEST.value());
            transfer.setMessage(exception.getMessage());
        }
        return transfer;


    }


}
