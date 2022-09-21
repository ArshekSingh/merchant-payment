package com.sts.merchant.payment.service.serviceImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.razorpay.RazorpayException;
import com.sts.merchant.core.entity.*;
import com.sts.merchant.core.enums.*;
import com.sts.merchant.core.repository.*;
import com.sts.merchant.core.response.Response;
//import com.sts.merchant.payment.mapper.RazorpayFetchResponseMapper;
//import com.sts.merchant.payment.mapper.RzpSettlementMapper;
//import com.sts.merchant.payment.mapper.RzpTransferMapper;
import com.sts.merchant.payment.request.razorpay.dto.TransactionTransfer;
import com.sts.merchant.payment.request.razorpay.fetchTransaction.TransactionFetchRequest;
import com.sts.merchant.payment.request.razorpay.transferTransaction.TransactionTransferRequest;
//import com.sts.merchant.payment.response.TotalTransfers;
import com.sts.merchant.payment.response.razorpay.dto.TransferStatus;
import com.sts.merchant.payment.response.razorpay.fetchTransaction.TransactionFetchResponse;
import com.sts.merchant.payment.response.razorpay.transferStatus.TransferStatusResponse;
import com.sts.merchant.payment.response.razorpay.transferTransaction.TransactionTransferResponse;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.service.PaymentTransactionService;
import com.sts.merchant.payment.service.RazorpayService;
import com.sts.merchant.payment.utils.Constants;
import com.sts.merchant.payment.utils.Crypto;
import com.sts.merchant.payment.utils.DateTimeUtil;
import com.sts.merchant.payment.utils.RazorpayPG;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
//    @Autowired
//    RazorpayFetchResponseMapper razorpayFetchResponseMapper;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private PaymentTransactionService paymentTransactionService;

