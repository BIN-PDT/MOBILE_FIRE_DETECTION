package com.study.firedetection.utils;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT_1 = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat DATE_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd");
    public static Date CURRENT_DATE;
    public static String CURRENT_DATE_TEXT_1, CURRENT_DATE_TEXT_2;

    static {
        try {
            CURRENT_DATE = DATE_FORMAT_1.parse(DateUtils.format1(new Date()));
            CURRENT_DATE_TEXT_1 = DateUtils.format1(CURRENT_DATE);
            CURRENT_DATE_TEXT_2 = DateUtils.format2(CURRENT_DATE);
        } catch (ParseException ignored) {
        }
    }

    public static String format1(Date date) {
        return DATE_FORMAT_1.format(date);
    }

    public static String format2(Date date) {
        return DATE_FORMAT_2.format(date);
    }
}
