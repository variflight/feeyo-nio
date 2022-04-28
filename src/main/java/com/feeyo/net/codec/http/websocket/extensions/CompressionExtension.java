package com.feeyo.net.codec.http.websocket.extensions;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.Frame;

public abstract class CompressionExtension extends DefaultExtension {
	@Override
	public void isFrameValid(final Frame inputFrame) throws UnknownProtocolException {
		if (inputFrame.isDataFrame() && (inputFrame.isRsv2() || inputFrame.isRsv3())) {
			throw new UnknownProtocolException("bad rsv RSV1: " + inputFrame.isRsv1() + " RSV2: " + inputFrame.isRsv2()
					+ " RSV3: " + inputFrame.isRsv3());
		}
		//
		if (inputFrame.isControlFrame() && (inputFrame.isRsv1() || inputFrame.isRsv2() || inputFrame.isRsv3())) {
			throw new UnknownProtocolException("bad rsv RSV1: " + inputFrame.isRsv1() + " RSV2: " + inputFrame.isRsv2()
					+ " RSV3: " + inputFrame.isRsv3());
		}
	}
}
