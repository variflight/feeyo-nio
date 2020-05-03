package com.feeyo.net.codec.http2;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.feeyo.net.codec.http2.test.Http2Connection;

/**
 * A logical bidirectional stream
 */
public class Http2Stream {

	long unacknowledgedBytesRead = 0;
	long bytesLeftInWriteWindow;

	final int id;
	final Http2Connection conn;

	//
	private int errorCode = -1;

	public Http2Stream(int id, Http2Connection conn, 
			boolean outFinished, boolean inFinished, List<Header> headers) {
		
		this.id = id;
		this.conn = conn;
	}

	public int getId() {
		return id;
	}
	
	public boolean isOpen() {
		if (errorCode != -1 ) {
			return false;
		}
		return true;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public void addBytesToWriteWindow(long delta) {
		bytesLeftInWriteWindow += delta;
	}
	
	public void receiveRstStream(int errorCode) {
		if (this.errorCode == -1) {
			this.errorCode = errorCode;
		}
	}
	

	// Headers example
	//
	// :method, GET
	// :path, /
	// :scheme, http
	// :authority, 127.0.0.1:8066
	// accept, */*
	// accept-encoding, gzip, deflate
	// user-agent, nghttp2/1.34.0
	//
	public void receiveHeaders(List<Header> headers) {
		
		System.out.println( "receiveHeaders" );
		
		boolean open = isOpen();
		if (!open) {
			conn.removeStream(id);
		}
		
		String path = null;
		for (Header header : headers) {
			if ( Util.equal(header.name, Header.TARGET_PATH ) ) {
				path = new String(header.value);
			}
			System.out.println( new String( header.name ) + ", " + new String( header.value ));
			
		}
		
		System.out.println("path=" + path);
		
		if ( path != null ) {
			List<Header> responseHeaders = Arrays.asList(
					new Header(":status", "404"),
					new Header("server", "FWS"),
					new Header("content-type", "content-type: text/html; charset=UTF-8"));
			
			    try {
					conn.writeSynReply(id, false, responseHeaders);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
	}

	public void receiveFin() {
		System.out.println( "receiveFin" );
	}
	
	public void receiveData(byte[] data, int length) {
		System.out.println( "receiveData" );
	}

}
