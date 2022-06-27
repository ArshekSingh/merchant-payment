package com.sts.merchant.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sts.merchant.core.entity.LoanAccountMapping;
import com.sts.merchant.core.entity.LoanDetail;
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

//
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
//        LoanDetail loanDetail = new LoanDetail();
//        loanDetail.setLoanId(1);
//        loanDetail.setLoanStatus("A");
//        loanDetail.setCreatedOn(LocalDateTime.now());
//        loanDetail.setCreatedBy("TEST");
//        loanDetail.setCapPercentage(10);
//        loanDetail.setFunderAccountId("acc_JVWLRvD3QVBodY");
//        loanDetail.setDailyLimitAmount(BigDecimal.valueOf(1000000));
//        loanDetail.setMonthlyLimitAmount(BigDecimal.valueOf(100000000));
//        loanDetail.setWeeklyLimitAmount(BigDecimal.valueOf(5000000));
//        loanDetail.setYearlyLimitAmount(BigDecimal.valueOf(500000000));
//        loanDetail.setLoanAmount(BigDecimal.valueOf(10000000000L));
//        loanDetail.setPaymentDate(LocalDateTime.of(2022, 5, 1, 0, 0));
//        loanDetail.setDisbursementDate(LocalDateTime.of(2022, 5, 1, 0, 0));
//        loanDetail.setWeekStartDate(LocalDateTime.of(2022, 5, 1, 0, 0));
//        loanDetail.setYearlyStartDate(LocalDateTime.of(2022, 5, 1, 0, 0));
//        loanDetail.setMonthStartDate(LocalDateTime.of(2022, 5, 1, 0, 0));
//        loanDetail = loanDetailRepository.save(loanDetail);
//
//        LoanAccountMapping loanAccountMapping = new LoanAccountMapping();
//        loanAccountMapping.setLoanId(loanDetail.getLoanId());
//        loanAccountMapping.setStatus("A");
//        loanAccountMapping.setLoanAccountMapId(1);
//        loanAccountMapping.setAccountId("JNvRFQjCGpwb6t");
//        loanAccountMapping.setSalt(getSaltString());
//        loanAccountMapping.setCreatedOn(LocalDateTime.now());
//        loanAccountMapping.setCreatedBy("TEST");
//        loanAccountMapping = loanAccountRepository.save(loanAccountMapping);
//        loanAccountMapping.setInfo1(Crypto.encrypt("rzp_test_kAD1RIVvc0iZ9j", secretKey, loanAccountMapping.getSalt()));
//        loanAccountMapping.setInfo2(Crypto.encrypt("UTyE6QZ5SSM0ggWRNW2x85d1", secretKey, loanAccountMapping.getSalt()));
//        loanAccountMapping = loanAccountRepository.save(loanAccountMapping);
//
//        System.out.println(loanAccountMapping.getInfo1() + "  ,,  " + loanAccountMapping.getInfo2());
//
//        System.out.println(Crypto.decrypt(loanAccountMapping.getInfo1(), secretKey, loanAccountMapping.getSalt()));
//        System.out.println(Crypto.decrypt(loanAccountMapping.getInfo2(), secretKey, loanAccountMapping.getSalt()));
//
//
//    }

}
