package com.sts.merchant.payment.service.serviceImpl;

import com.sts.merchant.core.dto.razorpayDto.Root;
import com.sts.merchant.core.response.Response;
import com.sts.merchant.payment.service.RazorpayWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Slf4j
@Service
public class RazorpayWebhookServiceImpl implements RazorpayWebhookService {
//    private  PaymentRepository paymentRepository;
//    private final VendorMasterRepository vendorMasterRepository;
//    private final LoanDetailRepository loanDetailRepository;
//    private final CollectionRepository collectionRepository;
//    private final RazorpayService razorpayService;
//    private final PaymentDetailsMapper paymentDetailsMapper;

//    @Autowired
//    public RazorpayWebhookServiceImpl(PaymentRepository paymentRepository, VendorMasterRepository vendorMasterRepository, LoanDetailRepository loanDetailRepository, CollectionRepository collectionRepository, RazorpayService razorpayService, PaymentDetailsMapper paymentDetailsMapper) {
//        this.paymentRepository = paymentRepository;
//        this.vendorMasterRepository = vendorMasterRepository;
//        this.loanDetailRepository = loanDetailRepository;
//        this.collectionRepository = collectionRepository;
//        this.razorpayService = razorpayService;
//        this.paymentDetailsMapper = paymentDetailsMapper;
//    }

