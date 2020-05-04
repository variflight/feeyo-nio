package com.feeyo.net.codec.mqtt;

import com.feeyo.net.codec.UnknownProtocolException;

/**
 * A {@link IdentifierRejectedException} which is thrown when a CONNECT request contains invalid client identifier.
 */
public final class IdentifierRejectedException extends UnknownProtocolException {

	private static final long serialVersionUID = -7935244491143554105L;

    public IdentifierRejectedException() { }

    public IdentifierRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentifierRejectedException(String message) {
        super(message);
    }

    public IdentifierRejectedException(Throwable cause) {
        super(cause);
    }

}