//    @Autowired
//    private RzpTransferMapper rzpTransferMapper;
//
//    @Autowired
//    private RzpSettlementMapper rzpSettlementMapper;

    public RazorpayServiceImpl(CollectionSummaryRepository collectionSummaryRepository, CollectionRepository collectionRepository, LoanDetailRepository loanDetailRepository, TransactionRepository transactionRepository, LoanAccountRepository loanAccountRepository, ClientInfoRepository clientInfoRepository) {
        this.collectionSummaryRepository = collectionSummaryRepository;
        this.collectionRepository = collectionRepository;
        this.loanDetailRepository = loanDetailRepository;
        this.transactionRepository = transactionRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.clientInfoRepository = clientInfoRepository;
    }


    public void fetchTransactionsAndRoute(String transactionStatus) {
        log.info("Initiating payment route to lender's razorpay account");
        try {
            Optional<List<LoanDetail>> loanDetails = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loanDetails.isPresent() && !loanDetails.get().isEmpty()) {

                for (LoanDetail loanDetail : loanDetails.get()) {
                    //Fetching loan collection summary of current time
                    Optional<CollectionSummary> collectionSummary = collectionSummaryRepository.findAllCollectionSummary(loanDetail.getLoanId());
                    if (collectionSummary.isPresent()) {
                        //Fetch the collection sequence time
                        Optional<Integer> collectionSequenceCount = collectionRepository.findCollectionSequenceCount();
                        Integer collectionSequence;
                        //Set sequence incremented by 1
                        collectionSequence = collectionSequenceCount.map(integer -> integer + 1).orElse(1);

                        Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loanDetail.getLoanId(), AccountType.RAZORPAY.toString());
                        if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                            for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                                if (loanDetail.getLoanId().equals(loanAccountMapping.getLoanId())) {
                                    //Find all captured transaction to process collection.
                                    Optional<List<TransactionDetail>> transactions = transactionRepository.findTransactionsByStatus(transactionStatus, loanDetail.getLoanId(), loanAccountMapping.getLoanAccountMapId());

                                    if (transactions.isPresent() && !transactions.get().isEmpty()) {
                                        for (TransactionDetail transaction : transactions.get()) {
                                            BigDecimal amountToBeCollected = transaction.getTransactionAmount().multiply(BigDecimal.valueOf(loanDetail.getPgShare())).divide(new BigDecimal(100), 2, RoundingMode.UP);
                                            BigDecimal monthlyAmount = amountToBeCollected.add(collectionSummary.get().getMonthlyCollectionAmountRec());

                                            if ((amountToBeCollected.add(collectionSummary.get().getTotalCollectionAmountRec())).compareTo(collectionSummary.get().getLoanAmount()) > 0) {
                                                log.info("Total collection amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                break;
                                            }

                                            if (monthlyAmount.compareTo(collectionSummary.get().getMonthlyLimitAmount()) > 0) {
                                                log.info("Total Monthly amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                break;
                                            }

                                            if ((amountToBeCollected.compareTo(BigDecimal.ONE) < 0)) {
                                                log.info("Cannot collect less than 1 rupee! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                break;

                                            }

                                            try {
                                                CollectionDetail collectionDetail = collectionService.saveCollection(loanDetail, collectionSequence, transaction, amountToBeCollected);
                                                collectionSequence++;
                                                Response<TransactionTransferResponse> transferResponse = transferFundsToLender(transaction, amountToBeCollected, loanAccountMapping.getFunderAccountId(), loanAccountMapping);
                                                if (transferResponse.getStatus().is2xxSuccessful()) {
                                                    if (!transferResponse.getData().getItems().isEmpty()) {
                                                       // log.info("tranferId: {}", transferResponse.getData().getItems().get(0).getId());
                                                        //Updating transfer id, collection status and transaction status...
                                                        collectionRepository.updateTransferIdByCollectionId(transferResponse.getData().getItems().get(0).getId(), loanDetail.getLoanId(), collectionDetail.getCollectionDetailPK().getCollectionSequence());
                                                        collectionRepository.updateCollectionStatusByCollectionId(Collection.COLLECTED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                                        transactionRepository.updateTransactionStatusById(Transaction.PROCESSED.toString(), transaction.getId());
                                                        log.info("Collection successful for loan :{}", loanDetail.getLoanId() + " account :" + loanAccountMapping.getAccountId() + " TransactionId :" + collectionDetail.getTransactionId() + "transferId" + transferResponse.getData().getItems().get(0).getId());
                                                    }
                                                } else {
                                                    collectionRepository.updateCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                                    log.error("Collection marked as failed for transaction :{}", transaction.getTransactionId() + ", collection sequence: " + collectionDetail.getCollectionDetailPK().getCollectionSequence() + " loan: " + loanDetail.getLoanId() + " account :" + loanAccountMapping.getAccountId());
                                                }
                                            } catch (Exception exception) {
                                                log.error("Error collecting for transaction: {}", transaction.getTransactionId(), exception);
                                                exception.printStackTrace();
                                            }

                                        }


                                    } else {
                                        log.info("No transactions to collect for loan: {}", loanDetail.getLoanId() + " account: " + loanAccountMapping.getAccountId());
                                    }


                                }


                            }

                        } else {

                            log.info("No loan accounts  found in system for loan:{}", loanDetail.getLoanId());
                        }
                    } else {
                        log.info("No collection summary for loan: {}", loanDetail.getLoanId());
                    }

                }

            } else {
                //If no vendor accounts are found, return
                log.info("No loans found in system");
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("exception while fetching transactions and route", e);
        }
    }

    @Override
    @Transactional
    public void fetchPaymentsAndRecord() {
        log.info("Initiating payment fetch and record from razorpay..");
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                log.info("Active loans found: {}", loans.get().size());
                for (LoanDetail loan : loans.get()) {
                    //fetch active loan accounts for this loan
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.RAZORPAY.toString());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        log.info("Active loan accounts found for loan: {}", loan.getLoanId() + ": " + loanAccountMappings.get().size());
                        for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                            //Fetch Last transaction for this account and vendor
                            log.info("Fetching last recorded transaction :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());
                            Optional<TransactionDetail> transactionDetail = transactionRepository.findLastTransactionByLoanAndAccount(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                            //Fetch razorpay payments
                            log.info("Fetching client info detail :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());
                            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
                            if (clientInfoDetail.isPresent()) {
                                log.info("Client info detail found :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());

                                String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());

                                RazorpayPG razorpayPG = new RazorpayPG(clientId, clientSecret);

                                if (transactionDetail.isPresent()) {
                                    log.info("Last recorded transaction found :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId() + ", Amount: " + transactionDetail.get().getTransactionAmount());
                                    try {
                                        TransactionFetchRequest transactionFetchRequest = new TransactionFetchRequest();
                                        int skip = 0;
                                        boolean recurse = true;
                                        List<TransactionFetchResponse> totalPayments = new ArrayList<>();
                                        ZonedDateTime zdt = transactionDetail.get().getTransactionDate().atZone(ZoneId.of(Constants.ZONE_ID));
                                        transactionFetchRequest.setFrom(zdt.toInstant().toEpochMilli() / 1000);
                                        transactionFetchRequest.setCount(100);
                                        transactionFetchRequest.setTo(System.currentTimeMillis() / 1000);
                                        List<TransactionFetchResponse> payments = fetchAllAvailableTransactions(transactionFetchRequest, razorpayPG, skip, recurse, totalPayments);
                                        if (!payments.isEmpty()) {
                                            mapPaymentsAndRecordTransactions(loan, payments, loanAccountMapping);
                                            log.info("total payments fetched :{}", payments.get(0).getItems().size() + ", at time : " + LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());
                                        } else {
                                            log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
                                        }

                                    } catch (RazorpayException e) {
                                        log.error("Error in razorpay fetch payments api for accountId: {}", loanAccountMapping.getAccountId() + ", loan id: " + loan.getLoanId() + e);
                                        e.printStackTrace();
                                    }
                                } else {
                                    try {
                                        TransactionFetchRequest transactionFetchRequest = new TransactionFetchRequest();
                                        int skip = 0;
                                        boolean recurse = true;
                                        List<TransactionFetchResponse> totalPayments = new ArrayList<>();
                                        ZonedDateTime zdt = loans.get().get(0).getDisbursementDate().atZone(ZoneId.of(Constants.ZONE_ID));
                                        transactionFetchRequest.setFrom(zdt.toInstant().toEpochMilli()/1000);
                                        transactionFetchRequest.setTo(System.currentTimeMillis() / 1000);
                                        transactionFetchRequest.setCount(100);
                                        List<TransactionFetchResponse> payments = fetchAllAvailableTransactions(transactionFetchRequest, razorpayPG, skip, recurse, totalPayments);
                                        if (payments.isEmpty()) {
                                            log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
                                        } else {
                                            mapPaymentsAndRecordTransactions(loan, payments, loanAccountMapping);
                                        }
                                    } catch (Exception e) {
                                        log.error("Error in razorpay fetch payments api for accountId: {}", loanAccountMapping.getAccountId() + ", loan id: " + loan.getLoanId() + e);
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


    @Transactional
    public void checkTransferStatus() {
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
                            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
                            if (clientInfoDetail.isPresent()) {
                                Optional<List<TransactionDetail>> transactionDetails = transactionRepository.findTransactionsByStatus(Transaction.PROCESSED.toString(), loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                                if (transactionDetails.isPresent() && !transactionDetails.get().isEmpty()) {
                                    for (TransactionDetail transactionDetail : transactionDetails.get()) {
                                        Optional<List<CollectionDetail>> collectionDetails = collectionRepository.findCollectionByTransactions(loan.getLoanId(), Collection.COLLECTED.toString(), transactionDetail.getTransactionId());
                                        if (collectionDetails.isPresent() && !collectionDetails.get().isEmpty()) {
                                            for (CollectionDetail collectionDetail : collectionDetails.get()) {

                                                String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                                String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());

                                                RazorpayPG razorpayPG = new RazorpayPG(clientId, clientSecret);

                                                //fetch transfer details based on transfer id
                                                try {
                                                    log.info("Getting transfer details at :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for transfer Id: " + collectionDetail.getTransferId() + ", for loan Id: " + loan.getLoanId() + ", account Id:" + loanAccountMapping.getAccountId() + " collection Id:" + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());

                                                    Response<TransferStatus> transfer = razorpayPG.fetchTransfer("trf_KK9SgE6624tLrH");
                                                    if(transfer.getStatus().is2xxSuccessful()){
                                                        //fetch settlement details of linked accounts
                                                        Response<TransferStatusResponse> transferStatusResponse = razorpayPG.fetchSettlementDetails();
                                                        if(transferStatusResponse.getStatus().is2xxSuccessful()) {

                                                            for (TransferStatus transferStatusResponse1 : transferStatusResponse.getData().getItems()) {
                                                                if (transfer.getData().getId().equals(transferStatusResponse1.getId())) {
                                                                    //Checking transfer statuses
                                                                    switch (transferStatusResponse1.getStatus()) {
                                                                        case Constants.RazorpayConstants.PENDING:
                                                                            log.info("Collection is not done because transfer status is {}", transferStatusResponse1.getStatus());
                                                                            break;
                                                                        case Constants.RazorpayConstants.PROCESSED:

                                                                            if (transferStatusResponse1.getSettlement_status().equals(Constants.RazorpayConstants.SETTLED)) {
                                                                                //update the utr in collection detail and update collection status SETTLED
                                                                                log.info("Amount of Rs: {}", transferStatusResponse1.getAmount() + " is settled in lender account: " + loanAccountMapping.getFunderAccountId() + ". TransferId: " + transferStatusResponse1.getId() + " loan Id: ", loan.getLoanId() + "account Id: ", loanAccountMapping.getAccountId() + " collection Id: ", collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: ", transactionDetail.getTransactionId());
                                                                                collectionRepository.updateCollectionStatusByCollectionId(Collection.SETTLED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                                                                collectionRepository.updateUtrId(transferStatusResponse1.getRecipient_settlement().getUtr(), transferStatusResponse1.getId());
                                                                                transactionRepository.updateTransactionStatusById(Transaction.SETTLED.toString(), transactionDetail.getId());

                                                                            } else {
                                                                                log.info("Settlement is pending for transfer: {}", transferStatusResponse1.getId() + ", for loan Id: " + loan.getLoanId() + ", account Id:" + loanAccountMapping.getAccountId() + " collection Id:" + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());
                                                                            }
                                                                            break;
                                                                        case Constants.RazorpayConstants.FAILED:
                                                                            //update the collection and transaction status to failed
                                                                            log.info("Amount of Rs: {}", transferStatusResponse1.getAmount() + " has failed to settle in lender account: " + loanAccountMapping.getFunderAccountId() + ". TransferId: " + transferStatusResponse1.getId() + " loan Id: ", loan.getLoanId() + "account Id: ", loanAccountMapping.getAccountId() + " collection Id: ", collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: ", transactionDetail.getTransactionId());
                                                                            log.info("update the collection and transaction status to FAILED for transfer id: {}", collectionDetail.getTransferId() + ", loan Id: " + loan.getLoanId() + ", account Id:" + loanAccountMapping.getAccountId() + ", collection Id:" + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());
                                                                            collectionRepository.updateCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                                                            transactionRepository.updateTransactionStatusById(Transaction.FAILED.toString(), transactionDetail.getId());

                                                                            break;
                                                                        case Constants.RazorpayConstants.REVERSED:
                                                                            //update collection status failed and transaction status reversed
                                                                            log.info("update collection status FAILED and transaction status REVERSED for transfer id {}", collectionDetail.getTransferId() + " loan Id: " + loan.getLoanId() + "account Id: " + loanAccountMapping.getAccountId() + " collection Id: " + collectionDetail.getCollectionDetailPK().getCollectionSequence() + " transaction Id: " + transactionDetail.getTransactionId());
                                                                            collectionRepository.updateCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                                                            transactionRepository.updateTransactionStatusById(Transaction.REVERSED.toString(), transactionDetail.getId());
                                                                            break;
                                                                    }

                                                                } else {
                                                                    log.info("transfer id is not found {}", transfer.getData().getId());

                                                                }

                                                            }
                                                        }
                                                        else {
                                                            log.error("There is error in fetching settlement details{}", transferStatusResponse.getMessage());

                                                        }
                                                    }
                                                    else{
                                                        log.error("Error in fetching transfer status{}", transfer.getStatus());
                                                    }

                                                } catch (Exception e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }

                                        } else {
                                            log.info("no collections to fetch");

                                        }
                                    }
                                } else {
                                    log.info("no transaction to fetch");
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


    private void mapPaymentsAndRecordTransactions(LoanDetail loan, List<TransactionFetchResponse> payments, LoanAccountMapping loanAccountMapping) {
        payments.forEach(payment -> {
            payment.getItems().forEach(item -> {
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
                    log.info("total payments fetched :{}", payments.get(0).getItems().size());
                }

            });

        });

    }

    private List<TransactionFetchResponse> fetchAllAvailableTransactions(TransactionFetchRequest transactionFetchRequest, RazorpayPG razorpayPG, int skip, boolean recurse, List<TransactionFetchResponse> totalPayments) throws Exception {
        List<TransactionFetchResponse> payment = fetchPaymentsFromRazorpay(transactionFetchRequest, razorpayPG, skip);
        while (recurse && payment.size() == 100) {
            totalPayments.addAll(payment);
            skip += 100;
            List<TransactionFetchResponse> paymentsAfterSkipping = fetchPaymentsFromRazorpay(transactionFetchRequest, razorpayPG, skip);
            totalPayments.addAll(paymentsAfterSkipping);
            if (paymentsAfterSkipping == null) recurse = false;
        }
        return payment;
    }

    private List<TransactionFetchResponse> fetchPaymentsFromRazorpay(TransactionFetchRequest transactionFetchRequest, RazorpayPG razorpayPG, Integer skip) throws Exception {
        Response<TransactionFetchResponse> response = razorpayPG.fetchRazorpayPayments(transactionFetchRequest, skip);
        List<TransactionFetchResponse> paymentFetchResponse = new ArrayList<>();
        paymentFetchResponse.add(response.getData());
        return paymentFetchResponse;
    }

    @Override
    public Response<TransactionTransferResponse> initiatePayment(TransactionTransferRequest map, String transactionId, LoanAccountMapping loanAccountMapping) {
        try {
            log.info("Initiating payment transfer for transactionId : {}", transactionId);
            log.info("Payment transfer at : {}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)));

            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
            if (clientInfoDetail.isPresent()) {

                String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                RazorpayPG razorpayPG = new RazorpayPG(clientId, clientSecret);

                Response<TransactionTransferResponse> razorPayTransferResponse = razorpayPG.routeTransactionToLender(transactionId, map);
                if (razorPayTransferResponse.getCode() == HttpStatus.OK.value()) {
                    log.info("Payment Completed for transactionId: {}", transactionId);
                    log.info("Payment completed at : {}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)));
                } else {
                    log.error("Error in collecting payment for transactionId: {}", transactionId);
                    log.error(razorPayTransferResponse.getMessage());
                }
                return razorPayTransferResponse;
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

    private Response<TransactionTransferResponse> transferFundsToLender(TransactionDetail transactionDetail, BigDecimal amountToBeCollected, String funderAccountId, LoanAccountMapping loanAccountMapping) {
        TransactionTransferRequest map = new TransactionTransferRequest();
       try {
           List<TransactionTransfer> transfers = new ArrayList<>();
           TransactionTransfer transactionTransfer = new TransactionTransfer();
           transactionTransfer.setAmount(amountToBeCollected.multiply(new BigDecimal(100)).intValue());
           transactionTransfer.setCurrency(transactionDetail.getCurrency());
           transactionTransfer.setAccount(funderAccountId);
           transfers.add(transactionTransfer);
           map.setTransfers(transfers);

           log.info("Route the amount to the lender's account {}", transactionTransfer.getAccount() + " " + "Amount" + " " + transactionTransfer.getAmount() / 100 + " " + transactionTransfer.getCurrency());

       }
       catch (Exception exception){
           log.error("Error in route the amount to the lender's account");
           exception.printStackTrace();

       }
        return initiatePayment(map, transactionDetail.getTransactionId(), loanAccountMapping);
    }
}