package com.feeyo.net.codec.http.test;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.*;

import java.nio.ByteBuffer;
import java.util.Map;

public class HttpRequestDecoderTest {

    public static void main(String[] args) throws UnknownProtocolException {
        testContentLength();
        testChunk();
    }

    private static void testContentLength() throws UnknownProtocolException {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++)
            data[i] = 11;

        HttpRequest request = new HttpRequest("POST", "/raft/message");
        request.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(data.length));
        request.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        request.setContent(data);

        byte[] bb = new HttpRequestEncoder().encode(request);

        long start = System.currentTimeMillis();
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        for (int i = 0; i < 100; i++) {
            HttpRequest rr = decoder.decode(bb);
            System.out.println(rr.toString());
            System.out.println(rr.headers());
        }

        long end = System.currentTimeMillis();
        System.out.println("total cost is " + (end - start));
        System.out.println("=======================================");
    }

    private static void testChunk() throws UnknownProtocolException {
        byte[] data1 = new byte[1024];
        for (int i = 0; i < data1.length; i++)
            data1[i] = 11;
        byte[] data2 = new byte[512];
        for (int i = 0; i < data2.length; i++)
            data2[i] = 22;
        byte[] data3 = new byte[256];
        for (int i = 0; i < data3.length; i++)
            data3[i] = 33;

        HttpRequest request = new HttpRequest("POST", "/raft/message");
        request.addHeader(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        request.addHeader(HttpHeaderNames.CONNECTION, "keep-alive");
        request.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        byte[] headerBytes = getHeaderBytes(request);
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        buffer.put(headerBytes);
        buffer.put(Integer.toHexString(data1.length).getBytes());
        buffer.put(CRLF.getBytes());
        buffer.put(data1);
        buffer.put(CRLF.getBytes());

        buffer.put(Integer.toHexString(data2.length).getBytes());
        buffer.put(CRLF.getBytes());
        buffer.put(data2);
        buffer.put(CRLF.getBytes());

        buffer.put(Integer.toHexString(data3.length).getBytes());
        buffer.put(CRLF.getBytes());
        buffer.put(data3);
        buffer.put(CRLF.getBytes());

        buffer.put("0".getBytes());
        buffer.put(CRLF.getBytes());
        buffer.put(CRLF.getBytes());

        buffer.flip();
        byte[] result = new byte[buffer.limit()];
        buffer.get(result);
        HttpRequestDecoder decoder = new HttpRequestDecoder();
        HttpRequest rr = decoder.decode(result);
        System.out.println(rr.toString());
        System.out.println(rr.headers());
        System.out.println(rr.getContent().length);
        System.out.println("=======================================");
    }

    private static final String CRLF = "\r\n";
    private static final String SP = " ";
    private static final String COLON = ":";

    private static byte[] getHeaderBytes(HttpRequest request) {
        //
        //  httpHeader ( host & headers)
        // ----------------------------------------------------
        StringBuffer headerSb = new StringBuffer();
        headerSb.append( request.getMethod() ).append( SP );
        headerSb.append( UriUtil.parsePath( request.getUri() ) ).append( SP );
        headerSb.append( request.getHttpVersion() ).append(CRLF);
        //
        for (Map.Entry<String, String> h : request.headers().entrySet()) {
            headerSb.append(h.getKey()).append(COLON).append(SP);
            headerSb.append(h.getValue()).append(CRLF);
        }
        headerSb.append(CRLF);

        return headerSb.toString().getBytes();
    }
}