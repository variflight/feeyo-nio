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
    private byte getMaskByte(boolean mask) {
		return mask ? ( byte ) -128 : 0;
	}
	
	//
	// @see https://datatracker.ietf.org/doc/html/rfc6455
	// @see https://stackoverflow.com/questions/25189006/how-to-frame-websocket-data-in-javascript
	// @see https://stackoverflow.com/questions/43715746/how-to-encode-binary-data-over-65536-bytes-to-websocket-frame-on-c
	// @see https://github.com/netty/netty/blob/4.1/codec-http/src/main/java/io/netty/handler/codec/http/websocketx/WebSocket08FrameEncoder.java
    // @see https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_WebSocket_servers
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
		boolean isMasked = frame.isMasked();
		int payloadLength = frame.getPayloadLength();
		byte b0 = frame.getFinRsvOp(); // (byte) (0x8f & (frame.getOpCode() | 0xf0));
		if (payloadLength <= 125) {
			buffer = ByteBuffer.allocate(2 + (isMasked ? 4 : 0) + payloadLength);
			buffer.put(b0);
			buffer.put((byte) (payloadLength | getMaskByte(isMasked)));
			//
		} else if (payloadLength <= 0xffff) {
			buffer = ByteBuffer.allocate(4 + (isMasked ? 4 : 0) + payloadLength);
			buffer.put(b0);
			buffer.put((byte) (126 | getMaskByte(isMasked)) ); 
			buffer.put((byte) (payloadLength >>> 8));
			buffer.put((byte) (payloadLength & 0xff));
			//
		} else {
			/* 0xffff < len <= 2^63 */
			buffer = ByteBuffer.allocate(10 + (isMasked ? 4 : 0) + payloadLength);
			buffer.put(b0);
			buffer.put((byte) (127 | getMaskByte(isMasked)) );
			buffer.putLong(payloadLength);
		}
		// 
		if (isMasked) {
			buffer.put(frame.getMask());
		}
		//
		ByteBuffer payload = frame.getPayload();
		for (int i = 0; i < frame.getPayloadLength(); i++) {
			// Reading and Unmasking the Data
			buffer.put( isMasked ? (byte)(payload.get(i) ^ frame.getMask()[i % 4]) : payload.get(i) );
		}
		return buffer;
	}
}