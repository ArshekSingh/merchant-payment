package com.sts.merchant.payment.service.serviceImpl;

import com.cashfree.lib.pg.domains.response.Settlement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sts.merchant.core.entity.*;
import com.sts.merchant.core.enums.*;
import com.sts.merchant.core.enums.Collection;
import com.sts.merchant.core.repository.*;
import com.sts.merchant.payment.request.cashfree.CashfreeTransferRequest;
import com.sts.merchant.payment.response.cashfree.*;
import com.sts.merchant.payment.service.CashfreeService;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.service.SettlementService;
import com.sts.merchant.payment.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    /**
     * CAPTURING CASH FREE SETTLEMENTS
     * 1. Fetch settlements for every CashFree account mapped to loan.
     * 2. Fetch all the information related to cashFree account and access the cashFree Apis
     * 3. If the loan account has no settlements recorded, start fetching settlements from cashFree account from loan disbursement date to current date.
     * Else start fetching settlements from last recorded settlement date to current date.
     * 4. Save settlements in DB having a check of existing settlement detail.
     */
    @Override
    public void captureCashFreeSettlements() {
        log.info("Initiating CashFree settlements fetch at: {}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)));
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    log.info("Loans fetch successful. Total Active Loans: {}", loans.get());
                    //fetch active cashFree loan accounts for this loan
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.CASHFREE.toString());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                            log.info("Loan account fetch successful. Total Active Loan Accounts: {}", loanAccountMappings.get().size() + ", loan: " + loan.getLoanId());
                            //Fetch Last Settlement for this loan account
                            log.info("Initiating cashFree last payment fetch at :{}", new Timestamp(System.currentTimeMillis()) + " for loan :" + loan.getLoanId() + ", accountId : " + loanAccountMapping.getAccountId());
                            Optional<SettlementDetail> lastSettlement = settlementRepository.findLastSettlementByLoanAndAccount(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());

                            //Fetch client info for client id and secret
                            Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.CASHFREE.toString(), InfoType.PG.toString());
                            String startDate = lastSettlement.map(settlementDetail -> DateTimeUtil.localDateTimeToString(DateTimeUtil.stringToLocalDateTime(settlementDetail.getSettledOn()))).orElseGet(() -> DateTimeUtil.localDateTimeToString(loan.getDisbursementDate()));
                            fetchAndSaveCashfreeSettlements(startDate, loan, loanAccountMapping, lastSettlement, clientInfoDetail);
                        }
                    } else {
                        log.error("No accounts mapped for loan: {}", loan.getLoanId());
                    }
                }
            } else {
                log.error("No Loans found to execute!");
            }
        } catch (Exception exception) {
            log.error("Error in capturing cashFree Settlements!");
        }
    }

    @Override
    public void transferCapturedPaymentsByPayouts() {
        log.info("Initiating captured settlements fetch and money transfer at :{}", LocalDateTime.now().atZone(ZoneId.of(Constants.ZONE_ID)));
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                log.info("Loans fetch successful. Total Active Loans: {}", loans.get());
                for (LoanDetail loan : loans.get()) {
                    //Fetching loan collection summary of current time
                    Optional<CollectionSummary> collectionSummary = collectionSummaryRepository.findAllCollectionSummary(loan.getLoanId());
                    if (collectionSummary.isPresent()) {
                        log.info("Collection summary fetch successful for Loan: {}", loan.getLoanId());
                        //Fetch the latest collection sequence
                        Optional<Integer> collectionSequenceCount = collectionRepository.findCollectionSequenceCount();
                        Integer collectionSequence;
                        //Set sequence incremented by 1
                        collectionSequence = collectionSequenceCount.map(integer -> integer + 1).orElse(1);

                        //fetch active cashFree loan accounts for this loan
                        Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId(), AccountType.CASHFREE.toString());
                        if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                            log.info("Loan account fetch successful. Total Active Loan Accounts: {}", loanAccountMappings.get().size() + ", loan: " + loan.getLoanId());
                            for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                                //Fetch client info
                                Optional<ClientInfoDetail> clientInfoDetail = clientInfoRepository.findClientInfoByAccount(loanAccountMapping.getLoanAccountMapId(), AccountType.CASHFREE.toString(), InfoType.PAYOUTS.toString());
                                if (clientInfoDetail.isPresent()) {
                                    log.info("Fetching settlements for loanID: {}", loan.getLoanId() + ",account: " + loanAccountMapping.getLoanAccountMapId());
                                    List<String> status = new ArrayList<>();
                                    status.add(Transaction.CAPTURED.toString());
                                    Optional<List<SettlementDetail>> settlements = settlementRepository.findSettlementsByStatus(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId(), status);
                                    if (settlements.isPresent() && !settlements.get().isEmpty()) {
                                        log.info("Fetching settlements success for loanID: {}", loan.getLoanId() + ",account: " + loanAccountMapping.getLoanAccountMapId());
                                        String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                        String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                                        CashFreePayouts cashFreePayouts = new CashFreePayouts(clientId, clientSecret);
                                        CashfreeAuthorizeResponse tokenResponse = cashFreePayouts.authorizePayouts();
                                        if (Objects.equals(tokenResponse.getSubCode(), "200")) {
                                            CashfreeBeneficiaryResponse beneficiaryResponse = cashFreePayouts.getBeneficiaryDetails(tokenResponse.getData().getToken(), loanAccountMapping.getBeneficiaryId());
                                            if (Objects.equals(beneficiaryResponse.getSubCode(), "200")) {
                                                for (SettlementDetail settlementDetail : settlements.get()) {
                                                    try {
                                                        CashfreeBalanceResponse balanceResponse = cashFreePayouts.getPayoutsBalance(tokenResponse.getData().getToken());
                                                        if (Objects.equals(balanceResponse.getSubCode(), "200")) {
                                                            BigDecimal availableBalance = BigDecimal.valueOf(Double.parseDouble(balanceResponse.getData().getAvailableBalance()));
                                                            BigDecimal amountToBeCollected = Constants.percentage(settlementDetail.getSettlementAmount(), BigDecimal.valueOf(loan.getPgShare()));
                                                            BigDecimal monthlyAmount = amountToBeCollected.add(collectionSummary.get().getMonthlyCollectionAmountRec());

                                                            if (validateAccountLimits(loan, collectionSummary, loanAccountMapping, settlementDetail, amountToBeCollected, monthlyAmount))
                                                                break;

                                                            if (availableBalance.compareTo(amountToBeCollected) > 0) {
                                                                //Collect the amount to be collected from balance
                                                                CollectionDetail collectionDetail = collectionService.saveCollection(loan, collectionSequence, settlementDetail, amountToBeCollected);
                                                                collectionSequence++;

                                                                if (amountToBeCollected.compareTo(BigDecimal.ONE) < 0) {
                                                                    log.error("Cannot collect amount less than 1.0 rupee loanId: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlement Id: " + settlementDetail.getSettlementId());
                                                                    collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                } else {
                                                                    String updatedSettlementStatus = Transaction.PROCESSED.toString();
                                                                    transferMoneyToLender(loan, loanAccountMapping, tokenResponse, settlementDetail, amountToBeCollected, collectionDetail, updatedSettlementStatus, cashFreePayouts);
                                                                }
                                                            } else {
                                                                //if amount is more than available balance, collect all the money from balance and mark as incomplete for later collection.
                                                                log.info("Not enough balance for: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlementId: " + settlementDetail.getSettlementId() + " Collecting total balance for this case.");
                                                                amountToBeCollected = availableBalance;
                                                                CollectionDetail collectionDetail = collectionService.saveCollection(loan, collectionSequence, settlementDetail, amountToBeCollected);
                                                                collectionSequence++;
                                                                if (amountToBeCollected.compareTo(BigDecimal.ONE) < 0) {
                                                                    log.error("Cannot collect amount less than 1.0 rupee loanId: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlement Id: " + settlementDetail.getSettlementId());
                                                                    collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                } else {
                                                                    String updatedSettlementStatus = Transaction.INCOMPLETE.toString();
                                                                    transferMoneyToLender(loan, loanAccountMapping, tokenResponse, settlementDetail, amountToBeCollected, collectionDetail, updatedSettlementStatus, cashFreePayouts);
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
    public void transferFailedAndIncompletePaymentsByPayouts() {
        log.info("Initiating failed and incomplete settlements fetch and money transfer");
        try {
            //fetch active loans
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    //Fetching loan collection summary of current time
                    Optional<CollectionSummary> collectionSummary = collectionSummaryRepository.findAllCollectionSummary(loan.getLoanId());
                    if (collectionSummary.isPresent()) {
                        //Fetch the latest collection sequence
                        Optional<Integer> collectionSequenceCount = collectionRepository.findCollectionSequenceCount();
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
                                    log.info("Fetching settlements for loanID: {}", loan.getLoanId() + ",account: " + loanAccountMapping.getLoanAccountMapId());
                                    List<String> status = new ArrayList<>();
                                    status.add(Transaction.FAILED.toString());
                                    status.add(Transaction.INCOMPLETE.toString());
                                    Optional<List<SettlementDetail>> settlements = settlementRepository.findSettlementsByStatus(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId(), status);
                                    if (settlements.isPresent() && !settlements.get().isEmpty()) {
                                        log.info("Fetching settlements success for loanID: {}", loan.getLoanId() + ",account: " + loanAccountMapping.getLoanAccountMapId());
                                        String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
                                        String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
                                        CashFreePayouts cashFreePayouts = new CashFreePayouts(clientId, clientSecret);
                                        CashfreeAuthorizeResponse tokenResponse = cashFreePayouts.authorizePayouts();
                                        if (Objects.equals(tokenResponse.getSubCode(), "200")) {
                                            CashfreeBeneficiaryResponse beneficiaryResponse = cashFreePayouts.getBeneficiaryDetails(tokenResponse.getData().getToken(), loanAccountMapping.getBeneficiaryId());
                                            if (Objects.equals(beneficiaryResponse.getSubCode(), "200")) {
                                                for (SettlementDetail settlementDetail : settlements.get()) {
                                                    try {
                                                        CashfreeBalanceResponse balanceResponse = cashFreePayouts.getPayoutsBalance(tokenResponse.getData().getToken());
                                                        if (Objects.equals(balanceResponse.getSubCode(), "200")) {
                                                            BigDecimal availableBalance = BigDecimal.valueOf(Double.parseDouble(balanceResponse.getData().getAvailableBalance()));
                                                            BigDecimal amountToBeCollected = Constants.percentage(settlementDetail.getSettlementAmount(), BigDecimal.valueOf(loan.getPgShare()));
                                                            BigDecimal monthlyAmount = amountToBeCollected.add(collectionSummary.get().getMonthlyCollectionAmountRec());

                                                            if (validateAccountLimits(loan, collectionSummary, loanAccountMapping, settlementDetail, amountToBeCollected, monthlyAmount))
                                                                break;

                                                            if (settlementDetail.getStatus().equals(Transaction.INCOMPLETE.toString())) {
                                                                Optional<BigDecimal> totalCollection = collectionRepository.findTotalCollectionBySettlementId(loan.getLoanId(), settlementDetail.getSettlementId(), Collection.COLLECTED.toString());
                                                                if (totalCollection.isPresent()) {
                                                                    amountToBeCollected = amountToBeCollected.subtract(totalCollection.get());
                                                                }
                                                            }

                                                            if (availableBalance.compareTo(amountToBeCollected) > 0) {
                                                                //Collect the amount to be collected from balance
                                                                CollectionDetail collectionDetail = collectionService.saveCollection(loan, collectionSequence, settlementDetail, amountToBeCollected);
                                                                collectionSequence++;

                                                                if (amountToBeCollected.compareTo(BigDecimal.ONE) < 0) {
                                                                    log.error("Cannot collect amount less than 1.0 rupee loanId: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlement Id: " + settlementDetail.getSettlementId());
                                                                    collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                } else {
                                                                    String updatedSettlementStatus = Transaction.PROCESSED.toString();
                                                                    transferMoneyToLender(loan, loanAccountMapping, tokenResponse, settlementDetail, amountToBeCollected, collectionDetail, updatedSettlementStatus, cashFreePayouts);
                                                                }
                                                            } else {
                                                                //if amount is more than available balance, collect all the money from balance and mark as incomplete for later collection.
                                                                log.info("Not enough balance for: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlementId: " + settlementDetail.getSettlementId() + " Collecting total balance for this case.");
                                                                amountToBeCollected = availableBalance;
                                                                CollectionDetail collectionDetail = collectionService.saveCollection(loan, collectionSequence, settlementDetail, amountToBeCollected);
                                                                collectionSequence++;
                                                                if (amountToBeCollected.compareTo(BigDecimal.ONE) < 0) {
                                                                    log.error("Cannot collect amount less than 1.0 rupee loanId: {}", loan.getLoanId() + " account: " + loanAccountMapping.getLoanAccountMapId() + " settlement Id: " + settlementDetail.getSettlementId());
                                                                    collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
                                                                } else {
                                                                    String updatedSettlementStatus = Transaction.INCOMPLETE.toString();
                                                                    transferMoneyToLender(loan, loanAccountMapping, tokenResponse, settlementDetail, amountToBeCollected, collectionDetail, updatedSettlementStatus, cashFreePayouts);
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

    private boolean validateAccountLimits(LoanDetail loan, Optional<CollectionSummary> collectionSummary, LoanAccountMapping loanAccountMapping, SettlementDetail settlementDetail, BigDecimal amountToBeCollected, BigDecimal monthlyAmount) {
        if ((amountToBeCollected.add(collectionSummary.get().getTotalCollectionAmountRec())).compareTo(collectionSummary.get().getLoanAmount()) > 0) {
            log.info("Total collection amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
            return true;
        }

        if (monthlyAmount.compareTo(collectionSummary.get().getMonthlyLimitAmount()) > 0) {
            log.info("Total Monthly amount exceeding! Aborting collection for settlement: {}", settlementDetail.getSettlementId() + ", loanId: " + loan.getLoanId() + ", loanAccount: " + loanAccountMapping.getAccountId());
            return true;
        }

        return false;
    }

    private void transferMoneyToLender(LoanDetail loan, LoanAccountMapping loanAccountMapping, CashfreeAuthorizeResponse tokenResponse, SettlementDetail settlementDetail, BigDecimal amountToBeCollected, CollectionDetail collectionDetail, String updatedSettlementStatus, CashFreePayouts payouts
    ) throws IOException {
        CashfreeTransferRequest transferRequest = new CashfreeTransferRequest();
        transferRequest.setAmount(amountToBeCollected.toString());
        transferRequest.setBeneId(loanAccountMapping.getBeneficiaryId());
        transferRequest.setTransferId(loanAccountMapping.getBeneficiaryId() + "_" + System.currentTimeMillis() / 1000);
        CashfreeTransferResponse transferResponse = payouts.transferMoneyAsync(tokenResponse.getData().getToken(), transferRequest);
        if (!transferResponse.getSubCode().isEmpty() && Long.parseLong(transferResponse.getSubCode()) < 300) {
            collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.COLLECTED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
            collectionRepository.updateCashfreeTransferIdByCollectionId(transferRequest.getTransferId(), loan.getLoanId(), settlementDetail.getSettlementId());
            settlementRepository.updateSettlementStatusById(updatedSettlementStatus, settlementDetail.getId());
            log.info("Collection successful for loan :{}", loan.getSanctionedAmount() + " account :" + loanAccountMapping.getAccountId() + " SettlementId :" + collectionDetail.getSettlementId());
        } else {
            collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), collectionDetail.getCollectionDetailPK().getLoanId(), collectionDetail.getSettlementId());
            log.error("Error in collecting for loan :{}", loan.getLoanId() + " account :" + loanAccountMapping.getAccountId());
        }
    }

    @Override
    public void checkPaymentTransferStatus() {
        log.info("Initiating payment transfers status enquiry at: {}");
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
                                CashFreePayouts payouts = new CashFreePayouts(clientId, clientSecret);
                                CashfreeAuthorizeResponse tokenResponse = payouts.authorizePayouts();
                                if (Objects.equals(tokenResponse.getSubCode(), "200")) {
                                    Optional<List<SettlementDetail>> settlements = settlementRepository.findSettlementsByStatus(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId(), status);
                                    if (settlements.isPresent() && !settlements.get().isEmpty()) {
                                        for (SettlementDetail settlementDetail : settlements.get()) {
                                            Optional<List<CollectionDetail>> collections = collectionRepository.findCollectionBySettlementId(loan.getLoanId(), settlementDetail.getSettlementId(), Collection.COLLECTED.toString());
                                            if (collections.isPresent() && !collections.get().isEmpty()) {
                                                for (CollectionDetail collectionDetail : collections.get()) {
                                                    TransferStatusResponse transferStatusResponse = payouts.getTransferStatus(tokenResponse.getData().getToken(), collectionDetail.getTransferId());
                                                    if (!transferStatusResponse.getStatus().isEmpty()) {
                                                        switch (transferStatusResponse.getStatus()) {
                                                            case "SUCCESS":
                                                                collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.SETTLED.toString(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                collectionRepository.updateCashFreeUtrByCollectionId(transferStatusResponse.getData().getTransfer().getUtr(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                Optional<BigDecimal> totalCollection = collectionRepository.findTotalCollectionBySettlementId(loan.getLoanId(), settlementDetail.getSettlementId(), Collection.SETTLED.toString());
                                                                if (totalCollection.isPresent()) {
                                                                    if (totalCollection.get().compareTo(settlementDetail.getSettlementAmount()) >= 0) {
                                                                        settlementRepository.updateSettlementStatusById(Transaction.SETTLED.toString(), settlementDetail.getId());
                                                                    }
                                                                }
                                                                break;
                                                            case "PENDING":
                                                                break;
                                                            case "FAILED":
                                                                settlementRepository.updateSettlementStatusById(Transaction.FAILED.toString(), settlementDetail.getId());
                                                                collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                break;
                                                            case "REVERSED":
                                                                settlementRepository.updateSettlementStatusById(Transaction.CAPTURED.toString(), settlementDetail.getId());
                                                                collectionRepository.updateCashfreeCollectionStatusByCollectionId(Collection.FAILED.toString(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                collectionRepository.updateCashFreeUtrByCollectionId(transferStatusResponse.getData().getTransfer().getUtr(), loan.getLoanId(), settlementDetail.getSettlementId());
                                                                break;
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
            }
        } catch (Exception exception) {

        }
    }

    private void fetchAndSaveCashfreeSettlements(String startDate, LoanDetail loan, LoanAccountMapping loanAccountMapping, Optional<SettlementDetail> lastSettlement, Optional<ClientInfoDetail> clientInfoDetail) throws Exception {
        //Fetch client info
        if (clientInfoDetail.isPresent()) {
            log.info("Client Info fetch successful. loan: {}", loan.getLoanId() + ", loan account: " + loanAccountMapping.getLoanAccountMapId());

            //If last settlement is present, start fetching settlements after last settlement date to current
            String endDate = DateTimeUtil.localDateTimeToString(LocalDateTime.now());
            String clientId = Crypto.decrypt(clientInfoDetail.get().getInfo1(), secretKey, clientInfoDetail.get().getSalt());
            String clientSecret = Crypto.decrypt(clientInfoDetail.get().getInfo2(), secretKey, clientInfoDetail.get().getSalt());
            CashFreePG cashFreePG = new CashFreePG(clientId, clientSecret);
            com.sts.merchant.core.response.Response<List<Settlement>> settlements = cashFreePG.fetchCashFreeSettlements(startDate, endDate);
            if (settlements.getCode() == HttpStatus.OK.value()) {
                log.info("CashFree Settlements fetch successful for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                if (settlements.getData().isEmpty()) {
                    log.info("Settlements are empty for loan: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId() + " " + LocalDateTime.now());
                } else {
                    for (Settlement settlement : settlements.getData()) {
                        log.info("Saving settlement: {}", settlement.getId() + " for loanID: " + loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
                        saveSettlements(loan, loanAccountMapping, settlement);
                    }
                }
            } else {
                log.info("CashFree Settlements fetch failed for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId() + " reason: " + settlements.getMessage());
            }
        } else {
            log.info("Client info not found! for loanId: {}", loan.getLoanId() + " accountId: " + loanAccountMapping.getLoanAccountMapId());
        }
    }

    private void saveSettlements(LoanDetail loan, LoanAccountMapping loanAccountMapping, Settlement settlement) {
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
