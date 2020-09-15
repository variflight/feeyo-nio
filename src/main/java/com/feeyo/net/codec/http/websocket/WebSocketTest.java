package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

import com.feeyo.net.codec.UnknownProtocolException;

public class WebSocketTest {
	//
	private static WebSocketDecoder webSocketDecoder = new WebSocketDecoder();
	private static WebSocketEncoder webSocketEncoder = new WebSocketEncoder();
	
	public static void main(String[] args) throws UnknownProtocolException {
		
		Frame frame = new Frame(Frame.TEXT);
		frame.setPayload("helloworld!!");
		ByteBuffer buf = webSocketEncoder.encode(frame);
		//
		Frame frame2 = webSocketDecoder.decode(buf.array());
		System.out.println( frame2.getOpCode() + ", " +  frame2.getPayloadAsUTF8() + ", " + frame2.getPayloadLength() );
	}

}
