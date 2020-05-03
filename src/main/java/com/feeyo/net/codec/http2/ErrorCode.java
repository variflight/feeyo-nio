package com.feeyo.net.codec.http2;

// Standard HTTP/2 error codes
//
// @see http://tools.ietf.org/html/draft-ietf-httpbis-http2-17#section-7
//
public final class ErrorCode {
	
	//  no errors
	public static final int NO_ERROR = 0;
	
	public static final int PROTOCOL_ERROR = 1;
	public static final int INTERNAL_ERROR = 2;
	public static final int FLOW_CONTROL_ERROR = 3;
	public static final int SETTINGS_TIMEOUT_ERROR = 4;
	public static final int STREAM_CLOSED_ERROR = 5;
	public static final int FRAME_SIZE_ERROR = 6;
	public static final int REFUSED_STREAM_ERROR = 7;
	public static final int CANCEL_STREAM_ERROR = 8;
	public static final int COMPRESSION_ERROR = 9;
	public static final int HTTP_CONNECT_ERROR = 10;
	public static final int ENHANCE_YOUR_CALM_ERROR = 11;
	public static final int INADEQUATE_SECURITY_ERROR = 12;
	public static final int HTTP_1_1_REQUIRED_ERROR = 13;
	
	//
	public static boolean check(int errorCode) {
		if (errorCode >= 0 && errorCode <= 13) {
			return true;
		}
		return false;
	}

}
