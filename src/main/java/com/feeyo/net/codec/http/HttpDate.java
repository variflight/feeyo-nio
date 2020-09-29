package com.feeyo.net.codec.http;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Best-effort parser for HTTP dates.
 */
public final class HttpDate {
	//
    private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override protected DateFormat initialValue() {
            DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            rfc1123.setTimeZone(TimeZone.getTimeZone("UTC"));
            return rfc1123;
        }
    };

    /**
     * If we fail to parse a date in a non-standard format, try each of these formats in sequence.
     */
    private static final String[] BROWSER_COMPATIBLE_DATE_FORMATS = new String[] {
            /* This list comes from  {@code org.apache.http.impl.cookie.BrowserCompatSpec}. */
            "EEEE, dd-MMM-yy HH:mm:ss zzz", // RFC 1036
            "EEE MMM d HH:mm:ss yyyy", // ANSI C asctime()
            "EEE, dd-MMM-yyyy HH:mm:ss z",
            "EEE, dd-MMM-yyyy HH-mm-ss z",
            "EEE, dd MMM yy HH:mm:ss z",
            "EEE dd-MMM-yyyy HH:mm:ss z",
            "EEE dd MMM yyyy HH:mm:ss z",
            "EEE dd-MMM-yyyy HH-mm-ss z",
            "EEE dd-MMM-yy HH:mm:ss z",
            "EEE dd MMM yy HH:mm:ss z",
            "EEE,dd-MMM-yy HH:mm:ss z",
            "EEE,dd-MMM-yyyy HH:mm:ss z",
            "EEE, dd-MM-yyyy HH:mm:ss z",

            /* RI bug 6641315 claims a cookie of this format was once served by www.yahoo.com */
            "EEE MMM d yyyy HH:mm:ss z",
    };

	//
	public static Date parse(String value) {
		try {
			return STANDARD_DATE_FORMAT.get().parse(value);
		} catch (ParseException ignore) {}
		//
		for (String formatString : BROWSER_COMPATIBLE_DATE_FORMATS) {
			try {
				return new SimpleDateFormat(formatString, Locale.US).parse(value);
			} catch (ParseException ignore) {}
		}
		return null;
	}
	
	//
	public static String format(long date) {
		return format( new Date(date) );
	}
	
	public static String format(Date date) {
		return STANDARD_DATE_FORMAT.get().format(date);
	}
}
