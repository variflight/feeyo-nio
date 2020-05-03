package com.feeyo.net.codec.http.test;


import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.HttpHeaderNames;
import com.feeyo.net.codec.http.HttpRequest;
import com.feeyo.net.codec.http.HttpRequestEncoder;
import com.feeyo.net.codec.http.HttpRequestPipeliningDecoder;

import java.util.List;

public class HttpRequestPipeliningDecoderTest {

    public static void main(String[] args) {

        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++)
            data[i] = 11;

        HttpRequest request = new HttpRequest("POST", "/raft/message");
        request.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(data.length));
        request.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        request.setContent(data);

        System.out.println("before \r" + request + "\r" + request.getContent().length);

        byte[] bb = new HttpRequestEncoder().encode(request);


        HttpRequestPipeliningDecoder decoder = new HttpRequestPipeliningDecoder();
        List<HttpRequest> requests = null;

        long t1 = System.currentTimeMillis();
        //
        for (int i = 0; i < 10000; i++) {
            try {
                requests = decoder.decode(bb);

            } catch (UnknownProtocolException e) {
                e.printStackTrace();
            }
        }
        long t2 = System.currentTimeMillis();
        //
        System.out.println("timeMs=" + (t2 - t1));

        System.out.println("After \r" + requests.get(0) + "\r" + requests.get(0).getContent().length);
    }
}