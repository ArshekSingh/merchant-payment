package com.sts.merchant.payment.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class DateTimeUtil {

    public static final String DD_MM_YYYY = "dd/MM/yyyy";
    public static final String DD_MM_YYYY_MINUS = "dd-MM-yyyy";
    public static final String YYYY_MM_DD_MINUS = "yyyy-MM-dd";
    public static final String DD_MM_YYYY_HH_MM_SS = "dd-MM-yyyy HH:mm:ss";

    public static final String DDMMYYYY = "dd-MM-yyyy";

    public static final String DD_MM_YYYYDbFormat = "dd-MM-yyyy";

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public static String dateTimeToString(LocalDateTime localDateTime) {
        return null != localDateTime ? dateTimeFormatter.format(localDateTime) : null;
    }

    public static LocalDateTime stringToDateTime(String localDateTimeString) {
        return StringUtils.hasText(localDateTimeString) ? LocalDateTime.parse(localDateTimeString, dateTimeFormatter) : null;
    }

    public static LocalDateTime localDateTimeToCashfreeDateTime(String localDateTimeString) {
        return StringUtils.hasText(localDateTimeString) ? LocalDateTime.parse(localDateTimeString, dateTimeFormatter) : null;
    }

    public static LocalDateTime stringToDateTime(String localDateTimeString, String format) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
        return StringUtils.hasText(localDateTimeString) ? LocalDateTime.parse(localDateTimeString, dateTimeFormatter) : null;
    }

    public static String localDateToString(LocalDate localDate, String dateFormat) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        return localDate != null ? dateFormatter.format(localDate) : null;
    }

    public static LocalDate stringToLocalDate(String localDateString, String dateFormat) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        return StringUtils.hasText(localDateString) ? LocalDate.parse(localDateString, dateFormatter) : null;
    }

    public static String dateTimeToString(LocalDateTime localDateTime, String format) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
        return localDateTime != null ? dateFormat.format(localDateTime) : null;
    }

    public static Date parseDate(String date, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false);
            return sdf.parse(date);
        } catch (Exception e) {
            log.error("Exception occured while parsing date {} with format {}", date, format);
        }
        return null;
    }

    public static LocalDate parseLocalDate(String date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(date, formatter);

    }

    public static String format(Date date, String format) {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(date);
        }
        return null;
    }

    public static String localDateTimeToString(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateTime.format(formatter);
    }


    public static LocalDateTime stringToLocalDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTime, formatter);
    }

    public static LocalDate getLocalDateFromDate(Date date) {
        if (date != null) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }
}
