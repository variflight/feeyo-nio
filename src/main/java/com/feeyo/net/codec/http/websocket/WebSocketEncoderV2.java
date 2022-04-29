package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

import com.feeyo.net.codec.http.websocket.extensions.IExtension;

public class WebSocketEncoderV2 {
	//
	private IExtension extension = null;
	 //
    public void setExtension(IExtension extension) {
		this.extension = extension;
	}
	
	//
	// @see https://datatracker.ietf.org/doc/html/rfc6455
	// @see https://stackoverflow.com/questions/25189006/how-to-frame-websocket-data-in-javascript
	// @see https://stackoverflow.com/questions/43715746/how-to-encode-binary-data-over-65536-bytes-to-websocket-frame-on-c
	// @see https://github.com/netty/netty/blob/4.1/codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocket08FrameEncoder.java
	//
	public ByteBuffer encode(Frame frame) {
		if ( frame == null ) {
			return null;
		}
		//
		if (extension != null) {
			extension.encodeFrame(frame);
		}
		//
		ByteBuffer buffer = null;
		//
		int payloadLength = frame.getPayloadLength();
		byte b0 = frame.getFinRsvOp(); // (byte) (0x8f & (frame.getOpCode() | 0xf0));
		if (payloadLength <= 125) {
			buffer = ByteBuffer.allocate(2 + payloadLength);
			buffer.put(b0);
			buffer.put((byte) payloadLength);
			//
		} else if (payloadLength <= 0xffff) {
			buffer = ByteBuffer.allocate(4 + payloadLength);
			buffer.put(b0);
			buffer.put((byte) 126); 
			buffer.put((byte) (payloadLength >>> 8));
			buffer.put((byte) (payloadLength & 0xff));
			//
		} else {
			/* 0xffff < len <= 2^63 */
			buffer = ByteBuffer.allocate(10 + payloadLength);
			buffer.put(b0);
			buffer.put((byte) 127);
			buffer.putLong(payloadLength);
		}
		//
		for (int i = 0; i < frame.getPayloadLength(); i++) {
			buffer.put(frame.getPayload().get(i));
		}
		return buffer;
	}

}