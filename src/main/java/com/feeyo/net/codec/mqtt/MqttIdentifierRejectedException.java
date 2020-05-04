package com.feeyo.net.codec.mqtt;

import com.feeyo.net.codec.UnknownProtocolException;

/**
 * A {@link MqttIdentifierRejectedException} which is thrown when a CONNECT request contains invalid client identifier.
 */
public final class MqttIdentifierRejectedException extends UnknownProtocolException {

	private static final long serialVersionUID = -7935244491143554105L;

    public MqttIdentifierRejectedException() { }

    public MqttIdentifierRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttIdentifierRejectedException(String message) {
        super(message);
    }

    public MqttIdentifierRejectedException(Throwable cause) {
        super(cause);
    }

}
