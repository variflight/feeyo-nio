package com.feeyo.net.codec;

public class ProtocolException extends RuntimeException {
	//
	private static final long serialVersionUID = 1041354217926026387L;

	public ProtocolException() {
		super();
	}

	public ProtocolException(String message) {
		super(message);
	}

	public ProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProtocolException(Throwable cause) {
		super(cause);
	}
}
