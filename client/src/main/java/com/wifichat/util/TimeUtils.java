package com.wifichat.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeUtils() {
    }

    public static String formatTime(long epochMillis) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        return TIME_FORMAT.format(dateTime);
    }
}

