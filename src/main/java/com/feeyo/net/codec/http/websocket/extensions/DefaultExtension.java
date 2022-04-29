package com.feeyo.net.codec.http.websocket.extensions;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.Frame;

public class DefaultExtension implements IExtension {
	//
	@Override
	public void decodeFrame(Frame inputFrame) throws UnknownProtocolException {
		//
	}

	@Override
	public void encodeFrame(Frame inputFrame) {
		//
	}

	@Override
	public boolean acceptProvidedExtensionAsServer(String inputExtension) {
		return true;
	}

	@Override
	public boolean acceptProvidedExtensionAsClient(String inputExtension) {
		return true;
	}

	@Override
	public void isFrameValid(Frame inputFrame) throws UnknownProtocolException {
		if (inputFrame.isRsv1() || inputFrame.isRsv2() || inputFrame.isRsv3()) {
			String msg = String.format("bad rsv RSV1: %s RSV2:%s  RSV3:%s", inputFrame.isRsv1(), inputFrame.isRsv2(), inputFrame.isRsv3());
			throw new UnknownProtocolException(msg);
		}
	}

	@Override
	public String getProvidedExtensionAsClient() {
		return "";
	}

	@Override
	public String getProvidedExtensionAsServer() {
		return "";
	}

	@Override
	public void reset() {
		//
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || (o != null && this.getClass() == o.getClass());
	}
}
