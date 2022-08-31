package com.sts.merchant.payment.service.serviceImpl;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sts.merchant.core.entity.*;
import com.sts.merchant.core.enums.*;
import com.sts.merchant.core.enums.Collection;
import com.sts.merchant.core.repository.*;
import com.sts.merchant.payment.request.CashfreeTransferRequest;
import com.sts.merchant.payment.response.*;
import com.sts.merchant.payment.service.CashfreeService;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.service.SettlementService;
import com.sts.merchant.payment.utils.Constants;
import com.sts.merchant.payment.utils.Crypto;
import com.sts.merchant.payment.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class CashFreeServiceImpl implements CashfreeService {
    private final SettlementRepository settlementRepository;
    private final CollectionSummaryRepository collectionSummaryRepository;
    private final CollectionRepository collectionRepository;
    private final LoanDetailRepository loanDetailRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final ClientInfoRepository clientInfoRepository;

    @Value("${app.encryption.secret}")
    private String secretKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CollectionService collectionService;

    public CashFreeServiceImpl(SettlementRepository settlementRepository, CollectionSummaryRepository collectionSummaryRepository, CollectionRepository collectionRepository, LoanDetailRepository loanDetailRepository, TransactionRepository transactionRepository, LoanAccountRepository loanAccountRepository, ClientInfoRepository clientInfoRepository) {
        this.settlementRepository = settlementRepository;
        this.collectionSummaryRepository = collectionSummaryRepository;
        this.collectionRepository = collectionRepository;
        this.loanDetailRepository = loanDetailRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.clientInfoRepository = clientInfoRepository;
    }

    @Override
    public void captureCashFreeSettlements() {
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    //fetch active cashFree loan accounts for this loan
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.CASHFREE.toString());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                            //Fetch Last Settlement for this loan account
                            log.info("Initiating cashFree last payment fetch at :{}", new Timestamp(System.currentTimeMillis()) + " for loan :" + loan.getLoanId() + ", accountId : " + loanAccountMapping.getAccountId());
                            Optional<SettlementDetail> lastSettlement = settlementRepository.findLastSettlementByLoanAndAccount(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.CASHFREE.toString(), InfoType.PG.toString());
                            if (lastSettlement.isPresent()) {
                                //Fetch client info
                                if (clientInfoDetail.isPresent()) {
                                    //If last settlement is present, start fetching settlements after last settlement date to current
                                    String startDate = DateTimeUtil.localDateTimeToString(DateTimeUtil.stringToLocalDateTime(lastSettlement.get().getSettledOn()));
                                    String endDate = DateTimeUtil.localDateTimeToString(LocalDateTime.now());
                                    String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                    String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                                    com.sts.merchant.core.response.Response<List<Settlement>> settlements = fetchCashFreeSettlements(startDate, endDate, clientId, clientSecret);
                                    if (settlements.getCode() == HttpStatus.OK.value()) {
                                        log.info("CashFree Settlements fetch successful for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                        if (settlements.getData().isEmpty()) {
                                            log.info("Settlements are empty for loan: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId() + " " + LocalDateTime.now());
                                        } else {
                                            for (Settlement settlement : settlements.getData()) {
                                                log.info("Saving settlement: {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                                try {
                                                    Optional<SettlementDetail> existingSettlement = settlementRepository.findExistingSettlement(settlement.getId());
                                                    if (existingSettlement.isPresent()) {
                                                        log.info("Settlement already exists! Skipping saving this settlement :{}", settlement.getId());
                                                    } else {
                                                        SettlementDetail settlementDetail = settlementService.saveSettlement(settlement, loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                                                        log.info("Saving settlement successful: {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                                    }
                                                } catch (Exception e) {
                                                    log.error("Error Saving settlement ! : {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                                }
                                            }
                                        }
                                    } else {
                                        log.info("CashFree Settlements fetch failed for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + settlements.getMessage());
                                    }
                                } else {
                                    log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                }
                            } else {
                                //Fetch client info
                                if (clientInfoDetail.isPresent()) {
                                    String startDate = DateTimeUtil.localDateTimeToString(loan.getPaymentDate());
                                    String endDate = DateTimeUtil.localDateTimeToString(LocalDateTime.now());
                                    String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                    String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                                    com.sts.merchant.core.response.Response<List<Settlement>> settlements = fetchCashFreeSettlements(startDate, endDate, clientId, clientSecret);
                                    if (settlements.getCode() == HttpStatus.OK.value()) {
                                        log.info("CashFree Settlements fetch successful for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                        if (settlements.getData().isEmpty()) {
                                            log.info("Settlements are empty for loan: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId() + " " + LocalDateTime.now());
                                        } else {
                                            for (Settlement settlement : settlements.getData()) {
                                                log.info("Saving settlement: {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                                try {
                                                    SettlementDetail settlementDetail = settlementService.saveSettlement(settlement, loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                                                    log.info("Saving settlement successful: {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                                } catch (Exception e) {
                                                    log.error("Error Saving settlement ! : {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                                }
                                            }
                                        }
                                    } else {
                                        log.info("CashFree Settlements fetch failed for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + settlements.getMessage());
                                    }
                                } else {
                                    log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            log.error("Error in capturing cashFree Settlements!");
        }
    }

    @Override
    public void transferPaymentByPayouts() {
        log.info("Initiating settlements fetch and money transfer");
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    //Fetching loan collection summary of current time
                    Optional<CollectionSummary> collectionSummary = collectionSummaryRepository.findAllCollectionSummary(loan.getLoanId());
                    if (collectionSummary.isPresent()) {
                        //Fetch the collection sequence time
                        Optional<Integer> collectionSequenceCount = collectionRepository.findCollectionSequenceCount(loan.getLoanId());
                        Integer collectionSequence;
                        //Set sequence incremented by 1
                        collectionSequence = collectionSequenceCount.map(integer -> integer + 1).orElse(1);

                        //fetch active cashFree loan accounts for this loan
                        Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.CASHFREE.toString());
                        if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                            for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                                //Fetch client info
                                Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.CASHFREE.toString(), InfoType.PAYOUTS.toString());
                                if (clientInfoDetail.isPresent()) {
                                    log.info("Fetching settlements for loanID: {}", loan.getLoanId() + ", account: " + loanAccountMapping.getLoanAccountMapId());
                                    List<String> status = new ArrayList<>();
                                    status.add(Transaction.INCOMPLETE.toString());
                                    status.add(Transaction.CAPTURED.toString());
                                    Optional<List<SettlementDetail>> settlements = settlementRepository.findSettlementsByStatus(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId(), status);
                                    if (settlements.isPresent() && !settlements.get().isEmpty()) {
                                        log.info("Fetching settlements success for loanID: {}", loan.getLoanId() + ", account: " + loanAccountMapping.getLoanAccountMapId());
                                        String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                        String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                                        CashfreeAuthorizeResponse tokenResponse = authorizePayouts(clientId, clientSecret);
                                        if (Objects.equals(tokenResponse.getSubCode(), "200")) {
                                            CashfreeBeneficiaryResponse beneficiaryResponse = getBeneficiaryDetails(tokenResponse.getData().getToken(), loanAccountMapping.getBeneficiaryId());
                                            if (Objects.equals(beneficiaryResponse.getSubCode(), "200")) {
                                                for (SettlementDetail settlementDetail : settlements.get()) {
                                                    try {
                                                        CashfreeBalanceResponse balanceResponse = getPayoutsBalance(tokenResponse.getData().getToken());
                                                        if (Objects.equals(balanceResponse.getSubCode(), "200")) {
                                                            BigDecimal availableBalance = BigDecimal.valueOf(Double.parseDouble(balanceResponse.getData().getAvailableBalance()));
                                                            BigDecimal amountToBeCollected = Constants.percentage(settlementDetail.getSettlementAmount(), BigDecimal.valueOf(loan.getCapPercentage()));
                                                            BigDecimal dailyAmount = amountToBeCollected.add(collectionSummary.get().getDailyCollectionAmountRec());
                                                            BigDecimal weeklyAmount = amountToBeCollected.add(collectionSummary.get().getWeeklyCollectionAmountRec());
                                                            BigDecimal monthlyAmount = amountToBeCollected.add(collectionSummary.get().getMonthlyCollectionAmountRec());
                                                            BigDecimal yearlyAmount = amountToBeCollected.add(collectionSummary.get().getYearlyCollectionAmountRec());

                                                            if ((amountToBeCollected.add(collectionSummary.get().getTotalCollectionAmountRec())).compareTo(collectionSummary.get().getLoanAmount()) > 0) {
                                                                log.info("Total collection amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                                break;
                                                            }

                                                            if (yearlyAmount.compareTo(collectionSummary.get().getYearlyLimitAmount()) > 0) {
                                                                log.info("Total Yearly amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                                break;
                                                            }

                                                            if (weeklyAmount.compareTo(collectionSummary.get().getWeeklyLimitAmount()) > 0) {
                                                                log.info("Total Weekly amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                                break;
                                                            }

                                                            if (monthlyAmount.compareTo(collectionSummary.get().getMonthlyLimitAmount()) > 0) {
                                                                log.info("Total Monthly amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                                break;
                                                            }

                                                            if (dailyAmount.compareTo(collectionSummary.get().getDailyLimitAmount()) > 0) {
                                                                log.info("Total Daily amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
                                                                break;
                                                            }

                                                            if (availableBalance.compareTo(amountToBeCollected) > 0) {
                                                                //Collect the amount to be collected from balance
                                                                CollectionDetail collectionDetail = collectionService.saveCollection(loan, collectionSequence, settlementDetail, amountToBeCollected);
                                                                collectionSequence++;

                                                                //if the collection is existing and incomplete, subtract the amount with already collected money.
                                                                if (Objects.equals(collectionDetail.getStatus(), Transaction.INCOMPLETE.toString())) {
                                                                    amountToBeCollected = amountToBeCollected.subtract(collectionDetail.getCollectionAmount());
                                                                }
                                                                if (amountToBeCollected.compareTo(BigDecimal.ONE) < 0) {
                                                                    log.error("Cannot collect amount less than 1.0 rupee loanId: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlement Id: " + settlementDetail.getSettlementId());
                                                                    collectionRepository.updateCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                } else {
                                                                    CashfreeTransferRequest transferRequest = new CashfreeTransferRequest();
                                                                    transferRequest.setAmount(amountToBeCollected.toString());
                                                                    transferRequest.setBeneId(loanAccountMapping.getBeneficiaryId());
                                                                    transferRequest.setTransferId(loanAccountMapping.getBeneficiaryId() + "_" + System.currentTimeMillis() / 1000);
                                                                    CashfreeTransferResponse transferResponse = transferMoneyAsync(tokenResponse.getData().getToken(), transferRequest);
                                                                    if (!transferResponse.getSubCode().isEmpty() && Long.parseLong(transferResponse.getSubCode()) < 300) {
                                                                        collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.COLLECTED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                        collectionRepository.updateCashfreeTransferIdByCollectionId(transferRequest.getTransferId(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                        settlementRepository.updateSettlementStatusById(Transaction.PROCESSED.toString(), settlementDetail.getId());
                                                                        log.info("Collection successful for loan :{}", loan.getLoanAmount() + " account :" + loanAccountMapping.getAccountId() + " SettlementId :" + collectionDetail.getSettlementId());
                                                                    } else {
                                                                        collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                        log.error("Error in collecting for loan :{}", loan.getLoanId() + " account :" + loanAccountMapping.getAccountId());
                                                                    }
                                                                }
                                                            } else {
                                                                //if amount is more than available balance, collect all the money from balance and mark as incomplete for later collection.
                                                                log.info("Not enough balance for: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlementId: " + settlementDetail.getSettlementId() + " Collecting total balance for this case.");
                                                                amountToBeCollected = availableBalance;
                                                                CollectionDetail collectionDetail = collectionService.saveCollection(loan, collectionSequence, settlementDetail, amountToBeCollected);
                                                                collectionSequence++;
                                                                if (amountToBeCollected.compareTo(BigDecimal.ONE) < 0) {
                                                                    log.error("Cannot collect amount less than 1.0 rupee loanId: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlement Id: " + settlementDetail.getSettlementId());
                                                                    collectionRepository.updateCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                } else {
                                                                    CashfreeTransferRequest transferRequest = new CashfreeTransferRequest();
                                                                    transferRequest.setAmount(amountToBeCollected.toString());
                                                                    transferRequest.setBeneId(loanAccountMapping.getBeneficiaryId());
                                                                    transferRequest.setTransferId(loanAccountMapping.getBeneficiaryId() + "_" + System.currentTimeMillis() / 1000);
                                                                    CashfreeTransferResponse transferResponse = transferMoneyAsync(tokenResponse.getData().getToken(), transferRequest);
                                                                    if (!transferResponse.getSubCode().isEmpty() && Long.parseLong(transferResponse.getSubCode()) < 300) {
                                                                        collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.COLLECTED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                        collectionRepository.updateCashfreeTransferIdByCollectionId(transferRequest.getTransferId(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                        settlementRepository.updateSettlementStatusById(Transaction.INCOMPLETE.toString(), settlementDetail.getId());
                                                                        log.info("Collection successful for loan :{}", loan.getLoanAmount() + " account :" + loanAccountMapping.getAccountId() + " SettlementId :" + collectionDetail.getSettlementId());
                                                                    } else {
                                                                        collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                        log.error("Error in collecting for loan :{}", loan.getLoanId() + " account :" + loanAccountMapping.getAccountId() + " message: " + transferResponse.getMessage());
                                                                    }
                                                                }
                                                            }

                                                        } else {
                                                            log.error("Error in getting balance for loan: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + balanceResponse.getMessage());
                                                        }
                                                    } catch (Exception exception) {
                                                        log.error("Error in collecting payment for loan :{}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + tokenResponse.getMessage());
                                                    }
                                                }
                                            } else {
                                                log.error("Error in fetching beneficiary details for loan: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + beneficiaryResponse.getMessage() + " beneficiaryId: " + loanAccountMapping.getBeneficiaryId());
                                            }
                                        } else {
                                            log.error("Error in authorizing payouts for loan: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + tokenResponse.getMessage());
                                        }
                                    } else {
                                        log.info("No settlements found to be transferred! loan: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId());
                                    }
                                } else {
                                    log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                                }
                            }
                        } else {
                            log.info("No accounts found for loan: {}", loan.getLoanId());
                        }
                    } else {
                        log.error("No collection summary for loan: {}", loan.getLoanId());
                    }
                }
            } else {
                log.info("No loans found!");
            }
        } catch (Exception exception) {
            log.error("Error in collecting loans! Please check your code");
        }
    }

    @Override
    public void checkPaymentTransferStatus() {
        log.info("Initiating payment transfers status enquiry");
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    //fetch active cashFree loan accounts for this loan
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.CASHFREE.toString());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {

                            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.CASHFREE.toString(), InfoType.PAYOUTS.toString());
                            if (clientInfoDetail.isPresent()) {
                                List<String> status = new ArrayList<>();
                                status.add(Transaction.PROCESSED.toString());
                                status.add(Transaction.INCOMPLETE.toString());

                                String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());

                                CashfreeAuthorizeResponse tokenResponse = authorizePayouts(clientId, clientSecret);
                                if (Objects.equals(tokenResponse.getSubCode(), "200")) {
                                    Optional<List<SettlementDetail>> settlements = settlementRepository.findSettlementsByStatus(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId(), status);
                                    if (settlements.isPresent() && !settlements.get().isEmpty()) {
                                        for (SettlementDetail settlementDetail : settlements.get()) {
                                            Optional<List<CollectionDetail>> collections = collectionRepository.findCollectionBySettlementId(loan.getLoanId(), settlementDetail.getSettlementId());
                                            if (collections.isPresent() && !collections.get().isEmpty()) {
                                                for (CollectionDetail collectionDetail : collections.get()) {
                                                    TransferStatusResponse transferStatusResponse = getTransferStatus(tokenResponse.getData().getToken(), collectionDetail.getTransferId());
                                                    if (Objects.equals(transferStatusResponse.getSubCode(), "200")) {

                                                    } else {

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {

        }
    }

    private CashfreeBeneficiaryResponse getBeneficiaryDetails(String token, String beneficiaryId) {
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

    private CashfreeTransferResponse transferMoneyAsync(String token, CashfreeTransferRequest transferRequest) throws IOException {
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

    private CashfreeBalanceResponse getPayoutsBalance(String token) throws IOException {
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

    private TransferStatusResponse getTransferStatus(String token, String transferId) throws IOException {
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

    private CashfreeAuthorizeResponse authorizePayouts(String clientId, String clientSecret) {
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

    private com.sts.merchant.core.response.Response<List<Settlement>> fetchCashFreeSettlements(String startDate, String endDate, String pgClientId, String pgClientSecret) {
        com.sts.merchant.core.response.Response<List<Settlement>> response = new com.sts.merchant.core.response.Response<>();
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(Constants.CashFreeConstants.APP_ID, pgClientId)
                    .addFormDataPart(Constants.CashFreeConstants.SECRET_KEY, pgClientSecret)
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
