package com.feeyo.net.codec.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.extensions.PerMessageDeflateExtension;

public class WebSocketDecoderV2Test {
	//
	private static WebSocketDecoderV2 webSocketDecoder = new WebSocketDecoderV2();
	private static WebSocketEncoderV2 webSocketEncoder = new WebSocketEncoderV2();

	public static void main(String[] args) throws UnknownProtocolException, IOException {
		//
		PerMessageDeflateExtension extension = new PerMessageDeflateExtension();
		extension.acceptProvidedExtensionAsServer("permessage-deflate; client_max_window_bits");
		webSocketDecoder.setExtension(extension);
		webSocketEncoder.setExtension(extension);
		//
		// TODO: 多包
		byte[] allBB = null;
		for(int i=0; i<10; i++ ) {
			Frame frame = new Frame(OpCode.BINARY);
			frame.setFin(true);
			frame.setPayload(("##xxxxxxxxxxxxxxx" + i).getBytes());
			ByteBuffer buf = webSocketEncoder.encode(frame);
			//
			byte[] bb = new byte[buf.position()];
			for (int j = 0; j < buf.position(); j++) {
				bb[j] = buf.get(j);
			}
			//
			if (allBB == null) {
				allBB = bb;
			} else {
				byte[] new_allBB = new byte[allBB.length + bb.length];
				System.arraycopy(allBB, 0, new_allBB, 0, allBB.length);
				System.arraycopy(bb, 0, new_allBB, allBB.length, bb.length);
				allBB = new_allBB;
			}
		}
		//
		// 分包
		for(int z=0; z<allBB.length; z++) {
			byte[] b0 = new byte[] { allBB[z] };
			//
			List<Frame> frameList = webSocketDecoder.decode(b0);
			for(Frame frame2: frameList) {
				System.out.println(frame2.getOpCode() + ", " + frame2.isFin() + "," + frame2.isRsv1() + "," + frame2.isRsv2()
						+ ", " + frame2.getPayloadAsUTF8() + ", " + frame2.getPayloadLength());
			}
		}
		
	}

}
