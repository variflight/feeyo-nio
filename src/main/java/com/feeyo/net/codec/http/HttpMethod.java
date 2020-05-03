package com.feeyo.net.codec.http;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public enum HttpMethod {
    GET,
    POST,
    HEAD,
    PUT,
    OPTIONS,
    DELETE,
    TRACE,
    CONNECT,
    MOVE,
    PROXY,
    PRI;

    /**
     * @return An HttpMethod if a match or null if no easy match.
     */
    public static HttpMethod find(StringBuilder stringBuilder) {
        int length = stringBuilder.length();
        if (length < 3)
            return null;

        char char0 = stringBuilder.charAt(0);
        char char1 = stringBuilder.charAt(1);
        char char2 = stringBuilder.charAt(2);
        switch (char0) {
            case 'g':
            case 'G':
                if ((char1 == 'E' || char1 == 'e')
                        && (char2 == 'T' || char2 == 't'))
                    return GET;
                break;
            case 'p':
            case 'P':
                if ((char1 == 'O' || char1 == 'o')
                        && (char2 == 'S' || char2 == 's')
                        && length >= 4
                        && (stringBuilder.charAt(3) == 'T' || stringBuilder.charAt(3) == 't'))
                    return POST;
                if ((char1 == 'R' || char1 == 'r')
                        && (char2 == 'O' || char2 == 'o')
                        && length >= 5
                        && (stringBuilder.charAt(3) == 'X' || stringBuilder.charAt(3) == 'x')
                        && (stringBuilder.charAt(4) == 'Y' || stringBuilder.charAt(4) == 'y'))
                    return PROXY;
                if ((char1 == 'U' || char1 == 'u')
                        && (char2 == 'T' || char2 == 't'))
                    return PUT;
                if ((char1 == 'R' || char1 == 'r')
                        && (char2 == 'I' || char2 == 'i'))
                    return PRI;
                break;
            case 'h':
            case 'H':
                if ((char1 == 'E' || char1 == 'e')
                        && (char2 == 'A' || char2 == 'a')
                        && length >= 4
                        && (stringBuilder.charAt(3) == 'D' || stringBuilder.charAt(3) == 'd'))
                    return HEAD;
                break;
            case 'o':
            case 'O':
                if ((char1 == 'P' || char1 == 'p')
                        && (char2 == 'T' || char2 == 't')
                        && length >= 7
                        && (stringBuilder.charAt(3) == 'I' || stringBuilder.charAt(3) == 'i')
                        && (stringBuilder.charAt(4) == 'O' || stringBuilder.charAt(4) == 'o')
                        && (stringBuilder.charAt(5) == 'N' || stringBuilder.charAt(5) == 'n')
                        && (stringBuilder.charAt(6) == 'S' || stringBuilder.charAt(6) == 's'))
                    return OPTIONS;
                break;
            case 'd':
            case 'D':
                if ((char1 == 'E' || char1 == 'e')
                        && (char2 == 'L' || char2 == 'l')
                        && length >= 6
                        && (stringBuilder.charAt(3) == 'E' || stringBuilder.charAt(3) == 'e')
                        && (stringBuilder.charAt(4) == 'T' || stringBuilder.charAt(4) == 't')
                        && (stringBuilder.charAt(5) == 'E' || stringBuilder.charAt(5) == 'e'))
                    return DELETE;
                break;
            case 't':
            case 'T':
                if ((char1 == 'R' || char1 == 'r')
                        && (char2 == 'A' || char2 == 'a')
                        && length >= 5
                        && (stringBuilder.charAt(3) == 'C' || stringBuilder.charAt(3) == 'c')
                        && (stringBuilder.charAt(4) == 'E' || stringBuilder.charAt(4) == 'e'))
                    return TRACE;
                break;
            case 'c':
            case 'C':
                if ((char1 == 'O' || char1 == 'o')
                        && (char2 == 'N' || char2 == 'n')
                        && length >= 7
                        && (stringBuilder.charAt(3) == 'N' || stringBuilder.charAt(3) == 'n')
                        && (stringBuilder.charAt(4) == 'E' || stringBuilder.charAt(4) == 'e')
                        && (stringBuilder.charAt(5) == 'C' || stringBuilder.charAt(5) == 'c')
                        && (stringBuilder.charAt(6) == 'T' || stringBuilder.charAt(6) == 't'))
                    return CONNECT;
                break;
            case 'm':
            case 'M':
                if ((char1 == 'O' || char1 == 'o')
                        && (char2 == 'V' || char2 == 'v')
                        && length >= 4
                        && (stringBuilder.charAt(3) == 'E' || stringBuilder.charAt(3) == 'e'))
                    return MOVE;
                break;

            default:
                break;
        }
        return null;
    }

    /**
     * Converts the given String parameter to an HttpMethod
     *
     * @param method the String to get the equivalent HttpMethod from
     * @return the HttpMethod or null if the parameter method is unknown
     */
    public static HttpMethod fromString(String method)
    {
        return CACHE.get(method);
    }

    public static final Map<String, HttpMethod> CACHE;

    static {
        ImmutableMap.Builder<String, HttpMethod> builder = ImmutableMap.builder();
        for (HttpMethod method : HttpMethod.values()) {
            builder.put(method.toString(), method);
        }
        CACHE = builder.build();
    }
}