package com.feeyo.net.codec.http2;

public interface Flags {
	
	public static final byte NONE 					= 0x0;
	public static final byte ACK 					= 0x1; 	// 用于 Settings 和 Ping
	public static final byte END_STREAM 			= 0x1; 	// 用于 Headers 和 Data
	public static final byte END_HEADERS 			= 0x4; 	// 用于 Headers 和 Continuation
	
	public static final byte END_PUSH_PROMISE 		= 0x4;
	public static final byte PADDED 				= 0x8; 	// 用于 Headers 和 Data
	public static final byte PRIORITY 				= 0x20; // 用于 Headers
	public static final byte COMPRESSED				= 0x20; // 用于 Data

}
