package com.sts.merchant.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sts.merchant.core.entity.ClientInfoDetail;
import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.enums.AccountType;
import com.sts.merchant.core.enums.Deal;
import com.sts.merchant.core.enums.InfoType;
import com.sts.merchant.core.enums.Loan;
import com.sts.merchant.core.repository.ClientInfoRepository;
import com.sts.merchant.core.repository.LoanAccountRepository;
import com.sts.merchant.core.repository.LoanDetailRepository;
import com.sts.merchant.payment.service.CollectionMailService;
import com.sts.merchant.payment.utils.Crypto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@SpringBootApplication
@ComponentScan("com.sts.merchant")
@EntityScan("com.sts.merchant")
@EnableJpaRepositories("com.sts.merchant")
@EnableScheduling
@PropertySource("classpath:application-razorpay-${spring.profiles.active}.properties")
@Slf4j
public class MerchantServiceApplication {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(MerchantServiceApplication.class, args);
    }


//    private String getSaltString() {
//        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
//        StringBuilder salt = new StringBuilder();
//        Random rnd = new Random();
//        while (salt.length() < 18) { // length of the random string.
//            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
//            salt.append(SALTCHARS.charAt(index));
//        }
//        String saltStr = salt.toString();
//        return saltStr;
//
//    }
//
//    @Autowired
//    ClientInfoRepository clientInfoRepository;
//
//    @Autowired
//    LoanDetailRepository loanDetailRepository;
//
//    @Autowired
//    LoanAccountRepository loanAccountRepository;
//
//    @Value("${app.encryption.secret}")
//    String secretKey;
//
//    @PostConstruct
//    void test() throws Exception {
//        //actualTransferredAmount
//        LoanDetail loanDetail = new LoanDetail();
//        loanDetail.setLoanId("PC_0000041_01");
//        loanDetail.setPartnerLoanId("RPL_009_01");
//        loanDetail.setSanctionedAmount(BigDecimal.valueOf(500000));
//        loanDetail.setDisbursedAmount(BigDecimal.valueOf(500000));
//        loanDetail.setLoanStatus(Loan.INACTIVE.toString());
//        loanDetail.setDisbursementDate(LocalDateTime.of(2022, 10, 19, 0, 0));
//        loanDetail.setLoanStartDate(LocalDateTime.of(2022, 10, 20, 0, 0));
//        loanDetail.setWeekStartDate(LocalDateTime.of(2022, 10, 20, 0, 0));
//        loanDetail.setYearlyStartDate(LocalDateTime.of(2022, 10, 20, 0, 0));
//        loanDetail.setMonthStartDate(LocalDateTime.of(2022, 10, 20, 0, 0));
//        loanDetail.setMonthlyMinAmount(BigDecimal.valueOf(250000));
//        loanDetail.setLenderName("Ramsons Projects Limited");
//        loanDetail.setDealType(Deal.REVENUE_SHARE.toString());
//        loanDetail.setContactEmail("rishabh@toffeecoffeeroasters.com");
//        loanDetail.setContactNumber("8552068076");
//        loanDetail.setContactName("Rishabh Nigam");
//        loanDetail.setPgShare(35);
//        loanDetail.setTotalShare(28);
//        loanDetail.setFixedFees(BigDecimal.valueOf(4));
//        loanDetail.setPgName(AccountType.RAZORPAY.toString());
//        loanDetail.setCreatedOn(LocalDateTime.now());
//        loanDetail.setCreatedBy("PARALLEL_CAP");
//        loanDetail = loanDetailRepository.save(loanDetail);
//
//        LoanAccountMapping loanAccountMapping = new LoanAccountMapping();
//        loanAccountMapping.setLoanId(loanDetail.getLoanId());
//        loanAccountMapping.setStatus(Loan.INACTIVE.toString());
//        loanAccountMapping.setFunderAccountId("acc_KKyCaEc5Gfn0u5");
//        loanAccountMapping.setAccountType(AccountType.RAZORPAY.toString());
//        loanAccountMapping.setAccountId("JkYgXkOWXVEmTF");
//        loanAccountMapping.setCreatedOn(LocalDateTime.now());
//        loanAccountMapping.setCreatedBy("PARALLEL_CAP");
//        loanAccountMapping = loanAccountRepository.save(loanAccountMapping);
//
//        ClientInfoDetail pgClientInfoDetail = new ClientInfoDetail();
//        pgClientInfoDetail.setSalt(getSaltString());
//        pgClientInfoDetail.setLoanAccountMapId(loanAccountMapping.getLoanAccountMapId());
//        pgClientInfoDetail.setAccountType(AccountType.RAZORPAY.toString());
//        pgClientInfoDetail.setInfoType(InfoType.PG.toString());
//        pgClientInfoDetail.setCreatedOn(LocalDateTime.now());
//        pgClientInfoDetail.setCreatedBy("PARALLEL_CAP");
//        pgClientInfoDetail = clientInfoRepository.save(pgClientInfoDetail);
//        pgClientInfoDetail.setInfo1(Crypto.encrypt("", secretKey, pgClientInfoDetail.getSalt()));
//        pgClientInfoDetail.setInfo2(Crypto.encrypt("", secretKey, pgClientInfoDetail.getSalt()));
//        pgClientInfoDetail = clientInfoRepository.save(pgClientInfoDetail);
//
//        System.out.println(pgClientInfoDetail.getInfo1() + "  ,,  " + pgClientInfoDetail.getInfo2());
//
//        System.out.println(Crypto.decrypt(pgClientInfoDetail.getInfo1(), secretKey, pgClientInfoDetail.getSalt()));
//        System.out.println(Crypto.decrypt(pgClientInfoDetail.getInfo2(), secretKey, pgClientInfoDetail.getSalt()));
//
//    }
}
