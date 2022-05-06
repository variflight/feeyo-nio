package com.feeyo.net.codec.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.extensions.PerMessageDeflateExtension;

public class WebSocketDecoderV2Test2 {
	//
	private static WebSocketDecoderV2 webSocketDecoder = new WebSocketDecoderV2();
	private static WebSocketEncoderV2 webSocketEncoder = new WebSocketEncoderV2();
	private static ExecutorService executorService = Executors.newFixedThreadPool(100);

	public static void main(String[] args) throws UnknownProtocolException, IOException {
		//
		PerMessageDeflateExtension extension = new PerMessageDeflateExtension();
		extension.acceptProvidedExtensionAsServer("permessage-deflate; client_max_window_bits");
		webSocketDecoder.setExtension(extension);
		webSocketEncoder.setExtension(extension);
		//
		AtomicInteger counter = new AtomicInteger(0);
		//
		int size = 10000;
		CountDownLatch cdl = new CountDownLatch(size);
		for(int i=0; i<size; i++) {
			//
			try {
				executorService.execute(new Runnable() {
					@Override
					public void run() {
						//
						try {
							Frame frame = new Frame(OpCode.BINARY);
							frame.setFin(true);
							frame.setPayload(("##xxxxxxxxxxxxxxx" + counter.incrementAndGet()).getBytes());
							ByteBuffer buf = webSocketEncoder.encode(frame);
							//
							byte[] bb = new byte[buf.position()];
							for (int j = 0; j < buf.position(); j++) {
								bb[j] = buf.get(j);
							}
							//
							List<Frame> frameList = webSocketDecoder.decode(bb);
							for(Frame frame2: frameList) {
								System.out.println(frame2.getOpCode() + ", " + frame2.isFin() + "," + frame2.isRsv1() + "," + frame2.isRsv2()
										+ ", " + frame2.getPayloadAsUTF8() + ", " + frame2.getPayloadLength());
							}
						} catch(Throwable e) {
							e.printStackTrace();
						} finally {
							cdl.countDown();
						}
					}
				});
			} catch(RejectedExecutionException e) {
				cdl.countDown();
			}
			
		}
		//
		try {
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//
		executorService.shutdownNow();
	}

}
