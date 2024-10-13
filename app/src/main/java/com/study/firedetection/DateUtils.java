package com.study.firedetection;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    @SuppressLint("SimpleDateFormat")
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    public static Date getDate(Date date) {
        Date datePart = parse(format(date));
        return datePart != null ? datePart : date;
    }

    public static String format(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static Date parse(String date) {
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException ignored) {
        }
        return null;
    }
}
