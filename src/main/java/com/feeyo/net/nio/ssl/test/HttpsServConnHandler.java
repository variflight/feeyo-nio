package com.feeyo.net.nio.ssl.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feeyo.net.nio.NIOHandler;
import com.feeyo.net.nio.util.ByteUtil;


public class HttpsServConnHandler implements NIOHandler<HttpsServConn>{
	
	private static Logger LOGGER = LoggerFactory.getLogger( HttpsServConnHandler.class );


	@Override
	public void handleReadEvent(final HttpsServConn conn, byte[] data) throws IOException {
		
		System.out.println("https handleReadEvent: " + ByteUtil.dump( data, 0, 800 ) );

		String body = "<html><head><title>TestHttps</title></head><body><h3>HelloWorld!</h3></body></html>";
		
		//
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("HTTP/1.0 ").append("200 OK").append("\r\n");
		strBuffer.append("Server: niossl/0.1").append("\r\n");
		strBuffer.append("Content-type: ").append("text/html; charset=iso-8859-1").append("\r\n");
		strBuffer.append("Content-length: ").append( body.getBytes().length ).append("\r\n");
		strBuffer.append("\r\n");
		strBuffer.append(body);
		
		conn.handleSslWrite( ByteBuffer.wrap( strBuffer.toString().getBytes() ));
		
		
	}
	
	@Override
	public void onConnected(HttpsServConn conn) throws IOException {
		// LOGGER.debug("onConnected(): {}", conn);
	}

	@Override
	public void onConnectFailed(HttpsServConn conn, Exception e) {
		// LOGGER.debug("onConnectFailed(): {}", conn);
	}

	@Override
	public void onClosed(HttpsServConn conn, String reason) {
		if ( reason != null && !reason.equals("stream closed"))
			LOGGER.warn("onClosed(): {}, {}", conn, reason);
	}
}