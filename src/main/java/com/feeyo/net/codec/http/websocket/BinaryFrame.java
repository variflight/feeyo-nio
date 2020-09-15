package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.feeyo.net.codec.UnknownProtocolException;

public class BinaryFrame extends DataFrame {
	//
	public BinaryFrame() {
		super(OpCode.BINARY);
	}

	public BinaryFrame setPayload(ByteBuffer buf) throws UnknownProtocolException {
		super.setPayload(buf);
		return this;
	}

	public BinaryFrame setPayload(byte[] buf) throws UnknownProtocolException {
		setPayload(ByteBuffer.wrap(buf));
		return this;
	}

	public BinaryFrame setPayload(String payload) throws UnknownProtocolException {
		setPayload(payload.getBytes(StandardCharsets.UTF_8));
		return this;
	}

	@Override
	public Type getType() {
		return Type.BINARY;
	}
}
