package com.feeyo.net.codec.http.websocket;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;

public class WsDecoder implements Decoder<Frame> {
	//
	private enum State
    {
        START,
        PAYLOAD_LEN,
        PAYLOAD_LEN_BYTES,
        MASK,
        MASK_BYTES,
        PAYLOAD
    }
	
    // State specific
    private State state = State.START;
    private int cursor = 0;
	

	@Override
	public Frame decode(byte[] buffer) throws UnknownProtocolException {

		
		return null;
	}

}
