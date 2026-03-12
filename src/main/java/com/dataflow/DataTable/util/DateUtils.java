package com.dataflow.DataTable.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final ZoneId ZONE_ID_SYS_DEFAULT = ZoneId.systemDefault();
    public static final ZoneId ZONE_ID_IST = ZoneId.of("Asia/Kolkata");
    public static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");
    public static final String DATE_FORMAT = "dd-MM-yyyy";
    private static ZoneId primaryZoneId = ZONE_ID_UTC;
    private static String primaryPattern = "dd-MM-yyyy HH:mm:ss";

    public static void setPrimaryZoneId(ZoneId zoneId) {
        if (zoneId != null) {
            primaryZoneId = zoneId;
        }
    }

    public static void setPrimaryPattern(String pattern) {
        if (pattern != null && !pattern.isEmpty()) {
            primaryPattern = pattern;
        }
    }

    public static LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(primaryZoneId);
    }

    public static LocalDateTime getLocalDateTimeInUTC() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static LocalDateTime getLocalDateTimeInCurrentZone(LocalDateTime localDateTime) {
        if (localDateTime == null)
            return null;
        ZonedDateTime utcDateTime = localDateTime.atZone(ZONE_ID_UTC).withZoneSameInstant(primaryZoneId);
        return utcDateTime.toLocalDateTime();
    }

    public static LocalDateTime getLocalDateTimeByEpoch(Long unixTimestamp) {
        if (unixTimestamp == null)
            return null;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), primaryZoneId);
    }

    public static Long getUnixTimestampInUTC() {
        return Instant.now().getEpochSecond();
    }

    public static Long getUnixTimestampInUTC(LocalDateTime localDateTime) {
        if (localDateTime == null)
            return null;
        return localDateTime.toEpochSecond(ZoneOffset.UTC);
    }

    public static String getFormattedDate(Long epochSecond) {
        if (epochSecond == null)
            return null;
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), primaryZoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(primaryPattern, java.util.Locale.ENGLISH);
        return dateTime.format(formatter).toUpperCase();
    }

    public static ZonedDateTime getUnixTimestampStartDayInZonedDateTime() {
        return ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC);
    }

    public static Long getUnixTimestampStartDayInUTC() {
        return getUnixTimestampStartDayInZonedDateTime().toEpochSecond();
    }

    public static Long getUnixTimestampMinusDaysInUTC(Long days) {
        return getUnixTimestampStartDayInUTC() - Duration.ofDays(days).getSeconds();
    }
}
