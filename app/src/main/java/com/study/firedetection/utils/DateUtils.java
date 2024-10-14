package com.study.firedetection.utils;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT_1 = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATE_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd");

    public static Date getDate(Date date) {
        Date datePart = parse(format(date));
        return datePart != null ? datePart : date;
    }

    public static String format(Date date) {
        return DATE_FORMAT_1.format(date);
    }

    public static Date parse(String date) {
        try {
            return DATE_FORMAT_1.parse(date);
        } catch (ParseException ignored) {
        }
        return null;
    }

    public static String format2(Date date) {
        return DATE_FORMAT_2.format(date);
    }
}
