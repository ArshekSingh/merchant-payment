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
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
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

    public void sendMailWithAttachment(String to, String subject, String body, String fileToAttach) {
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                mimeMessage.setFrom(new InternetAddress("admin@gmail.com"));
                mimeMessage.setSubject(subject);
                mimeMessage.setText(body);

                FileSystemResource file = new FileSystemResource(new File(fileToAttach));
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.addAttachment("logo.jpg", file);
            }
        };
        try {
            mailSender.send(preparator);
        } catch (MailException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void generateExcelForCollectionDetail(HttpServletResponse httpServletResponse) {
        try {
            LocalDateTime date = LocalDateTime.now();
            LocalDateTime collectionStartDate = date.minusDays(3);
            LocalDateTime collectionEndDate = date.minusDays(1);
            LocalDateTime collStartDate = DateTimeUtil.formatLocalDateTime(collectionStartDate);
            LocalDateTime collEndDate = DateTimeUtil.formatLocalDateTime(collectionEndDate);
            findCollectionDetailByDate(httpServletResponse, collStartDate, collEndDate);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void findCollectionDetailByDate(HttpServletResponse httpServerResponse, LocalDateTime collectionStartDate, LocalDateTime collectionEndDate) {

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
                        excelGeneratorUtil.downloadDocument(httpServerResponse, map, workbook);
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
