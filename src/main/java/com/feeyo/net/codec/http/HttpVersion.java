package com.feeyo.net.codec.http;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public enum HttpVersion {

    HTTP_0_9("HTTP/0.9"),
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1"),
    HTTP_2("HTTP/2.0");

    private final String _string;

    HttpVersion(String s) {
        _string = s;
    }

    /**
     * @return An HttpMethod if a match or null if no easy match.
     */
    public static HttpVersion find(StringBuilder stringBuilder) {
        int length = stringBuilder.length();
        if (length < 8)
            return null;

        char char0 = stringBuilder.charAt(0);
        char char1 = stringBuilder.charAt(1);
        char char2 = stringBuilder.charAt(2);
        char char3 = stringBuilder.charAt(3);
        char char4 = stringBuilder.charAt(4);
        char char5 = stringBuilder.charAt(5);
        char char6 = stringBuilder.charAt(6);
        char char7 = stringBuilder.charAt(7);

        if ((char0 == 'H' || char0 == 'h')
                && (char1 == 'T' || char1 == 't')
                && (char2 == 'T' || char2 == 't')
                && (char3 == 'P' || char3 == 'p')
                && char4 == '/' && char6 == '.') {

            switch (char5) {
                case '1':
                    switch (char7) {
                        case '0':
                            return HTTP_1_0;
                        case '1':
                            return HTTP_1_1;
                    }
                    break;
                case '2':
                    switch (char7) {
                        case '0':
                            return HTTP_2;
                    }
                    break;
            }
        }

        return null;
    }

    public String toString() {
        return _string;
    }

    /**
     * Case insensitive fromString() conversion
     *
     * @param version the String to convert to enum constant
     * @return the enum constant or null if version unknown
     */
    public static HttpVersion fromString(String version) {
        return CACHE.get(version);
    }

    public static final Map<String, HttpVersion> CACHE;

    static {
        ImmutableMap.Builder<String, HttpVersion> builder = ImmutableMap.builder();
        for (HttpVersion httpVersion : HttpVersion.values()) {
            builder.put(httpVersion._string, httpVersion);
        }
        CACHE = builder.build();
    }
}