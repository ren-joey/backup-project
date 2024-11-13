package com.delta.dms.community.utils;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimestampConverter {

    public static String convertMillisToISO8601UTC(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }

    public static LocalDateTime convertToLocalDateTimeWithOffset(Instant instant, int offsetHours) {
        return LocalDateTime.ofInstant(instant, ZoneId.of(DsmpConstants.TIME_ZONE_ID_UTC)).plusHours(offsetHours);
    }
}

