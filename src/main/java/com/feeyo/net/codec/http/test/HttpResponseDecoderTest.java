package com.feeyo.net.codec.http.test;

import com.feeyo.net.codec.http.*;

public class HttpResponseDecoderTest {
    public static void main(String[] args) {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++)
            data[i] = 11;

        HttpResponse request = new HttpResponse(HttpResponseStatus.OK.getCode(), HttpResponseStatus.OK.getReasonPhrase());
        request.addHeader(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(data.length));
        request.addHeader(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        request.setContent(data);

        byte[] bb = new HttpResponseEncoder().encodeToByteArray(request);

        HttpResponseDecoder decoder = new HttpResponseDecoder();
        for (int i = 0; i < 10; i++) {
            HttpResponse rr = decoder.decode(bb);
            System.out.println(rr.toString());
        }
    }
}