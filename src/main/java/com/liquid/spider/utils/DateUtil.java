package com.liquid.spider.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    public static boolean sameDay(Date day1, Date day2){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(day1).equals(df.format(day2));
    }

    public static Date getDate(String dateStr) throws ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.parse(dateStr);
    }
}
