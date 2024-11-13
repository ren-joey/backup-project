package com.delta.dms.community.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtility {
    public final static Long EMPTY_DATE_TIME_VALUE = -1L;

    // 如果time小於0回傳空字串
    public static String convertToDateTime(DateTimeFormatter dateTimeFormatter, long time) {
        if (time < 0) {
            return "";
        }
        ZonedDateTime dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
        return dateTime.format(dateTimeFormatter);
    }
}
