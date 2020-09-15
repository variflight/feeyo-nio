package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.feeyo.net.codec.UnknownProtocolException;

public class ContinuationFrame extends DataFrame {
	//
	public ContinuationFrame() {
		super(OpCode.CONTINUATION);
	}

	public ContinuationFrame setPayload(ByteBuffer buf) throws UnknownProtocolException {
		super.setPayload(buf);
		return this;
	}

	public ContinuationFrame setPayload(byte buf[]) throws UnknownProtocolException {
		return this.setPayload(ByteBuffer.wrap(buf));
	}

	public ContinuationFrame setPayload(String message) throws UnknownProtocolException {
		return this.setPayload(message.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public Type getType() {
		return Type.CONTINUATION;
	}
}