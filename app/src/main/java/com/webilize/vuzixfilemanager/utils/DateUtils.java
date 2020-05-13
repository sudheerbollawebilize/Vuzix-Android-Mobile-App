package com.webilize.vuzixfilemanager.utils;

import android.text.format.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
    private static final String TAG = "DateUtils";
    public static final String DATE_FORMAT_0 = "MMM dd, yyyy";
    public static final String DATE_FORMAT_1 = "hh:mm a";
    public static final String DATE_FORMAT_2 = "h:mm a";
    public static final String DATE_FORMAT_3 = "yyyy-MM-dd";
    public static final String DATE_FORMAT_4 = "dd-MMMM-yyyy";
    public static final String DATE_FORMAT_5 = "dd MMMM yyyy";
    public static final String DATE_FORMAT_6 = "dd MMMM yyyy zzzz";
    public static final String DATE_FORMAT_7 = "EEE, MMM d, ''yy";
    public static final String DATE_FORMAT_8 = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_9 = "h:mm a dd MMMM yyyy";
    public static final String DATE_FORMAT_10 = "K:mm a, z";
    public static final String DATE_FORMAT_11 = "hh 'o''clock' a, zzzz";
    public static final String DATE_FORMAT_12 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String DATE_FORMAT_13 = "E, dd MMM yyyy HH:mm:ss z";
    public static final String DATE_FORMAT_14 = "yyyy.MM.dd G 'at' HH:mm:ss z";
    public static final String DATE_FORMAT_15 = "yyyyy.MMMMM.dd GGG hh:mm aaa";
    public static final String DATE_FORMAT_16 = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final String DATE_FORMAT_17 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATE_FORMAT_18 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_7);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date today = Calendar.getInstance().getTime();
        return dateFormat.format(today);
    }

    public static String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_1);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date today = Calendar.getInstance().getTime();
        return dateFormat.format(today);
    }

    /**
     * @param time        in milliseconds (Timestamp)
     * @param mDateFormat SimpleDateFormat
     */
    public static String getDateTimeFromTimeStamp(Long time, String mDateFormat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(mDateFormat);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dateTime = new Date(time);
        return dateFormat.format(dateTime);
    }

    /**
     * Get Timestamp from date and time
     *
     * @param mDateTime   datetime String
     * @param mDateFormat Date Format
     * @throws ParseException
     */
    public static long getTimeStampFromDateTime(String mDateTime, String mDateFormat)
            throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(mDateFormat);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = dateFormat.parse(mDateTime);
        return date.getTime();
    }

    /**
     * Return  datetime String from date object
     *
     * @param mDateFormat format of date
     * @param date        date object that you want to parse
     */
    public static String formatDateTimeFromDate(String mDateFormat, Date date) {
        if (date == null) {
            return null;
        }
        return DateFormat.format(mDateFormat, date).toString();
    }

    /**
     * Convert one date format string  to another date format string in android
     *
     * @param inputDateFormat  Input SimpleDateFormat
     * @param outputDateFormat Output SimpleDateFormat
     * @param inputDate        input Date String
     * @throws ParseException
     */
    public static String formatDateFromDateString(String inputDateFormat, String outputDateFormat,
                                                  String inputDate) throws ParseException {
        Date mParsedDate;
        String mOutputDateString;
        SimpleDateFormat mInputDateFormat =
                new SimpleDateFormat(inputDateFormat, java.util.Locale.getDefault());
        SimpleDateFormat mOutputDateFormat =
                new SimpleDateFormat(outputDateFormat, java.util.Locale.getDefault());
        mParsedDate = mInputDateFormat.parse(inputDate);
        mOutputDateString = mOutputDateFormat.format(mParsedDate);
        return mOutputDateString;
    }
}