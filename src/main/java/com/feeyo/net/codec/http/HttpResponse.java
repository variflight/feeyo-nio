package com.feeyo.net.codec.http;

import com.feeyo.net.nio.util.ByteUtil;

public class HttpResponse extends HttpMessage {

    private final int statusCode;
    private final String reasonPhrase;

    public HttpResponse(HttpResponseStatus status) {
        super();
        //
        if (status == null) {
            throw new IllegalArgumentException("status can not be null!");
        }
        this.statusCode = status.getCode();
        this.reasonPhrase = status.getReasonPhrase();
    }

    public HttpResponse(int statusCode, String reasonPhrase) {
        if (statusCode < 0) {
            throw new IllegalArgumentException("status code may be not negative");
        }
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public HttpResponse(String httpVersion, int code, String reasonPhrase) {
        super(httpVersion);
        this.statusCode = code;
        this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public String toString() {
        //
        StringBuilder buf = new StringBuilder();
        buf.append(httpVersion).append(" ");
        buf.append(statusCode).append(" ");
        buf.append(reasonPhrase).append("\r\n");

        byte[] content = getContent();
        if (content != null && content.length != 0) buf.append(new String(content));
        return buf.toString();
    }

    // split会使用ArrayList+SubList, 性能不好.
    static HttpResponse parseFromBytes(byte[] bytes) {

        int aStart, aEnd;
        int bStart, bEnd;
        int cStart, cEnd;

        aStart = findNonWhitespace(bytes, 0);
        aEnd = findWhitespace(bytes, aStart);

        bStart = findNonWhitespace(bytes, aEnd);
        bEnd = findWhitespace(bytes, bStart);

        cStart = findNonWhitespace(bytes, bEnd);
        cEnd = findEndOfString(bytes);

        return new HttpResponse(new String(bytes, aStart, aEnd - aStart), (int) ByteUtil.asciiBytesToLong(bytes, bStart, bEnd),
                cStart < cEnd ? new String(bytes, cStart, cEnd - cStart) : "");
    }
}