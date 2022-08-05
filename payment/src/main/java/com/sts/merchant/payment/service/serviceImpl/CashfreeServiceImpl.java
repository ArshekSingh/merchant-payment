package com.sts.merchant.payment.service.serviceImpl;

import com.cashfree.lib.constants.Constants;
import com.cashfree.lib.pg.clients.Pg;
import com.cashfree.lib.pg.clients.Settlements;
import com.cashfree.lib.pg.clients.Transactions;
import com.cashfree.lib.pg.domains.request.ListSettlementsRequest;
import com.cashfree.lib.pg.domains.request.ListTransactionsRequest;
import com.cashfree.lib.pg.domains.response.ListSettlementsResponse;
import com.cashfree.lib.pg.domains.response.ListTransactionsResponse;
import com.cashfree.lib.pg.domains.response.Settlement;
import com.cashfree.lib.pg.domains.response.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.entity.TransactionDetail;
import com.sts.merchant.core.enums.Loan;
import com.sts.merchant.core.repository.*;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.service.CashfreeService;
import com.sts.merchant.payment.service.CollectionService;
import com.sts.merchant.payment.service.PaymentTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class CashfreeServiceImpl implements CashfreeService {

    private final CollectionSummaryRepository collectionSummaryRepository;
    private final CollectionRepository collectionRepository;
    private final LoanDetailRepository loanDetailRepository;
    private final TransactionRepository transactionRepository;
    private final LoanAccountRepository loanAccountRepository;

    @Value("${app.encryption.secret}")
    String secretKey;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    public CashfreeServiceImpl(CollectionSummaryRepository collectionSummaryRepository, CollectionRepository collectionRepository, LoanDetailRepository loanDetailRepository, TransactionRepository transactionRepository, LoanAccountRepository loanAccountRepository) {
        this.collectionSummaryRepository = collectionSummaryRepository;
        this.collectionRepository = collectionRepository;
        this.loanDetailRepository = loanDetailRepository;
        this.transactionRepository = transactionRepository;
        this.loanAccountRepository = loanAccountRepository;
    }

    @Override
    @Transactional
    public void fetchPaymentsAndRecord() {
        try {
            //fetch active loans tagged to this account
            Optional<List<LoanDetail>> loans = loanDetailRepository.findActiveLoans(Loan.ACTIVE.toString());
            if (loans.isPresent() && !loans.get().isEmpty()) {
                for (LoanDetail loan : loans.get()) {
                    //fetch active loan accounts for this loan
                    Optional<List<LoanAccountMapping>> loanAccountMappings = loanAccountRepository.findAllActiveLoanAccounts(Loan.ACTIVE.toString(), loan.getLoanId());
                    if (loanAccountMappings.isPresent() && !loanAccountMappings.get().isEmpty()) {
                        for (LoanAccountMapping loanAccountMapping : loanAccountMappings.get()) {
                            //Fetch Last transaction for this account and vendor
                            Optional<TransactionDetail> transactionDetail = transactionRepository.findLastTransactionByLoanAndAccount(loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                            //Fetch Cashfree settlements
                            log.info("Initiating payment fetch at :{}", new Timestamp(System.currentTimeMillis()) + " for loan :" + loan.getLoanId() + ", accountId : " + loanAccountMapping.getAccountId());


                            Pg pg = Pg.getInstance(Constants.Environment.PRODUCTION, "1916997e085a9d3cc0e00ad2a6996191", "7fae0d71487d60c347207dd8c1ee025bc9cea48c");

                            if (transactionDetail.isPresent()) {
                                try {
                                    Settlements settlements = new Settlements(pg);
                                    ListSettlementsRequest settlementsRequest = new ListSettlementsRequest();
                                    settlementsRequest.setStartDate(transactionDetail.get().getTransactionDate());
                                    settlementsRequest.setEndDate(LocalDateTime.of(2022, 8, 3, 12, 0));
                                    ListSettlementsResponse response = settlements.fetchAllSettlements(settlementsRequest);
                                    List<Settlement> settlementList = response.getSettlements();
                                    if (!settlementList.isEmpty()) {
                                        mapPaymentsAndRecordTransactions(loans.get(), settlementList, loanAccountMappings.get());
                                    } else {
                                        log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
                                    }
                                    log.info("total payments fetched :{}", settlementList.size());
                                    fetchTransactionsAndRoute(loans.get(), loanAccountMappings.get());
                                } catch (Exception e) {
                                    log.error("Error in razorpay fetch payments api for accountId: {}", loanAccountMapping.getAccountId(), e);
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    Settlements settlements = new Settlements(pg);
                                    ListSettlementsRequest settlementsRequest = new ListSettlementsRequest();
                                    settlementsRequest.setStartDate(loans.get().get(0).getPaymentDate());
                                    settlementsRequest.setEndDate(LocalDateTime.now());
                                    ListSettlementsResponse response = settlements.fetchAllSettlements(settlementsRequest);
                                    List<Settlement> settlementList = response.getSettlements();
                                    if (settlementList.isEmpty()) {
                                        log.info("No payments to process for loanId :{}", loan.getLoanId() + "account: " + loanAccountMapping.getAccountId());
                                    } else {
                                        mapPaymentsAndRecordTransactions(loans.get(), settlementList, loanAccountMappings.get());
                                    }
                                    fetchTransactionsAndRoute(loans.get(), loanAccountMappings.get());
                                } catch (Exception e) {
                                    log.error("Error in cashFree fetch payments api for accountId: {}", loanAccountMapping.getAccountId(), e);
                                    e.printStackTrace();
                                }
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

    @Override
    public void fetchTransactionsAndRoute(List<LoanDetail> loans, List<LoanAccountMapping> loanAccountMappings) {

    }

    @Override
    public Response transferPayment(Transaction transaction, String transactionId, LoanAccountMapping loanAccountMapping) {
        return null;
    }

    private void mapPaymentsAndRecordTransactions(List<LoanDetail> loans, List<Settlement> settlementList, List<LoanAccountMapping> loanAccountMappings) {
        settlementList.forEach(item -> {
            loans.forEach(loan -> {
                loanAccountMappings.forEach(loanAccountMapping -> {
                    if (Objects.equals(item.getSettledOn(), "CAPTURED")) {
                        try {
                            Optional<TransactionDetail> transactionDetail = transactionRepository.findTransactionById(item.getId());
                            if (transactionDetail.isEmpty()) {
                                log.info("Initiating saving transactions for loanId: {}", loan.getLoanId() + " accountId " + loanAccountMapping.getAccountId());
                                paymentTransactionService.saveCashFreePaymentAsTransaction(item, loanAccountMapping.getAccountId(), loan.getLoanId(), loanAccountMapping.getLoanAccountMapId());
                            }
                        } catch (JsonProcessingException e) {
                            log.error("error inserting transaction for loan :{}", loan.getLoanId() + " account: " + loanAccountMapping.getAccountId(), e);
                        } catch (Exception exception) {
                            log.error("error inserting transaction for loan :{}", loan.getLoanId() + " account: " + loanAccountMapping.getAccountId(), exception);
                        }
                        log.info("total payments fetched :{}", settlementList.size());
                    }
                });
            });
        });
    }

}
