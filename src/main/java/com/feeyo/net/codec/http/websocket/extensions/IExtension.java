package com.feeyo.net.codec.http.websocket.extensions;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.http.websocket.Frame;

/**
 * WebSocket 扩展
 */
public interface IExtension {
	//
	void decodeFrame(Frame inputFrame) throws UnknownProtocolException;
	void encodeFrame(Frame inputFrame);
	//
	boolean acceptProvidedExtensionAsServer(String inputExtensionHeader);
	boolean acceptProvidedExtensionAsClient(String inputExtensionHeader);
	void isFrameValid(Frame inputFrame) throws UnknownProtocolException;
	String getProvidedExtensionAsClient();
	String getProvidedExtensionAsServer();
	//
	void reset();
	String toString();
}
