package com.sts.merchant.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sts.merchant.core.entity.ClientInfoDetail;
import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.entity.LoanDetail;
import com.sts.merchant.core.enums.AccountType;
import com.sts.merchant.core.enums.Deal;
import com.sts.merchant.core.enums.InfoType;
import com.sts.merchant.core.repository.ClientInfoRepository;
import com.sts.merchant.core.repository.LoanAccountRepository;
import com.sts.merchant.core.repository.LoanDetailRepository;
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


    private String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    @Autowired
    ClientInfoRepository clientInfoRepository;

    @Autowired
    LoanDetailRepository loanDetailRepository;

    @Autowired
    LoanAccountRepository loanAccountRepository;

    @Value("${app.encryption.secret}")
    String secretKey;

    @PostConstruct
    void test() throws Exception {
        LoanDetail loanDetail = new LoanDetail();
        loanDetail.setLoanId(1);
        loanDetail.setPartnerLoanId(1);
        loanDetail.setSanctionedAmount(BigDecimal.valueOf(100000000));
        loanDetail.setDisbursedAmount(BigDecimal.valueOf(100000000));
        loanDetail.setLoanStatus("A");
        loanDetail.setDisbursementDate(LocalDateTime.of(2022, 8, 1, 0, 0));
        loanDetail.setWeekStartDate(LocalDateTime.of(2022, 8, 1, 0, 0));
        loanDetail.setYearlyStartDate(LocalDateTime.of(2022, 8, 1, 0, 0));
        loanDetail.setMonthStartDate(LocalDateTime.of(2022, 8, 1, 0, 0));
        loanDetail.setDailyMaxAmount(BigDecimal.valueOf(10000));
        loanDetail.setDailyMinAmount(BigDecimal.valueOf(10000));
        loanDetail.setMonthlyMaxAmount(BigDecimal.valueOf(10000));
        loanDetail.setMonthlyMinAmount(BigDecimal.valueOf(10000));
        loanDetail.setWeeklyMaxAmount(BigDecimal.valueOf(10000));
        loanDetail.setWeeklyMinAmount(BigDecimal.valueOf(10000));
        loanDetail.setYearlyMaxAmount(BigDecimal.valueOf(10000));
        loanDetail.setYearlyMinAmount(BigDecimal.valueOf(10000));
        loanDetail.setLenderName("PARALLEL_CAP_LENDER");
        loanDetail.setDealType(Deal.REVENUE_SHARE.toString());
        loanDetail.setContactEmail("rohilla17@gmail.com");
        loanDetail.setContactNumber("9878186819");
        loanDetail.setContactName("Shubham");
        loanDetail.setPgShare(20);
        loanDetail.setTotalShare(10);
        loanDetail.setPgName(AccountType.CASHFREE.toString());
        loanDetail.setCreatedOn(LocalDateTime.now());
        loanDetail.setCreatedBy("PARALLEL_CAP_TEST");
        loanDetail = loanDetailRepository.save(loanDetail);

        LoanAccountMapping loanAccountMapping = new LoanAccountMapping();
        loanAccountMapping.setLoanId(loanDetail.getLoanId());
        loanAccountMapping.setStatus("A");
        loanAccountMapping.setBeneficiaryId("shubham1995");
        loanAccountMapping.setAccountType(AccountType.CASHFREE.toString());
        loanAccountMapping.setLoanAccountMapId(1);
        loanAccountMapping.setAccountId("DOC_CASHFREE");
        loanAccountMapping.setCreatedOn(LocalDateTime.now());
        loanAccountMapping.setCreatedBy("PARALLEL_CAP_TEST");
        loanAccountMapping = loanAccountRepository.save(loanAccountMapping);

        ClientInfoDetail pgClientInfoDetail = new ClientInfoDetail();
        pgClientInfoDetail.setSalt(getSaltString());
        pgClientInfoDetail.setLoanAccountMapId(1);
        pgClientInfoDetail.setAccountType(AccountType.CASHFREE.toString());
        pgClientInfoDetail.setInfoType(InfoType.PG.toString());
        pgClientInfoDetail.setCreatedOn(LocalDateTime.now());
        pgClientInfoDetail.setCreatedBy("PARALLEL_CAP_TEST");
        pgClientInfoDetail = clientInfoRepository.save(pgClientInfoDetail);
        pgClientInfoDetail.setInfo1(Crypto.encrypt("1916997e085a9d3cc0e00ad2a6996191", secretKey, pgClientInfoDetail.getSalt()));
        pgClientInfoDetail.setInfo2(Crypto.encrypt("7fae0d71487d60c347207dd8c1ee025bc9cea48c", secretKey, pgClientInfoDetail.getSalt()));
        pgClientInfoDetail = clientInfoRepository.save(pgClientInfoDetail);

        System.out.println(pgClientInfoDetail.getInfo1() + "  ,,  " + pgClientInfoDetail.getInfo2());

        System.out.println(Crypto.decrypt(pgClientInfoDetail.getInfo1(), secretKey, pgClientInfoDetail.getSalt()));
        System.out.println(Crypto.decrypt(pgClientInfoDetail.getInfo2(), secretKey, pgClientInfoDetail.getSalt()));


        ClientInfoDetail payoutClientInfoDetail = new ClientInfoDetail();
        payoutClientInfoDetail.setSalt(getSaltString());
        payoutClientInfoDetail.setLoanAccountMapId(1);
        payoutClientInfoDetail.setAccountType(AccountType.CASHFREE.toString());
        payoutClientInfoDetail.setInfoType(InfoType.PAYOUTS.toString());
        payoutClientInfoDetail.setCreatedOn(LocalDateTime.now());
        payoutClientInfoDetail.setCreatedBy("PARALLEL_CAP_TEST");
        payoutClientInfoDetail = clientInfoRepository.save(payoutClientInfoDetail);
        payoutClientInfoDetail.setInfo1(Crypto.encrypt("CF191699CC45DR8CULKD6QQTF7Q0", secretKey, payoutClientInfoDetail.getSalt()));
        payoutClientInfoDetail.setInfo2(Crypto.encrypt("4abd42fd0db53f53309dbf622b2738bf35e0a241", secretKey, payoutClientInfoDetail.getSalt()));
        payoutClientInfoDetail = clientInfoRepository.save(payoutClientInfoDetail);

        System.out.println(payoutClientInfoDetail.getInfo1() + "  ,,  " + payoutClientInfoDetail.getInfo2());

        System.out.println(Crypto.decrypt(payoutClientInfoDetail.getInfo1(), secretKey, payoutClientInfoDetail.getSalt()));
        System.out.println(Crypto.decrypt(payoutClientInfoDetail.getInfo2(), secretKey, payoutClientInfoDetail.getSalt()));


    }

}
