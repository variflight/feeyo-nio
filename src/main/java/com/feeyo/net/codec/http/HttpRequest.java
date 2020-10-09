package com.feeyo.net.codec.http;

import java.util.Collections;
import java.util.Map;

public class HttpRequest extends HttpMessage {

    private final String method;
    private final String uri;
    private String path;
    private Map<String, String> parameters;

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
    
    public String getPath() {
    	if ( uri != null && path == null ) {
    		this.path = UriUtil.parsePath(false, uri);
    	}
    	return path;
    }

    //
    public Map<String, String> getParameters() {
    	if ( uri != null && parameters == null ) {
    		this.parameters = UriUtil.parseParameters(false, uri);
    	}
    	if ( parameters != null ) {
    		return parameters;
    	} 
    	return Collections.emptyMap();
    }
    
    /*
     * TODO: split会使用ArrayList+SubList, 性能不好.
     */
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
        StringBuilder sb = new StringBuilder(32);
        sb.append(httpVersion).append(" ");
        sb.append(method).append("\r\n");
        sb.append(uri).append("\r\n");
        return sb.toString();
    }
}