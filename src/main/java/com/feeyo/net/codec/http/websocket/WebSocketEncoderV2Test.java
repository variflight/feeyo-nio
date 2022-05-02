package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.extensions.PerMessageDeflateExtension;

public class WebSocketEncoderV2Test {
	
	private static WebSocketDecoderV2 webSocketDecoder = new WebSocketDecoderV2();
	private static WebSocketEncoderV2 webSocketEncoder = new WebSocketEncoderV2();
	
	public static void main(String[] args) throws UnknownProtocolException {
		//
		PerMessageDeflateExtension extension = new PerMessageDeflateExtension();
		webSocketDecoder.setExtension(extension);
		webSocketEncoder.setExtension(extension);
		//
		String payloadTxt = "\n";
		for(int i=0; i<10; i++) {
			payloadTxt += (i + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx,\n");
		}
		//
		//
		byte[] maskingKey = {0x11, 0x22, 0x33, 0x44};
		Frame frame1 = new Frame(OpCode.BINARY);
		frame1.setFin(true);
		frame1.setRsv1(false);
		frame1.setRsv2(false);
		frame1.setMasked(true);
		frame1.setMask(maskingKey);
		frame1.setPayload(payloadTxt.getBytes());
		ByteBuffer buf = webSocketEncoder.encode(frame1);
		//
		byte[] bb = new byte[buf.position()];
		for (int j = 0; j < buf.position(); j++) {
			bb[j] = buf.get(j);
		}
		//
		List<Frame> frameList = webSocketDecoder.decode(bb);
		for(Frame frame2: frameList) {
			//
			System.out.println(frame2.getOpCode() + ", " + 
					frame2.isFin() + "," + 
					frame2.isRsv1() + "," + 
					frame2.isRsv2() + ", " + 
					frame2.isRsv3() + ", " + 
					frame2.getPayloadAsUTF8() + ", " + 
					frame2.getPayloadLength());
		}
	}

}
