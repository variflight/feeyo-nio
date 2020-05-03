package com.feeyo.net.codec.http;

public class HttpRequest extends HttpMessage {

    private final String method;
    private final String uri;

    public HttpRequest(String method, String uri) {
        super();
        if (method == null || uri == null) {
            throw new IllegalArgumentException("Http method or uri may be not null");
        }
        this.method = method;
        this.uri = uri;
    }

    public HttpRequest(String httpVersion, String method, String uri) {
        super(httpVersion);
        this.method = method;
        this.uri = uri;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    // split会使用ArrayList+SubList, 性能不好.
    public static HttpRequest parseFromBytes(byte[] bytes) {
        int aStart, aEnd;
        int bStart, bEnd;
        int cStart, cEnd;

        aStart = findNonWhitespace(bytes, 0);
        aEnd = findWhitespace(bytes, aStart);

        bStart = findNonWhitespace(bytes, aEnd);
        bEnd = findWhitespace(bytes, bStart);

        cStart = findNonWhitespace(bytes, bEnd);
        cEnd = findEndOfString(bytes);

        return new HttpRequest(cStart < cEnd ? new String(bytes, cStart, cEnd - cStart) : "", //
                new String(bytes, aStart, aEnd - aStart), new String(bytes, bStart, bEnd - bStart));
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(httpVersion).append(" ");
        buf.append(method).append("\r\n");
        buf.append(uri).append("\r\n");
        return buf.toString();
    }
}