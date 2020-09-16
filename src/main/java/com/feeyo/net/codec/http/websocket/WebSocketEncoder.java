package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

public class WebSocketEncoder {
	//
	// @see https://stackoverflow.com/questions/25189006/how-to-frame-websocket-data-in-javascript
	//
	public ByteBuffer encodeToByteBuffer(Frame frame) {
		ByteBuffer buffer = null;
		int len = frame.getPayloadLength();
		byte header0 = (byte) (0x8f & (frame.getOpCode() | 0xf0));
		if (len <= 125) {
			buffer = ByteBuffer.allocate(2 + len);
			buffer.put(header0);
			buffer.put((byte) len);
			//
		} else if (len <= 0xffff) {
			buffer = ByteBuffer.allocate(4 + len);
			buffer.put(header0);
			buffer.put((byte) 126); 
			buffer.put((byte) (len >>> 8));
			buffer.put((byte) (len & 0xff));
			//
		} else {
			/* 0xffff < len <= 2^63 */
			buffer = ByteBuffer.allocate(10 + len);
			buffer.put(header0);
			buffer.put((byte) 127);
			buffer.put((byte)((len >> 56) & 0xff));
			buffer.put((byte)((len >> 48) & 0xff));
			buffer.put((byte)((len >> 40) & 0xff));
			buffer.put((byte)((len >> 32) & 0xff));
			buffer.put((byte)((len >> 24) & 0xff));
			buffer.put((byte)((len >> 16) & 0xff));
			buffer.put((byte)((len >> 8) & 0xff));
			buffer.put((byte)(len & 0xff));
		}
		//
		for (int i = 0; i < frame.getPayloadLength(); i++) {
			buffer.put(frame.getPayload().get(i));
		}
		//
		return buffer;
	}

}
