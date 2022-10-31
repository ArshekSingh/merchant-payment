package com.sts.merchant.payment.service.serviceImpl;


import com.sts.merchant.core.assembler.ExcelAssembler;
import com.sts.merchant.core.entity.CollectionDetail;
import com.sts.merchant.core.repository.CollectionRepository;
import com.sts.merchant.payment.service.CollectionMailService;
import com.sts.merchant.payment.utils.ExcelGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.recipient.email}")
    private String recipient;

    @Value("${spring.mail.username}")
    private String sender;

    public void sendMail(byte[] bytes, String loanId) {

        try {
            String to = recipient;
            Long collectionAmount = collectionRepository.findCollectionAmountByLoanId(loanId);
            if (collectionAmount == null) {
                collectionAmount = 0L;
            }
            Long totalTransaction = collectionRepository.findTotalTransactionBasedOnLoanId(loanId);
            Long totalSettledAmount = collectionRepository.findTotalSettledAmountBasedOnLoanId(loanId);
            if (totalSettledAmount == null) {
                totalSettledAmount = 0L;
            }
            Long totalSettledTransaction = collectionRepository.findTotalSettledTransaction(loanId);
            String subject = "Total Collections of TCR for loanId " + loanId;
            String body = "Please find the attachment for collections of Toffee Coffee Roaster for loanId " + loanId + "\n" + "Total Collections : Rs " + collectionAmount + "\n" + "Total Transactions: " + totalTransaction + "\n" + "Total Settled Amount : Rs " + totalSettledAmount + "\n" + "Total Settled Transactions: " + totalSettledTransaction;
            sendMailWithAttachment(to, subject, body, "CollectionDetail", bytes);

        }
        catch (Exception exception){
            log.error("Values of required fields are not correct {}", exception.getMessage());
            exception.printStackTrace();

        }
    }

    @Override
    public void sendMailWithAttachment(String to, String subject, String body, String fileName, byte[] ba) {
        MimeMessagePreparator preparator = mimeMessage -> {

            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress(sender));
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
    public void generateExcelForCollectionDetail(String loanId) {
        try {
            findCollectionDetailByLoanId(loanId);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void findCollectionDetailByLoanId(String loanId) {

        try {
            if (loanId != null) {
                log.info("Fetching collection details for loanId {}", loanId);
                Optional<List<CollectionDetail>> collections = collectionRepository.findCollectionDetailByLoanId(loanId);

                if (collections.isPresent()) {
                    log.info("Initiating process");
                    try {
                        byte[] bytes;
                        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
                            Map<String, Object> map = excelGeneratorUtil.populateHeaderAndName(COLLECTION_DETAIL, "collection_detail.xls");
                            map.put("RESULTS", excelAssembler.prepareCollectionDetailData(collections.get()));
                            excelGeneratorUtil.buildExcelDocument(map, workbook);
                            bytes = excelGeneratorUtil.downloadDocument(workbook);
                        }
                        sendMail(bytes, loanId);
                    } catch (Exception exception) {
                        log.error("Exception occurs while downloading Excel {}", exception.getMessage());
                    }
                } else {
                    log.info("Collection details are not present");

                }
            } else {
                log.info("LoanId is empty or not appropriate");
            }
        } catch (Exception e) {
            log.error("There is some issue in fetching collection details based on loanId");
            e.printStackTrace();
        }

    }
}
