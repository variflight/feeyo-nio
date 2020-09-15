package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ContinuationFrame extends DataFrame {
	//
	public ContinuationFrame() {
		super(OpCode.CONTINUATION);
	}

	public ContinuationFrame setPayload(ByteBuffer buf) {
		super.setPayload(buf);
		return this;
	}

	public ContinuationFrame setPayload(byte buf[]) {
		return this.setPayload(ByteBuffer.wrap(buf));
	}

	public ContinuationFrame setPayload(String message) {
		return this.setPayload( message.getBytes(StandardCharsets.UTF_8) );
	}

	@Override
	public Type getType() {
		return Type.CONTINUATION;
	}
}