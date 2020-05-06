package com.feeyo.net.codec.http.test;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.*;
import com.feeyo.net.nio.NetConfig;
import com.feeyo.net.nio.NetSystem;
import com.feeyo.buffer.BufferPool;
import com.feeyo.buffer.bucket.BucketBufferPool;
import com.feeyo.net.nio.util.ExecutorUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpRequestDecoderV2Test {

    public static void main(String[] args) throws UnknownProtocolException {

        //
        // 构造 boss & time threadPool
        final BufferPool bufferPool2 = new BucketBufferPool(1024 * 1024 * 512, //
                1024 * 1024 * 1024, //
                new int[]{4096, 16384, 20480, 32768, 65536});    //
        //
        try {
            new NetSystem(bufferPool2, //
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(4),
                    ExecutorUtil.create("TimerExecutor-", 4), //
                    ExecutorUtil.createScheduled("TimerSchedExecutor-", 4));
        } catch (IOException e) {
            e.printStackTrace();
        }
        NetSystem.getInstance().setNetConfig(new NetConfig(1024, 2048));

        testChunk();
        testContentLength();
        testOthers();
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
        request.addHeader(HttpHeaderNames.TRANSFER_ENCODING, "gzip, chunked");
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
        HttpRequestDecoderV2 decoder = new HttpRequestDecoderV2();
        HttpRequest rr = decoder.decode(result);
        System.out.println(rr.toString());
        System.out.println(rr.headers());
        System.out.println(rr.getContent().length);
        System.out.println("=======================================");
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
        HttpRequestDecoderV2 decoder = new HttpRequestDecoderV2();
        // for (int i = 0; i < 100_0000; i++) {
        //     HttpRequest rr = decoder.decode(bb);
        //     if (rr == null) {
        //         System.out.println("decode error");
        //     }
        // }
        for (int i = 0; i < 100; i++) {
            HttpRequest rr = decoder.decode(bb);
            System.out.println(rr.toString());
            System.out.println(rr.headers());
        }

        long end = System.currentTimeMillis();
        System.out.println("total cost is " + (end - start));
        // try {
        //     Thread.sleep(100_000L);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        System.out.println("=======================================");
    }

    private static void testOthers() throws UnknownProtocolException {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++)
            data[i] = 11;

        HttpRequest request = new HttpRequest("POST", "/raft/message");
        request.addHeader(HttpHeaderNames.CONNECTION, "keep-alive");
        request.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        request.setContent(data);

        byte[] bb = new HttpRequestEncoder().encode(request);
        HttpRequestDecoderV2 decoder = new HttpRequestDecoderV2();
        HttpRequest rr = decoder.decode(bb);
        System.out.println(rr.toString());
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