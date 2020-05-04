package com.feeyo.net.codec.mqtt;

import com.feeyo.net.codec.UnknownProtocolException;

public final class UnacceptableProtocolVersionException extends UnknownProtocolException {

	private static final long serialVersionUID = 169640476379221786L;

    public UnacceptableProtocolVersionException() { }

    public UnacceptableProtocolVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnacceptableProtocolVersionException(String message) {
        super(message);
    }

    public UnacceptableProtocolVersionException(Throwable cause) {
        super(cause);
    }

}
