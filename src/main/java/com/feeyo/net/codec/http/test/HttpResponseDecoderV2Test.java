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

public class HttpResponseDecoderV2Test {

    public static void main(String[] args) throws Exception {
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
    }

    private static void testContentLength() throws UnknownProtocolException {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++)
            data[i] = 54;

        HttpResponse response = new HttpResponse(HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getReasonPhrase());
        response.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(data.length));
        response.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        response.setContent(data);

        byte[] bb = new HttpResponseEncoder().encodeToByteArray(response);

        HttpResponseDecoderV2 decoder = new HttpResponseDecoderV2();
        for (int i = 0; i < 10; i++) {
            HttpResponse rr = decoder.decode(bb);
            System.out.println(rr.toString());
        }
    }

    private static void testChunk() throws UnknownProtocolException {
        byte[] data1 = new byte[1024];
        for (int i = 0; i < data1.length; i++)
            data1[i] = 54;
        byte[] data2 = new byte[512];
        for (int i = 0; i < data2.length; i++)
            data2[i] = 54;
        byte[] data3 = new byte[256];
        for (int i = 0; i < data3.length; i++)
            data3[i] = 54;

        HttpResponse httpResponse = new HttpResponse(HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED.getCode(),
                HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED.getReasonPhrase());
        httpResponse.addHeader(HttpHeaderNames.TRANSFER_ENCODING, "gzip, chunked");
        httpResponse.addHeader(HttpHeaderNames.CONNECTION, "keep-alive");
        httpResponse.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");

        byte[] headerBytes = getHeaderBytes(httpResponse);
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
        HttpResponseDecoderV2 decoder = new HttpResponseDecoderV2();
        HttpResponse rr = decoder.decode(result);
        System.out.println(rr.toString());
        System.out.println("=======================================");
    }

    private static final String CRLF = "\r\n";
    private static final String SP = " ";
    private static final String COLON = ":";

    private static byte[] getHeaderBytes(HttpResponse response) {
        // headline
        StringBuffer headerSb = new StringBuffer();
        headerSb.append(response.getHttpVersion()).append(SP);
        headerSb.append(response.getStatusCode()).append(SP);
        headerSb.append(response.getReasonPhrase()).append(CRLF);

        // headers
        for (Map.Entry<String, String> h : response.headers().entrySet()) {
            headerSb.append(h.getKey()).append(COLON).append(SP);
            headerSb.append(h.getValue()).append(CRLF);
        }
        headerSb.append(CRLF);
        return headerSb.toString().getBytes();
    }
}