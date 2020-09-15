package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

public class WsEncoder {
	
	public ByteBuffer encode(Frame frame) {
		int length = 1; // 固定有一个命令码，占一位
		length += frame.getPayloadLength();
		//
		byte header0 = (byte) (0x8f & (frame.getOpCode() | 0xf0));
		ByteBuffer buffer = null;
		if (length < 126) {
			buffer = ByteBuffer.allocate(2 + length);
			buffer.put(header0);
			buffer.put((byte) length);
		} else if (length < (1 << 16) - 1) {
			buffer = ByteBuffer.allocate(4 + length);
			buffer.put(header0);
			buffer.put((byte) 126);
			// 
			buffer.put((byte) (length >>> 8));
			buffer.put((byte) (length & 0xff));
		} else {
			buffer = ByteBuffer.allocate(10 + length);
			buffer.put(header0);
			buffer.put((byte) 127);
			buffer.put(new byte[] { 0, 0, 0, 0 });
			//
			buffer.put((byte) (length >>> 24));
			buffer.put((byte) (length >>> 16));
			buffer.put((byte) (length >>> 8));
			buffer.put((byte) (length & 0xff));
		}
		return buffer;
	}

}
