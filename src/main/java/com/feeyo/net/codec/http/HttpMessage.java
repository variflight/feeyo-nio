package com.feeyo.net.codec.http;

import java.util.HashMap;

public abstract class HttpMessage {

    private static final String HTTP_1_1 = "HTTP/1.1";
    private static final String HTTP_1_0 = "HTTP/1.0";

    protected HashMap<String, String> headers;
    protected String httpVersion;
    protected byte[] content = null;

    public HttpMessage() {
        this(HTTP_1_1);
    }

    public HttpMessage(String httpVersion) {
        //
        if (!(HTTP_1_1.equalsIgnoreCase(httpVersion) || HTTP_1_0.equalsIgnoreCase(httpVersion))) 
            throw new IllegalArgumentException("Unsupported http protocol version, excepted http/1.1 or http/1.0 ");
        //
        this.httpVersion = httpVersion;
        this.headers = new HashMap<>();
    }

    public int getContentLength() {
        String value = headers.get(HttpHeaderNames.CONTENT_LENGTH);
        if (value == null) {
            value = headers.get("content-length");
        }
        return value != null ? Integer.parseInt(value) : -1;
    }

    /**
     * @see HttpHeaderNames
     */
    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public HashMap<String, String> headers() {
        return headers;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public boolean containsHeader(String name, String value, boolean ignoreCase) {
        if (name == null || value == null) return false;
        //
        return ignoreCase ? value.equalsIgnoreCase(headers.get(name)) : value.equals(headers.get(name));
    }

    static int findNonWhitespace(byte[] bytes, int offset) {
        for (int result = offset; result < bytes.length; ++result) {
            if (!Character.isWhitespace(bytes[result])) {
                return result;
            }
        }
        return bytes.length;
    }

    static int findWhitespace(byte[] bytes, int offset) {
        for (int result = offset; result < bytes.length; ++result) {
            if (Character.isWhitespace(bytes[result])) {
                return result;
            }
        }
        return bytes.length;
    }

    static int findEndOfString(byte[] bytes) {
        for (int result = bytes.length - 1; result > 0; --result) {
            if (!Character.isWhitespace(bytes[result])) {
                return result + 1;
            }
        }
        return 0;
    }
}