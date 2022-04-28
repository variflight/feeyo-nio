package com.feeyo.net.codec.http.websocket.extensions;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.Frame;

public class DefaultExtension implements IExtension {
	//
	@Override
	public void decodeFrame(final Frame inputFrame) throws UnknownProtocolException {
	}

	@Override
	public void encodeFrame(final Frame inputFrame) {
	}

	@Override
	public boolean acceptProvidedExtensionAsServer(final String inputExtension) {
		return true;
	}

	@Override
	public boolean acceptProvidedExtensionAsClient(final String inputExtension) {
		return true;
	}

	@Override
	public void isFrameValid(final Frame inputFrame) throws UnknownProtocolException {
		if (inputFrame.isRsv1() || inputFrame.isRsv2() || inputFrame.isRsv3()) {
			throw new UnknownProtocolException("bad rsv RSV1: " + inputFrame.isRsv1() + " RSV2: " + inputFrame.isRsv2()
					+ " RSV3: " + inputFrame.isRsv3());
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
	public IExtension copyInstance() {
		return new DefaultExtension();
	}

	@Override
	public void reset() {
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
	public boolean equals(final Object o) {
		return this == o || (o != null && this.getClass() == o.getClass());
	}
}
