package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

/**
 * Process the payload (for demasking, validating, etc..)
 */
public interface PayloadProcessor {
	//
	public void process(ByteBuffer payload);
	public void reset(Frame frame);
}
