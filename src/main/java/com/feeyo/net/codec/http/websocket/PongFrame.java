package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PongFrame extends AbstractControlFrame {
	//
	public PongFrame() {
		super(OpCode.PONG);
	}

	public PongFrame setPayload(byte[] bytes) {
		setPayload(ByteBuffer.wrap(bytes));
		return this;
	}

	public PongFrame setPayload(String payload) {
		setPayload(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)));
		return this;
	}

	@Override
	public Type getType() {
		return Type.PONG;
	}
}
