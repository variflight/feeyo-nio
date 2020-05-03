package com.feeyo.net.nio.ssl.test;

import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;

import com.feeyo.net.nio.ssl.SslConnection;
import com.feeyo.net.nio.util.TimeUtil;

public class HttpsServConn extends SslConnection {
	
	private static final long TIMEOUT = 5 * 60 * 1000L;
	
	public HttpsServConn(SocketChannel socketChannel, SSLContext sslContext) {
		super(socketChannel, sslContext);
	}

	@Override
	public boolean isIdleTimeout() {
		return TimeUtil.currentTimeMillis() > Math.max(lastWriteTime, lastReadTime) + TIMEOUT;
	}
	
}
