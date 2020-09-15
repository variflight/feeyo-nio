package com.feeyo.net.codec.http.websocket;

public class CloseFrame extends AbstractControlFrame {
	//
	public CloseFrame() {
		super(OpCode.CLOSE);
	}

	@Override
	public Type getType() {
		return Type.CLOSE;
	}

	/**
	 * Truncate arbitrary reason into something that will fit into the
	 * CloseFrame limits.
	 * 
	 * @param reason the arbitrary reason to possibly truncate.
	 * @return the possibly truncated reason string.
	 */
	public static String truncate(String reason) {
		if (reason == null) 
			return null;
		//
		int maxSize = (AbstractControlFrame.MAX_CONTROL_PAYLOAD - 2);
		if (reason.length() <= maxSize) {
			return reason;
		}
		return reason.substring(0, maxSize);
	}
}
