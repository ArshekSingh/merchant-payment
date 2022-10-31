package com.sts.merchant.payment.service.serviceImpl;


import com.sts.merchant.core.assembler.ExcelAssembler;
import com.sts.merchant.core.entity.CollectionDetail;
import com.sts.merchant.core.repository.CollectionRepository;
import com.sts.merchant.core.util.DateTimeUtil;
import com.sts.merchant.payment.service.CollectionMailService;
import com.sts.merchant.payment.utils.ExcelGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sts.merchant.payment.utils.Constants.COLLECTION_DETAIL;


@Service
@Slf4j
public class CollectionMailServiceImpl implements CollectionMailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private ExcelGeneratorUtil excelGeneratorUtil;

    @Autowired
    private ExcelAssembler excelAssembler;

    public void sendMail(byte[] bytes) {

        String to = "shubham.rohilla@sastechstudio.com";
        LocalDateTime date = LocalDateTime.now();
        LocalDateTime collectionStartDate = date.minusDays(25);
        LocalDateTime collectionEndDate = date.minusDays(1);
        LocalDateTime collStartDate = DateTimeUtil.formatLocalDateTime(collectionStartDate);
        LocalDateTime collEndDate = DateTimeUtil.formatLocalDateTime(collectionEndDate);

        LocalDate startDate = collStartDate.toLocalDate();
        LocalDate endDate = collEndDate.toLocalDate();

        String subject = "Total Collections of TCR from " + startDate + " to " + endDate + ")";
        Long totalCollection = collectionRepository.findTotalCollectionBasedOnDate(collStartDate, collEndDate);
        Long totalTransaction = collectionRepository.findTotalTransactionBasedOnDate(collStartDate, collEndDate);
        Long totalSettledAmount = collectionRepository.findTotalSettledAmountBasedOnDate(collStartDate, collEndDate, "s");
        if (totalSettledAmount == null) {
            totalSettledAmount = 0l;
        }
        Long totalSettledTransaction = collectionRepository.findTotalSettledTransactions(collStartDate, collEndDate, "s");
        String body = "Please find the attachment for collections of Toffee Coffee Roaster till" + endDate + "\n" + "Total Collections : Rs " + totalCollection.toString() + "\n" + "Total Transactions: " + totalTransaction + "\n" + "Total Settled Amount : Rs " + totalSettledAmount + "\n" + "Total Settled Transactions: " + totalSettledTransaction;
        sendMailWithAttachment(to, subject, body, "CollectionDetail", bytes);
    }

    @Override
    public void sendMailWithAttachment(String to, String subject, String body, String fileName, byte[] ba) {
        MimeMessagePreparator preparator = mimeMessage -> {

            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress("arshek.singh@parallelcap.in"));
            mimeMessage.setSubject(subject);

            Multipart multiPart = new MimeMultipart();

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);

            if (ba != null && ba.length > 0) {
                DataSource fds = new ByteArrayDataSource(ba, "application/octet-stream");
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.setDataHandler(new DataHandler(fds));
                attachment.setDisposition(Part.ATTACHMENT);
                attachment.setFileName(fileName + ".xls");
                multiPart.addBodyPart(messageBodyPart);
                multiPart.addBodyPart(attachment);

                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
                messageHelper.addAttachment(fileName + ".xls", fds);
            }
            mimeMessage.setContent(multiPart);
        };
        try {
            mailSender.send(preparator);
        } catch (MailException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void generateExcelForCollectionDetail() {
        try {
            LocalDateTime date = LocalDateTime.now();
            LocalDateTime collectionStartDate = date.minusDays(25);
            LocalDateTime collectionEndDate = date.minusDays(1);
            LocalDateTime collStartDate = DateTimeUtil.formatLocalDateTime(collectionStartDate);
            LocalDateTime collEndDate = DateTimeUtil.formatLocalDateTime(collectionEndDate);
            findCollectionDetailByDate(collStartDate, collEndDate);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void findCollectionDetailByDate(LocalDateTime collectionStartDate, LocalDateTime collectionEndDate) {

        try {
            if (collectionStartDate != null) {
                log.info("Fetching collection details of today from the database");
                Optional<List<CollectionDetail>> collections = collectionRepository.findCollectionDetailByCollectionDate(collectionStartDate, collectionEndDate);

                if (collections.isPresent()) {
                    log.info("Initiating process");
                    try {
                        HSSFWorkbook workbook = new HSSFWorkbook();
                        Map<String, Object> map = excelGeneratorUtil.populateHeaderAndName(COLLECTION_DETAIL, "collection_detail.xls");
                        map.put("RESULTS", excelAssembler.prepareCollectionDetailData(collections.get()));
                        excelGeneratorUtil.buildExcelDocument(map, workbook);
                        byte[] bytes = excelGeneratorUtil.downloadDocument(map, workbook);
                        sendMail(bytes);
                    } catch (Exception exception) {
                        log.error("Exception occurs while downloading Excel {}", exception.getMessage());
                    }
                } else {
                    log.info("Collection details are not present");

                }
            } else {
                log.info("Collection date is empty or not appropriate");
            }
        } catch (Exception e) {
            log.error("There is some issue in fetching collection details based on date");
            e.printStackTrace();
        }

    }
}
