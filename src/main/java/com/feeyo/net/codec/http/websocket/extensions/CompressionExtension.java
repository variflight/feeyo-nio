package com.feeyo.net.codec.http.websocket.extensions;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.Frame;

public abstract class CompressionExtension extends DefaultExtension {
	//
	@Override
	public void isFrameValid(final Frame inputFrame) throws UnknownProtocolException {
		if (inputFrame.isDataFrame() && (inputFrame.isRsv2() || inputFrame.isRsv3())) {
			String msg = String.format("bad rsv RSV1: %s RSV2:%s  RSV3:%s", 
					inputFrame.isRsv1(), inputFrame.isRsv2(), inputFrame.isRsv3());
			throw new UnknownProtocolException(msg);
		}
		//
		if (inputFrame.isControlFrame() && (inputFrame.isRsv1() || inputFrame.isRsv2() || inputFrame.isRsv3())) {
			String msg = String.format("bad rsv RSV1: %s RSV2:%s  RSV3:%s", 
					inputFrame.isRsv1(), inputFrame.isRsv2(), inputFrame.isRsv3());
			throw new UnknownProtocolException(msg);
		}
	}
}
