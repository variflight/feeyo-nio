package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PingFrame extends ControlFrame {
	//
	public PingFrame() {
		super(OpCode.PING);
	}

	public PingFrame setPayload(byte[] bytes) {
		setPayload(ByteBuffer.wrap(bytes));
		return this;
	}

	public PingFrame setPayload(String payload) {
		setPayload(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
		return this;
	}

	@Override
	public Type getType() {
		return Type.PING;
	}
}