    @Transactional
    @Override
    public Response capturePayment(Root request) {
        Response response = new Response<>();
//        Optional<TransactionDetail> paymentDetail = paymentRepository.fetchPaymentByTransactionId(request.getPayload().getPayment().getEntity().getId());
//
//        if (paymentDetail.isEmpty()) {
//
//            Optional<AccountMaster> merchant = vendorMasterRepository.findByAccountId(request.account_id);
//
//            //Fetch merchant loan details
//            if (merchant.isPresent()) {
//
//                Optional<LoanDetails> loanDetailsOptional = loanDetailRepository.findByMerchantIdAndLoanStatus(merchant.get().getMerchantId(), "A");
//                if (loanDetailsOptional.isPresent()) {
//                    TransactionDetail transactionDetail = savePaymentDetails(request, merchant.get());
//                    BigDecimal amountToBeCollected = BigDecimal.valueOf((long) request.payload.payment.entity.amount * merchant.get().getCommissionPercentage() / 100L);
//                    log.info("Amount to be collected : {}", amountToBeCollected);
//                    response = processPaymentCollection(request, merchant, loanDetailsOptional.get(), transactionDetail, amountToBeCollected);
//                } else {
//                    log.info("No Active loan details found for merchantId : {}", merchant.get().getMerchantId());
//                    response.setCode(HttpStatus.BAD_REQUEST.value());
//                    response.setStatus(HttpStatus.BAD_REQUEST);
//                }
//            } else {
//                log.info("No such merchant with specified account id : {}", request.getAccount_id());
//                response.setCode(HttpStatus.BAD_REQUEST.value());
//                response.setStatus(HttpStatus.BAD_REQUEST);
//            }
//        } else {
//            log.info("This webhook has already been processed for accountId {}", request.getAccount_id());
//            response.setCode(HttpStatus.OK.value());
//            response.setStatus(HttpStatus.OK);
//        }
//        return response;
//    }
//
//    private synchronized Response processPaymentCollection(Root request, Optional<AccountMaster> merchant, LoanDetails loanDetails, TransactionDetail transactionDetail, BigDecimal amountToBeCollected) {
//        Response response = new Response();
//        /**Fetch Last collection of merchant.**/
//        Optional<CollectionDetail> lastCollectionOptional = collectionRepository.findLastCollectionByMerchantId(merchant.get().getMerchantId());
//        //add info logs in both if and else
//        if (lastCollectionOptional.isEmpty()) {
//            /**Save collection draft**/
//            CollectionDetail collectionDetail = new CollectionDetail();
//            collectionDetail.setCollectionDate(LocalDate.now());
//            collectionDetail.setCollectionAmount(amountToBeCollected);
//            collectionDetail.setMerchantId(merchant.get().getMerchantId());
//            collectionDetail.setTransactionDate(transactionDetail.getTransactionDate());
//            collectionDetail.setTransactionId(transactionDetail.getTransactionId());
//            collectionDetail.setDailyCollection(amountToBeCollected);
//            collectionDetail.setCollectionStatus("P");
//            collectionDetail.setWeeklyCollection(amountToBeCollected);
//            collectionDetail.setMonthlyCollection(amountToBeCollected);
//            collectionDetail.setYearlyCollection(amountToBeCollected);
//            collectionDetail.setWeekOfYear(LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()));
//            collectionDetail.setMonthOfYear(LocalDate.now().getMonth().getValue());
//            collectionDetail.setYear(LocalDate.now().getYear());
//            collectionDetail = collectionRepository.save(collectionDetail);
//
//            Response<RazorpayTransferResponse> transferResponse = transferFundsToParallelCap(request, transactionDetail, amountToBeCollected);
//            if (transferResponse.getStatus().is2xxSuccessful()) {
//                //put these statuses in enum
//                collectionRepository.updateCollectionStatusByCollectionId("C", collectionDetail.getCollectionId());
//            } else {
//                collectionRepository.updateCollectionStatusByCollectionId("F", collectionDetail.getCollectionId());
//                log.error(response.getMessage());
//            }
//            //make sure in each case response object is being returned with status code and status
//        } else {
//            BigDecimal dailyCollection;
//            BigDecimal weeklyCollection;
//            BigDecimal monthlyCollection;
//            BigDecimal yearlyCollection;
//
//            /**If the last payment collected in same year
//             add the amount collected with last collected yearlyCollection amount
//             */
//            CollectionDetail lastCollection = lastCollectionOptional.get();
//            if (LocalDate.now().getYear() == lastCollection.getYear()) {
//                yearlyCollection = amountToBeCollected.add(lastCollection.getYearlyCollection());
//                log.info("Yearly collection amount : {}", yearlyCollection);
//                if (yearlyCollection.compareTo(loanDetails.getYearlyLimit()) > 0) {
//                    /* exit from here if the yearly limit exceeds*/
//                    log.info("Collection denied as the amount exceeds yearly limit : {}", loanDetails.getYearlyLimit());
//                    response.setCode(HttpStatus.OK.value());
//                    response.setStatus(HttpStatus.OK);
//                    return response;
//                }
//            } else {
//                /**
//                 * If last payment was collected in last year
//                 * Enter new entry for a new year collection
//                 */
//                yearlyCollection = amountToBeCollected;
//                log.info("Yearly collection amount : {}", yearlyCollection);
//            }
//
//            /** If last payment collected in same month and year
//             *  add the amount collected with last collected monthlyCollection amount
//             */
//            if (LocalDate.now().getMonth().getValue() == lastCollection.getMonthOfYear() &&
//                    LocalDate.now().getYear() == lastCollection.getYear()) {
//                monthlyCollection = amountToBeCollected.add(lastCollection.getMonthlyCollection());
//                log.info("Monthly collection amount : {}", monthlyCollection);
//                if (monthlyCollection.compareTo(loanDetails.getMonthlyLimit()) > 0) {
//                    /** exit from here if the monthly limit exceeds*/
//                    log.info("Collection denied as the amount exceeds monthly limit : {}", loanDetails.getMonthlyLimit());
//                    response.setCode(HttpStatus.OK.value());
//                    response.setStatus(HttpStatus.OK);
//                    return response;
//                }
//            } else {
//                /**
//                 * If last payment was collected in last month
//                 * Enter new entry for a new month collection
//                 */
//                monthlyCollection = amountToBeCollected;
//                log.info("Monthly collection amount : {}", monthlyCollection);
//            }
//
//            /** If last payment collected in same week of year and same year
//             *  add the amount collected with last collected weeklyCollection amount
//             */
//            if (LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
//                    == lastCollection.getWeekOfYear()) {
//                weeklyCollection = amountToBeCollected.add(lastCollection.getWeeklyCollection());
//                log.info("Weekly collection amount : {}", weeklyCollection);
//                if (weeklyCollection.compareTo(loanDetails.getWeeklyLimit()) > 0) {
//                    /** exit from here if the monthly limit exceeds*/
//                    log.info("Collection denied as the amount exceeds weekly limit : {}", loanDetails.getWeeklyLimit());
//                    response.setCode(HttpStatus.OK.value());
//                    response.setStatus(HttpStatus.OK);
//                    return response;
//
//                }
//            } else {
//                /**
//                 * If last payment was collected in last week
//                 * Enter new entry for a new week collection
//                 */
//                weeklyCollection = amountToBeCollected;
//                log.info("Weekly collection amount : {}", weeklyCollection);
//            }
//
//            /** If last payment collected in same day of year and same year
//             *  add the amount collected with last collected dailyCollection amount
//             */
//            if (LocalDate.now().getDayOfYear() == lastCollection.getCollectionDate().getDayOfYear()
//                    && LocalDate.now().getYear() == lastCollection.getYear()) {
//                dailyCollection = amountToBeCollected.add(lastCollection.getDailyCollection());
//                log.info("Daily collection amount : {}", dailyCollection);
//                if (dailyCollection.compareTo(loanDetails.getDailyLimit()) > 0) {
//                    /** exit from here if the daily limit exceeds*/
//                    log.info("Collection denied as the amount exceeds daily limit : {}", loanDetails.getDailyLimit());
//                    response.setCode(HttpStatus.OK.value());
//                    response.setStatus(HttpStatus.OK);
//                    return response;
//
//                }
//            } else {
//                /**
//                 * If last payment was collected in last day
//                 * Enter new entry for a new day collection
//                 */
//                dailyCollection = amountToBeCollected;
//                log.info("Daily collection amount : {}", dailyCollection);
//            }
//
//            CollectionDetail collectionDetails = new CollectionDetail();
//            collectionDetails.setCollectionDate(LocalDate.now());
//            collectionDetails.setCollectionAmount(amountToBeCollected);
//            collectionDetails.setMerchantId(merchant.get().getMerchantId());
//            collectionDetails.setTransactionDate(transactionDetail.getTransactionDate());
//            collectionDetails.setTransactionId(transactionDetail.getTransactionId());
//            collectionDetails.setDailyCollection(dailyCollection);
//            collectionDetails.setCollectionStatus("P");
//            collectionDetails.setWeeklyCollection(weeklyCollection);
//            collectionDetails.setMonthlyCollection(monthlyCollection);
//            collectionDetails.setYearlyCollection(yearlyCollection);
//            collectionDetails.setWeekOfYear(LocalDate.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()));
//            collectionDetails.setMonthOfYear(LocalDate.now().getMonth().getValue());
//            collectionDetails.setYear(LocalDate.now().getYear());
//            collectionDetails = collectionRepository.save(collectionDetails);
//            //enums capture
//            Response<RazorpayTransferResponse> transferResponse = transferFundsToParallelCap(request, transactionDetail, amountToBeCollected);
//            if (transferResponse.getStatus().is2xxSuccessful()) {
//                log.info("Collection saved successfully");
//                collectionRepository.updateCollectionStatusByCollectionId("C", collectionDetails.getCollectionId());
//            } else {
//                log.info("Amount could not be collected! Payment transfer to parallel cap amount failed");
//                collectionRepository.updateCollectionStatusByCollectionId("F", collectionDetails.getCollectionId());
//                log.error(response.getMessage());
//            }
//        }
        response.setCode(HttpStatus.OK.value());
        response.setStatus(HttpStatus.OK);
        return response;
    }

//    private TransactionDetail savePaymentDetails(Root request, AccountMaster merchant) {
//        //Capture payment
////        log.info("Saving payment details against merchantId : {} for invoiceId : {}", merchant.getMerchantId(), request.payload.payment.entity.invoice_id);
//        TransactionDetail transactionDetail = paymentDetailsMapper.dtoToEntity(request, merchant);
//        paymentRepository.save(transactionDetail);
//        return transactionDetail;
//    }

//    private Response<RazorpayTransferResponse> transferFundsToParallelCap(Root request, TransactionDetail
//            transactionDetail, BigDecimal amountToBeCollected) {
//        RazorpayTransferMap map = new RazorpayTransferMap();
//        map.setAccount(constants.getParalleCapAccountId());
//        map.setCurrency(transactionDetail.getCurrency());
//        map.setAmount(amountToBeCollected.intValue());
//
//        return razorpayService.transferPayment(map, request.getPayload().getPayment().getEntity().getId());
//    }

}
