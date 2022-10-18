package com.sts.merchant.payment.service.serviceImpl;

import com.sts.merchant.core.entity.*;
import com.sts.merchant.core.enums.*;
import com.sts.merchant.core.repository.*;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.request.razorpay.fetchTransaction.TransactionFetchRequest;
import com.sts.merchant.payment.request.razorpay.model.TransactionTransfer;
import com.sts.merchant.payment.request.razorpay.transferTransaction.TransactionTransferRequest;
import com.sts.merchant.payment.response.razorpay.dto.TransferStatus;
import com.sts.merchant.payment.response.razorpay.fetchTransaction.TransactionFetchResponse;
import com.sts.merchant.payment.response.razorpay.transferTransaction.TransactionTransferResponse;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.service.PaymentTransactionService;
import com.sts.merchant.payment.service.RazorpayService;
import com.sts.merchant.payment.utils.Constants;
import com.sts.merchant.payment.utils.Crypto;
import com.sts.merchant.payment.utils.DateTimeUtil;
import com.sts.merchant.payment.utils.RazorpayPG;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Override
    @Transactional
    public void fetchRazorpayPayments() {
        log.info("Initiating payment fetch and record from razorpay..");
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                initiateFetchingPaymentsFromRazorpay(loans.get());
            } else {
                log.info("No loans found in system");
            }
        } catch (Exception exception) {
            log.error("exception while fetching payments :", exception);
        }
    }

    @Transactional
    public void transferMoney(String transactionStatus) {
        log.info("Initiating payment route to lender's razorpay account");
        try {
            Optional<List<LoanDetail>> loanDetails = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loanDetails.isPresent() && !loanDetails.get().isEmpty()) {
                for (LoanDetail loanDetail : loanDetails.get()) {
                    //Initiate money transfer for every loan.
                    initiateMoneyTransfer(transactionStatus, loanDetail);
                }
            } else {
                log.info("No loans found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception while transferring money! :{}", e.getMessage());
        }
    }

    @Transactional
    @Override
    public void transferEnquiry() {
        log.info("Initiating money transfer enquiry status");
        try {
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.RAZORPAY.toString());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        fetchClientInfo(loan, loanAccountMappings.get());
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

    private void initiateMoneyTransfer(String transactionStatus, LoanDetail loanDetail) {
        //Fetching loan collection summary of current time
        Optional<CollectionSummary> collectionSummary = collectionSummaryRepository.findAllCollectionSummary(loanDetail.getLoanId());
        if (collectionSummary.isPresent()) {
            fetchLoanAccountsFromDB(transactionStatus, loanDetail, collectionSummary.get());
        } else {
            log.info("No collection summary for loan: {}", loanDetail.getLoanId());
        }
    }

    private void fetchLoanAccountsFromDB(String transactionStatus, LoanDetail loanDetail, CollectionSummary collectionSummary) {
        //Fetch the collection sequence count & set sequence incremented by 1.
        Optional<Integer> collectionSequenceCount = collectionRepository.findCollectionSequenceCount();
        Integer collectionSequence = collectionSequenceCount.map(integer -> integer + 1).orElse(1);

        //Fetch loan accounts
        Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loanDetail.getLoanId(), AccountType.RAZORPAY.toString());
        if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
            fetchSavedTransactionsFromDB(transactionStatus, loanDetail, collectionSummary, collectionSequence, loanAccountMappings.get());
        } else {
            log.info("No loan accounts found in system for loan:{}", loanDetail.getLoanId());
        }
    }

    private void fetchSavedTransactionsFromDB(String transactionStatus, LoanDetail loanDetail, CollectionSummary collectionSummary, Integer collectionSequence, List<LoanAccountMapping> loanAccountMappings) {
        for (LoanAccountMapping loanAccountMapping : loanAccountMappings) {
            if (loanDetail.getLoanId().equals(loanAccountMapping.getLoanId())) {
                //Find all captured transaction to process collection.
                Optional<List<TransactionDetail>> transactions = transactionRepository.findTransactionsByStatus(transactionStatus, loanDetail.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                if (transactions.isPresent() && !transactions.get().isEmpty()) {
                    validateLoanAccountAndGenerateCollection(loanDetail, collectionSummary, collectionSequence, loanAccountMapping, transactions.get());
                } else {
                    log.info("No transactions to collect for loan: {}", loanDetail.getLoanId() + " account: " + loanAccountMapping.getAccountId());
                }
            }
        }
    }

    private void validateLoanAccountAndGenerateCollection(LoanDetail loanDetail, CollectionSummary collectionSummary, Integer collectionSequence, LoanAccountMapping loanAccountMapping, List<TransactionDetail> transactions) {
        for (TransactionDetail transaction : transactions) {
            BigDecimal amountToBeCollected = transaction.getTransactionAmount().multiply(BigDecimal.valueOf(loanDetail.getPgShare())).divide(new BigDecimal(100), 2, RoundingMode.UP);
            BigDecimal monthlyAmount = amountToBeCollected.add(collectionSummary.getMonthlyCollectionAmountRec());

            if (validateLoanAccount(loanDetail, collectionSummary, loanAccountMapping, transaction, amountToBeCollected, monthlyAmount))
                break;
            try {
                CollectionDetail collectionDetail = collectionService.saveCollection(loanDetail, collectionSequence, transaction, amountToBeCollected);
                collectionSequence++;
                Response<TransactionTransferResponse> transferResponse = transferFundsToLender(transaction, amountToBeCollected, loanAccountMapping.getFunderAccountId(), loanAccountMapping);
                if (transferResponse.getStatus().is2xxSuccessful()) {
                    if (!transferResponse.getData().getItems().isEmpty()) {
                        //Updating transfer id, collection status and transaction status...
                        collectionRepository.updateTransferIdByCollectionId(transferResponse.getData().getItems().get(Constants.FIRST_ELEMENT).getId(), loanDetail.getLoanId(), collectionDetail.getCollectionDetailPK().getCollectionSequence());
                        collectionRepository.updateCollectionStatusByCollectionId(Collection.COLLECTED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                        transactionRepository.updateTransactionStatusById(Transaction.PROCESSED.toString(), transaction.getId());
                        log.info("Collection successful for loan :{}", loanDetail.getLoanId() + " account :" + loanAccountMapping.getAccountId() + " TransactionId :" + collectionDetail.getTransactionId() + ", transferId: " + transferResponse.getData().getItems().get(0).getId());
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
    }

    private boolean validateLoanAccount(LoanDetail loanDetail, CollectionSummary collectionSummary, LoanAccountMapping loanAccountMapping, TransactionDetail transaction, BigDecimal amountToBeCollected, BigDecimal monthlyAmount) {
        if ((amountToBeCollected.add(collectionSummary.getTotalCollectionAmountRec())).compareTo(collectionSummary.getLoanAmount()) > 0) {
            log.info("Total collection amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
            return true;
        }

        if (monthlyAmount.compareTo(collectionSummary.getMonthlyLimitAmount()) > 0) {
            log.info("Total Monthly amount exceeding! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
            return true;
        }

        if ((amountToBeCollected.compareTo(BigDecimal.ONE) < 0)) {
            log.info("Cannot collect less than 1 rupee! Aborting collection for transaction: {}", transaction.getTransactionId() + ", loanId: " + loanDetail.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
            return true;
        }
        return false;
    }

    private void initiateFetchingPaymentsFromRazorpay(List<LoanDetail> loans) {
        log.info("Active loans found: {}", loans.size());
        for (LoanDetail loan : loans) {
            //fetch active loan accounts for this loan
            Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.RAZORPAY.toString());
            if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                log.info("Active loan accounts found for loan: {}", loan.getLoanId() + ": " + loanAccountMappings.get().size());
                for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                    try {
                        fetchLastTransactionRecordedAndFetchFromRazorpay(loan, loanAccountMapping);
                    } catch (Exception e) {
                        log.error("Error in fetching payments for loan: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId());
                    }
                }
            } else {
                log.info("No loan accounts  found in system for loan:{}", loan.getLoanId());
            }
        }
    }

    private void fetchLastTransactionRecordedAndFetchFromRazorpay(LoanDetail loan, LoanAccountMapping loanAccountMapping) throws Exception {
        //Fetch Last transaction for this account and vendor
        log.info("Fetching last recorded transaction :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());
        Optional<TransactionDetail> transactionDetail = transactionRepository.findLastTransactionByLoanAndAccount(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
        //Fetch razorpay payments
        log.info("Fetching client info detail :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());
        Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
        if (clientInfoDetail.isPresent()) {
            RazorpayPG razorpayPG = authorizeRazorpayAccount(loan, loanAccountMapping, clientInfoDetail.get());
            ZonedDateTime zonedDateTime = transactionDetail.map(detail -> detail.getTransactionDate().atZone(ZoneId.of(Constants.ZONE_ID))).orElseGet(() -> loan.getLoanStartDate().atZone(ZoneId.of(Constants.ZONE_ID)));
            try {
                fetchPayments(loan, loanAccountMapping, razorpayPG, zonedDateTime.toInstant().toEpochMilli() / 1000);
            } catch (Exception e) {
                log.error("Error in razorpay fetch payments api for accountId: {}", loanAccountMapping.getAccountId() + ", loan id: " + loan.getLoanId() + e);
                e.printStackTrace();
            }
        } else {
            log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
        }
    }

    private void fetchPayments(LoanDetail loan, LoanAccountMapping loanAccountMapping, RazorpayPG razorpayPG, Long from) {
        TransactionFetchRequest transactionFetchRequest = new TransactionFetchRequest();
        int skip = 0;
        boolean recurse = true;
        List<TransactionFetchResponse> totalPayments = new ArrayList<>();
        transactionFetchRequest.setFrom(from);
        transactionFetchRequest.setTo(System.currentTimeMillis() / 1000);
        transactionFetchRequest.setCount(100);
        List<TransactionFetchResponse> payments = fetchAllAvailableTransactions(transactionFetchRequest, razorpayPG, skip, recurse, totalPayments);
        if (payments.isEmpty()) {
            log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
        } else {
            mapPaymentsAndRecordTransactions(loan, payments, loanAccountMapping);
        }
    }

    @NotNull
    private RazorpayPG authorizeRazorpayAccount(LoanDetail loan, LoanAccountMapping loanAccountMapping, ClientInfoDetail clientInfoDetail) throws Exception {
        log.info("Client info detail found :{}", DateTimeUtil.localDateTimeToString(LocalDateTime.now()) + " for loan :" + loan.getLoanId() + ", accountId :" + loanAccountMapping.getAccountId());
        String clientId = Crypto.decrypt(clientInfoDetail.getInfo1(), secretKey, clientInfoDetail.getSalt());
        String clientSecret = Crypto.decrypt(clientInfoDetail.getInfo2(), secretKey, clientInfoDetail.getSalt());
        return new RazorpayPG(clientId, clientSecret);
    }

    private void fetchClientInfo(LoanDetail loan, List<LoanAccountMapping> loanAccountMappings) throws Exception {
        for (LoanAccountMapping loanAccountMapping : loanAccountMappings) {
            //Fetch Last transaction for this account and vendor
            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
            if (clientInfoDetail.isPresent()) {
                fetchProcessedTransactionsAndCollections(loan, loanAccountMapping, clientInfoDetail.get());
            } else {
                log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
            }
        }
    }

    private void fetchProcessedTransactionsAndCollections(LoanDetail loan, LoanAccountMapping loanAccountMapping, ClientInfoDetail clientInfoDetail) throws Exception {
        Optional<List<TransactionDetail>> transactionDetails = transactionRepository.findTransactionsByStatus(Transaction.PROCESSED.toString(), loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
        if (transactionDetails.isPresent() && !transactionDetails.get().isEmpty()) {
            for (TransactionDetail transactionDetail : transactionDetails.get()) {
                Optional<List<CollectionDetail>> collectionDetails = collectionRepository.findCollectionByTransactions(loan.getLoanId(), Collection.COLLECTED.toString(), transactionDetail.getTransactionId());
                if (collectionDetails.isPresent() && !collectionDetails.get().isEmpty()) {
                    authorizeRazorpayAndInitiateEnquiryStatus(loan, loanAccountMapping, clientInfoDetail, transactionDetail, collectionDetails.get());
                } else {
                    log.info("no collections to fetch");
                }
            }
        } else {
            log.info("no transaction to fetch");
        }
    }

    private void authorizeRazorpayAndInitiateEnquiryStatus(LoanDetail loan, LoanAccountMapping loanAccountMapping, ClientInfoDetail clientInfoDetail, TransactionDetail transactionDetail, List<CollectionDetail> collectionDetails) throws Exception {
        for (CollectionDetail collectionDetail : collectionDetails) {

            String clientId = Crypto.decrypt(clientInfoDetail.getInfo1(), secretKey, clientInfoDetail.getSalt());
            String clientSecret = Crypto.decrypt(clientInfoDetail.getInfo2(), secretKey, clientInfoDetail.getSalt());

            RazorpayPG razorpayPG = new RazorpayPG(clientId, clientSecret);

            //fetch transfer details based on transfer id
            try {
                log.info("Getting transfer details at :{}", DateTimeUtil.localDateTimeToString(LocalDateTime.now()) + " for transfer Id: " + collectionDetail.getTransferId() + ", for loan Id: " + loan.getLoanId() + ", account Id:" + loanAccountMapping.getAccountId() + " collection Id:" + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());

                Response<TransferStatus> transferResponse = razorpayPG.fetchSettlementDetails(collectionDetail.getTransferId());
                log.info("Razorpay settlement details for transferID: {}", collectionDetail.getTransferId() + " response: " + transferResponse.getMessage());
                if (transferResponse.getStatus().is2xxSuccessful()) {
                    switch (transferResponse.getData().getStatus()) {
                        case Constants.RazorpayConstants.PENDING:
                            log.info("Collection is not done because transfer status is {}", transferResponse.getData().getStatus());
                            break;
                        case Constants.RazorpayConstants.PROCESSED:

                            if (transferResponse.getData().getSettlement_status().equals(Constants.RazorpayConstants.SETTLED)) {
                                //update the utr in collection detail and update collection status SETTLED
                                log.info("Amount of Rs: {}", transferResponse.getData().getAmount() + " is settled in lender account: " + loanAccountMapping.getFunderAccountId() + ". TransferId: " + transferResponse.getData().getId() + " loan Id: " + loan.getLoanId() + "account Id: " + loanAccountMapping.getAccountId() + " collection Id: " + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());
                                collectionRepository.updateCollectionStatusByCollectionId(Collection.SETTLED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getTransactionId());
                                collectionRepository.updateUtrId(transferResponse.getData().getRecipient_settlement().getUtr(), transferResponse.getData().getId());
                                transactionRepository.updateTransactionStatusById(Transaction.SETTLED.toString(), transactionDetail.getId());

                            } else {
                                log.info("Settlement is pending for transfer: {}", transferResponse.getData().getId() + ", for loan Id: " + loan.getLoanId() + ", account Id:" + loanAccountMapping.getAccountId() + " collection Id:" + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());
                            }
                            break;
                        case Constants.RazorpayConstants.FAILED:
                            //update the collection and transaction status to failed
                            log.info("Amount of Rs: {}", transferResponse.getData().getAmount() + " has failed to settle in lender account: " + loanAccountMapping.getFunderAccountId() + ". TransferId: " + transferResponse.getData().getId() + " loan Id: " + loan.getLoanId() + "account Id: " + loanAccountMapping.getAccountId() + " collection Id: " + collectionDetail.getCollectionDetailPK().getCollectionSequence() + ", transaction Id: " + transactionDetail.getTransactionId());
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
                        default:
                            break;
                    }
                } else {
                    log.error("There is error in fetching settlement details{}", transferResponse.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Exception in fetching transfer details", e);
            }
        }
    }


    private void mapPaymentsAndRecordTransactions(LoanDetail loan, List<TransactionFetchResponse> payments, LoanAccountMapping loanAccountMapping) {
        payments.forEach(payment -> payment.getItems().forEach(item -> {
            if (Boolean.TRUE.equals(item.getCaptured())) {
                try {
                    Optional<TransactionDetail> transactionDetail = transactionRepository.findTransactionById(item.getId());
                    if (transactionDetail.isEmpty()) {
                        log.info("Initiating saving transactions for loanId: {}", loan.getLoanId() + " accountId " + loanAccountMapping.getAccountId());
                        log.info("Transaction amount {}", BigDecimal.valueOf(item.getAmount() / 100));
                        paymentTransactionService.saveRazorpayPaymentAsTransaction(item, loanAccountMapping.getAccountId(), loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                    }
                } catch (Exception e) {
                    log.error("error inserting transaction for loan :{}", loan.getLoanId() + " account: " + loanAccountMapping.getAccountId(), e);
                }
                log.info("total payments fetched :{}", payments.get(0).getItems().size());
            }

        }));

    }

    private List<TransactionFetchResponse> fetchAllAvailableTransactions(TransactionFetchRequest transactionFetchRequest, RazorpayPG razorpayPG, int skip, boolean recurse, List<TransactionFetchResponse> totalPayments) {
        List<TransactionFetchResponse> payment = fetchPaymentsFromRazorpay(transactionFetchRequest, razorpayPG, skip);
        while (recurse && payment.size() == 100) {
            totalPayments.addAll(payment);
            skip += 100;
            List<TransactionFetchResponse> paymentsAfterSkipping = fetchPaymentsFromRazorpay(transactionFetchRequest, razorpayPG, skip);
            totalPayments.addAll(paymentsAfterSkipping);
            if (paymentsAfterSkipping.isEmpty()) recurse = false;
        }
        return payment;
    }

    private List<TransactionFetchResponse> fetchPaymentsFromRazorpay(TransactionFetchRequest transactionFetchRequest, RazorpayPG razorpayPG, Integer skip) {
        Response<TransactionFetchResponse> response = razorpayPG.fetchRazorpayPayments(transactionFetchRequest, skip);
        log.info("Razorpay fetch transaction details at: {}", DateTimeUtil.localDateTimeToString(LocalDateTime.now()) + " response: " + response.getMessage());
        List<TransactionFetchResponse> paymentFetchResponse = new ArrayList<>();
        if (response.getStatus() == HttpStatus.BAD_REQUEST) {
            log.info("Razorpay fetch transaction failed, message: {}", response.getMessage());
        } else {
            paymentFetchResponse.add(response.getData());
        }
        return paymentFetchResponse;
    }

    public Response<TransactionTransferResponse> initiatePayment(TransactionTransferRequest map, String transactionId, LoanAccountMapping loanAccountMapping) {
        try {
            log.info("Initiating payment transfer for transactionId : {}", transactionId);
            log.info("Payment transfer at : {}", DateTimeUtil.localDateTimeToString(LocalDateTime.now()));

            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.RAZORPAY.toString(), InfoType.PG.toString());
            if (clientInfoDetail.isPresent()) {

                String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                RazorpayPG razorpayPG = new RazorpayPG(clientId, clientSecret);

                Response<TransactionTransferResponse> razorPayTransferResponse = razorpayPG.routeTransactionToLender(transactionId, map);
                log.info("Razorpay transfer details for transactionID: {}", transactionId + " response: " + razorPayTransferResponse.getMessage());
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
        } catch (Exception exception) {
            log.error("exception for transactionId: {}", transactionId, exception);
            return new Response<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Response<TransactionTransferResponse> transferFundsToLender(TransactionDetail transactionDetail, BigDecimal amountToBeCollected, String funderAccountId, LoanAccountMapping loanAccountMapping) {
        TransactionTransferRequest map = new TransactionTransferRequest();
        List<TransactionTransfer> transfers = new ArrayList<>();
        TransactionTransfer transactionTransfer = new TransactionTransfer();
        try {

            transactionTransfer.setAmount(amountToBeCollected.multiply(new BigDecimal(100)).intValue());
            transactionTransfer.setCurrency(transactionDetail.getCurrency());
            transactionTransfer.setAccount(funderAccountId);
            transfers.add(transactionTransfer);
            map.setTransfers(transfers);

            log.info("Route the amount to the lender's account {}", transactionTransfer.getAccount() + " " + "Amount" + " " + transactionTransfer.getAmount() / 100 + " " + transactionTransfer.getCurrency());

        } catch (Exception exception) {
            log.error("Error in route the amount to the lender's account {}", transactionTransfer.getAccount(), exception);
            return new Response<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
        return initiatePayment(map, transactionDetail.getTransactionId(), loanAccountMapping);
    }
}