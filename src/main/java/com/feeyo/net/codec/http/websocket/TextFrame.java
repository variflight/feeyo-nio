package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.nio.util.BufferUtil;

public class TextFrame extends DataFrame {
	//
	public TextFrame() {
		super(OpCode.TEXT);
	}

	@Override
	public Type getType() {
		return Type.TEXT;
	}

	public TextFrame setPayload(String str) throws UnknownProtocolException {
		setPayload(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
		return this;
	}

	public String getPayloadAsUTF8() {
		if (data == null) {
			return null;
		}
		return BufferUtil.toUTF8String(data);
	}
}
