package com.feeyo.net.codec.http.websocket;

/*
 * https://www.rfc-editor.org/rfc/rfc6455.html#section-5.4
 * https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers
 */
public class OpCode {
	public static final byte CONTINUATION = (byte) 0x00;	// 继续帧
	public static final byte TEXT = (byte) 0x01; // 文本帧, UTF-8编码的文本数据
	public static final byte BINARY = (byte) 0x02; // 二进制帧
	public static final byte CLOSE = (byte) 0x08; // 连接关闭
	public static final byte PING = (byte) 0x09; // ping
	public static final byte PONG = (byte) 0x0A; // pong
	public static final byte UNDEFINED = (byte) -1; //

	public static boolean isControlFrame(byte opcode) {
		return (opcode >= CLOSE);
	}

	public static boolean isDataFrame(byte opcode) {
		return (opcode == TEXT) || (opcode == BINARY);
	}
	
	public static boolean isContinuousFrame(byte opcode) {
		return opcode == CONTINUATION;
	}

	public static boolean isKnown(byte opcode) {
		return (opcode == CONTINUATION) || (opcode == TEXT) || (opcode == BINARY) || (opcode == CLOSE)
				|| (opcode == PING) || (opcode == PONG);
	}

	public static String name(byte opcode) {
		switch (opcode) {
		case -1:
			return "NO-OP";
		case CONTINUATION:
			return "CONTINUATION";
		case TEXT:
			return "TEXT";
		case BINARY:
			return "BINARY";
		case CLOSE:
			return "CLOSE";
		case PING:
			return "PING";
		case PONG:
			return "PONG";
		default:
			return "NON-SPEC[" + opcode + "]";
		}
	}

}
