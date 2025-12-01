package com.example.replate.util;

import android.os.Build;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeUtil {

    public static String formatDateForDisplay(String dateStr) {
        if (dateStr == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "—";
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate parcelDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            if (parcelDate.equals(today)) {
                return "Today,";
            } else if (parcelDate.equals(tomorrow)) {
                return "Tomorrow,";
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
                return parcelDate.format(formatter) + ",";
            }
        } catch (Exception e) {
            return "—";
        }
    }

    public static String formatTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            return "—";
        }
        return startTime + " - " + endTime;
    }

    public static String getTodayDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        return sdf.format(calendar.getTime());
    }
}