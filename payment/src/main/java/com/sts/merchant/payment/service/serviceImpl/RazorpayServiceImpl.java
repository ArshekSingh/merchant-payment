package com.sts.merchant.payment.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Transfer;
import com.sts.merchant.core.enums.*;
import com.sts.merchant.core.entity.*;
import com.sts.merchant.core.repository.*;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.mapper.RazorpayFetchResponseMapper;
import com.sts.merchant.payment.mapper.RazorpayTransferMap;
import com.sts.merchant.payment.response.RazorpayFetchPaymentResponse;
import com.sts.merchant.payment.response.RazorpayTransferResponse;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.service.PaymentTransactionService;
import com.sts.merchant.payment.service.RazorpayService;
import com.sts.merchant.payment.utils.Crypto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RazorpayServiceImpl implements RazorpayService {
    private final CollectionSummaryRepository collectionSummaryRepository;
    private final CollectionRepository collectionRepository;
    private final LoanDetailRepository loanDetailRepository;
    private final TransactionRepository transactionRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final ClientInfoRepository clientInfoRepository;

    @Value("${app.encryption.secret}")
    String secretKey;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    public RazorpayServiceImpl(CollectionSummaryRepository collectionSummaryRepository, CollectionRepository collectionRepository, LoanDetailRepository loanDetailRepository, TransactionRepository transactionRepository, LoanAccountRepository loanAccountRepository, ClientInfoRepository clientInfoRepository) {
        this.collectionSummaryRepository = collectionSummaryRepository;
        this.collectionRepository = collectionRepository;
        this.loanDetailRepository = loanDetailRepository;
        this.transactionRepository = transactionRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.clientInfoRepository = clientInfoRepository;
    }

    @Autowired
    RazorpayFetchResponseMapper razorpayFetchResponseMapper;

    @Override
    public void fetchTransactionsAndRoute(List<LoanDetail> loans, List<LoanAccountMapping> loanAccountMappings) {
        try {
            loanLoop:
            for (LoanDetail loanDetail : loans) {
                //Fetching loan collection summary of current time
                Optional<CollectionSummary> collectionSummary = collectionSummaryRepository.findAllCollectionSummary(loanDetail.getLoanId());
                if (collectionSummary.isPresent()) {
                    //Fetch the collection sequence time
                    Optional<Integer> collectionSequenceCount = collectionRepository.findCollectionSequenceCount(loanDetail.getLoanId());
                    Integer collectionSequence;
                    //Set sequence incremented by 1
                    collectionSequence = collectionSequenceCount.map(integer -> integer + 1).orElse(1);
                    accountLoop:
                    for (LoanAccountMapping accountMapping : loanAccountMappings) {
                        if (loanDetail.getLoanId().equals(accountMapping.getLoanId())) {
                            //Find all captured transaction to process collection.
                            Optional<List<TransactionDetail>> transactions = transactionRepository.findAllCapturedTransactionsByLoanAndAccount(Transaction.CAPTURED.toString(), loanDetail.getLoanId(), accountMapping.getLoanAccountMapId());
                            if (transactions.isPresent() && !transactions.get().isEmpty()) {
                                transactionLoop:
                                for (TransactionDetail transaction : transactions.get()) {
                                    BigDecimal amountToBeCollected = transaction.getTransactionAmount().multiply(BigDecimal.valueOf(loanDetail.getCapPercentage())).divide(new BigDecimal(100), 2, RoundingMode.UP);
                                    BigDecimal dailyAmount = amountToBeCollected.add(collectionSummary.get().getDailyCollectionAmountRec());
                                    BigDecimal weeklyAmount = amountToBeCollected.add(collectionSummary.get().getWeeklyCollectionAmountRec());
                                    BigDecimal monthlyAmount = amountToBeCollected.add(collectionSummary.get().getMonthlyCollectionAmountRec());
                                    BigDecimal yearlyAmount = amountToBeCollected.add(collectionSummary.get().getYearlyCollectionAmountRec());

                                    if ((amountToBeCollected.add(collectionSummary.get().getTotalCollectionAmountRec())).compareTo(collectionSummary.get().getLoanAmount()) > 0) {
                                        log.info("Total collection amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + accountMapping.getAccountId());
                                        break;
                                    }

                                    if (yearlyAmount.compareTo(collectionSummary.get().getYearlyLimitAmount()) > 0) {
                                        log.info("Total Yearly amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + accountMapping.getAccountId());
                                        break;
                                    }

                                    if (weeklyAmount.compareTo(collectionSummary.get().getWeeklyLimitAmount()) > 0) {
                                        log.info("Total Weekly amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + accountMapping.getAccountId());
                                        break;
                                    }

                                    if (monthlyAmount.compareTo(collectionSummary.get().getMonthlyLimitAmount()) > 0) {
                                        log.info("Total Monthly amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + accountMapping.getAccountId());
                                        break;
                                    }

                                    if (dailyAmount.compareTo(collectionSummary.get().getDailyLimitAmount()) > 0) {
                                        log.info("Total Daily amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + accountMapping.getAccountId());
                                        break;
                                    }
                                    try {
                                        CollectionDetail collectionDetail = collectionService.saveCollection(loanDetail, collectionSequence, transaction, amountToBeCollected);
                                        collectionSequence++;
                                        Response<RazorpayTransferResponse> transferResponse = transferFundsToParallelCap(transaction, amountToBeCollected, loanDetail.getFunderAccountId(), accountMapping);
                                        if (transferResponse.getStatus().is2xxSuccessful()) {
                                            //put these statuses in enum
                                            collectionRepository.updateCollectionStatusByCollectionId(Collection.COLLECTED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                            transactionRepository.updateTransactionStatusById(Transaction.PROCESSED.toString(), transaction.getId());
                                            log.info("Collection successful for loan :{}", loanDetail.getLoanAmount() + " account :" + accountMapping.getAccountId() + " TransactionId :" + collectionDetail.getTransactionId());
                                        } else {
                                            collectionRepository.updateCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                            log.error("Error in collecting for loan :{}", loanDetail.getLoanId() + " account :" + accountMapping.getAccountId());
                                        }
                                    } catch (Exception exception) {
                                        log.error("Error collecting for transaction: {}", transaction.getTransactionId(), exception);
                                        exception.printStackTrace();
                                    }
                                }
                            } else {
                                log.error("No transactions to collect for loan: {}", loanDetail.getLoanId() + " account: " + accountMapping.getAccountId());
                            }
                        }
                    }
                } else {
                    log.error("No collection summary for loan: {}", loanDetail.getLoanId());
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.error("exception while fetching transactions and route", exception);
        }
    }

    @Override
    @Transactional
    public void fetchPaymentsAndRecord() {
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    //fetch active loan accounts for this loan
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.RAZORPAY.toString());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                            //Fetch Last transaction for this account and vendor
                            Optional<TransactionDetail> transactionDetail = transactionRepository.findLastTransactionByLoanAndAccount(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                            //Fetch razorpay payments
                            log.info("Initiating last payment fetch at :{}", new Timestamp(System.currentTimeMillis()) + " for loan :" + loan.getLoanId() + ", accountId : " + loanAccountMapping.getAccountId());

                            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
                            if (clientInfoDetail.isPresent()) {
                                RazorpayClient razorpayClient = new RazorpayClient(Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt()),
                                        Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt()));
                                if (transactionDetail.isPresent()) {
                                    try {
                                        int skip = 0;
                                        boolean recurse = true;
                                        List<Payment> totalPayments = new ArrayList<>();
                                        Long from = transactionDetail.get().getTransactionDate().toInstant(ZoneOffset.UTC).toEpochMilli();
                                        List<Payment> payments = fetchAllAvailableTransactions(from, razorpayClient, skip, recurse, totalPayments);
                                        if (!payments.isEmpty()) {
                                            mapPaymentsAndRecordTransactions(loans.get(), payments, loanAccountMappings.get());
                                        } else {
                                            log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
                                        }
                                        log.info("total payments fetched :{}", payments.size());
                                        fetchTransactionsAndRoute(loans.get(), loanAccountMappings.get());
                                    } catch (RazorpayException e) {
                                        log.error("Error in razorpay fetch payments api for accountId: {}", loanAccountMapping.getAccountId(), e);
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        int skip = 0;
                                        boolean recurse = true;
                                        List<Payment> totalPayments = new ArrayList<>();
                                        Long from = loans.get().get(0).getPaymentDate().toInstant(ZoneOffset.UTC).toEpochMilli();
                                        List<Payment> payments = fetchAllAvailableTransactions(from, razorpayClient, skip, recurse, totalPayments);
                                        if (payments.isEmpty()) {
                                            log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
                                        } else {
                                            mapPaymentsAndRecordTransactions(loans.get(), payments, loanAccountMappings.get());
                                        }
                                        fetchTransactionsAndRoute(loans.get(), loanAccountMappings.get());
                                    } catch (RazorpayException e) {
                                        log.error("Error in razorpay fetch payments api for accountId: {}", loanAccountMapping.getAccountId(), e);
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                            }
                        }
                    } else {
                        log.info("No loan accounts  found in system for loan:{}", loan.getLoanId());
                    }
                }
            } else {
                //If no vendor accounts are found, return
                log.info("No loans found in system");
            }
        } catch (Exception exception) {
            log.error("exception while fetching payments :", exception);
        }
    }

    private void mapPaymentsAndRecordTransactions(List<LoanDetail> loans, List<Payment> payments, List<LoanAccountMapping> loanAccountMappings) {
        RazorpayFetchPaymentResponse response = razorpayFetchResponseMapper.mapFromPaymentResponse(payments);
        response.getItems().forEach(item -> {
            loans.forEach(loan -> {
                loanAccountMappings.forEach(loanAccountMapping -> {
                    if (item.getCaptured()) {
                        try {
                            Optional<TransactionDetail> transactionDetail = transactionRepository.findTransactionById(item.getId());
                            if (transactionDetail.isEmpty()) {
                                log.info("Initiating saving transactions for loanId: {}", loan.getLoanId() + " accountId " + loanAccountMapping.getAccountId());
                                log.info("Transaction amount {}", BigDecimal.valueOf(item.getAmount() / 100));
                                paymentTransactionService.saveRazorpayPaymentAsTransaction(item, loanAccountMapping.getAccountId(), loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                            }
                        } catch (JsonProcessingException e) {
                            log.error("error inserting transaction for loan :{}", loan.getLoanId() + " account: " + loanAccountMapping.getAccountId(), e);
                        } catch (Exception exception) {
                            log.error("error inserting transaction for loan :{}", loan.getLoanId() + " account: " + loanAccountMapping.getAccountId(), exception);
                        }
                        log.info("total payments fetched :{}", payments.size());
                    }
                });
            });
        });
    }

    private List<Payment> fetchAllAvailableTransactions(Long from, RazorpayClient razorpayClient, int skip,
                                                        boolean recurse, List<Payment> totalPayments) throws RazorpayException {
        List<Payment> payments = fetchPaymentsFromRazorpay(from, razorpayClient, skip);
        while (recurse && payments.size() == 100) {
            totalPayments.addAll(payments);
            skip += 100;
            List<Payment> paymentsAfterSkipping = fetchPaymentsFromRazorpay(from, razorpayClient, skip);
            totalPayments.addAll(paymentsAfterSkipping);
            if (paymentsAfterSkipping.isEmpty())
                recurse = false;
        }
        return payments;
    }

    private List<Payment> fetchPaymentsFromRazorpay(Long from, RazorpayClient razorpayClient, Integer skip) throws
            RazorpayException {
        JSONObject paymentRequest = new JSONObject();
        paymentRequest.put("from", from / 1000);
        paymentRequest.put("to", System.currentTimeMillis() / 1000);
        paymentRequest.put("count", 100);
        paymentRequest.put("skip", skip);
        List<Payment> payments = razorpayClient.payments.fetchAll(paymentRequest);
        return payments;
    }

    @Override
    public Response transferPayment(RazorpayTransferMap map, String transactionId, LoanAccountMapping loanAccountMapping) {
        try {
            log.info("Initiating payment transfer for transactionId : {}", transactionId);
            log.info("Payment transfer at : {}", new Timestamp(System.currentTimeMillis()));

            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
            if (clientInfoDetail.isPresent()) {

                RazorpayClient razorpayClient = new RazorpayClient(Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt()),
                        Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt()));
                JSONObject request = new JSONObject();

                JSONArray transfers = new JSONArray();

                JSONObject transfer = new JSONObject();
                transfer.put("amount", map.getAmount());
                transfer.put("currency", map.getCurrency());
                transfer.put("account", map.getAccount());

                transfers.put(transfer);
                request.put("transfers", transfers);

                List<Transfer> razorPayTransferResponse = razorpayClient.payments.transfer(transactionId, request);
                log.info("Payment Completed for transactionId: {}", transactionId);
                log.info("Payment completed at : {}", new Timestamp(System.currentTimeMillis()));
                return new Response<>("Transaction Completed Successfully", HttpStatus.OK, razorPayTransferResponse);

            } else {
                log.error("Client info not found for loanAccount : {} ", loanAccountMapping.getLoanAccountMapId());
                return new Response<>("Client info not found for loanAccount", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (RazorpayException exception) {
            log.error("razorpayException for transactionId : {} ", transactionId, exception);
            return new Response<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception exception) {
            log.error("exception for transactionId: {}", transactionId, exception);
            return new Response<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Response<RazorpayTransferResponse> transferFundsToParallelCap(TransactionDetail transactionDetail,
                                                                          BigDecimal amountToBeCollected,
                                                                          String funderAccountId,
                                                                          LoanAccountMapping loanAccountMapping) {
        RazorpayTransferMap map = new RazorpayTransferMap();
        map.setAccount(funderAccountId);
        map.setCurrency(transactionDetail.getCurrency());
        map.setAmount(amountToBeCollected.multiply(new BigDecimal(100)).intValue());

        return transferPayment(map, transactionDetail.getTransactionId(), loanAccountMapping);
    }
}