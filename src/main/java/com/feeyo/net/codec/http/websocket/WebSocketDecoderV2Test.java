package com.feeyo.net.codec.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import com.feeyo.buffer.BufferPool;
import com.feeyo.buffer.bucket.BucketBufferPool;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.nio.NetConfig;
import com.feeyo.net.nio.NetSystem;
import com.feeyo.net.nio.util.ExecutorUtil;

public class WebSocketDecoderV2Test {
	
	//
	private static WebSocketDecoderV2 webSocketDecoder = new WebSocketDecoderV2();
	private static WebSocketEncoder webSocketEncoder = new WebSocketEncoder();

	public static void main(String[] args) throws UnknownProtocolException, IOException {

		BufferPool bufferPool = new BucketBufferPool(1024 * 1024 * 40, 1024 * 1024 * 80, new int[] { 1024 });
		new NetSystem(bufferPool, ExecutorUtil.create("BusinessExecutor-", 2), ExecutorUtil.create("TimerExecutor-", 2),
				ExecutorUtil.createScheduled("TimerSchedExecutor-", 1));
		NetConfig systemConfig = new NetConfig(1048576, 4194304);
		NetSystem.getInstance().setNetConfig(systemConfig);
		//
		
		Frame frame = new Frame(OpCode.BINARY);
		frame.setFin(true);
		frame.setRsv1(true);
		frame.setRsv2(true);
		frame.setPayload("helloworld,, xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx!!");
		ByteBuffer buf = webSocketEncoder.encode(frame);
		//
		byte[] bb = new byte[buf.position()];
		for (int i = 0; i < buf.position(); i++) {
			bb[i] = buf.get(i);
		}
		//
		List<Frame> frameList = webSocketDecoder.decode(bb);
		for(Frame frame2: frameList) {
			System.out.println(frame2.getOpCode() + ", " + frame2.isFin() + "," + frame2.isRsv1() + "," + frame2.isRsv2()
					+ ", " + frame2.getPayloadAsUTF8() + ", " + frame2.getPayloadLength());
		}
	}

}
