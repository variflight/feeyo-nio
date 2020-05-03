package com.feeyo.net.codec;

public class UnknownProtocolException extends Exception {

	private static final long serialVersionUID = 7243824194940173465L;

	public UnknownProtocolException() {
		super();
	}
	
	public UnknownProtocolException(final String message) {
		super(message);
	}

	public UnknownProtocolException(final Throwable cause) {
		super(cause);
	}

	public UnknownProtocolException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
