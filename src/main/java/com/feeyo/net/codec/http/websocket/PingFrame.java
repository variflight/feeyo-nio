package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.feeyo.net.codec.UnknownProtocolException;

public class PingFrame extends AbstractControlFrame {
	//
	public PingFrame() {
		super(OpCode.PING);
	}

	public PingFrame setPayload(byte[] bytes) throws UnknownProtocolException {
		setPayload(ByteBuffer.wrap(bytes));
		return this;
	}

	public PingFrame setPayload(String payload) throws UnknownProtocolException {
		setPayload(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
		return this;
	}

	@Override
	public Type getType() {
		return Type.PING;
	}
}
