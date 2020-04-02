package com.wekay.drawguess.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Ouerghi Yassine
 */
public class DateUtils {
    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static String getCurrentTime() {
        return SIMPLE_DATE_FORMAT.format(new Date());
    }
}
