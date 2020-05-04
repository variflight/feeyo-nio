package com.feeyo.net.codec.mqtt;

import com.feeyo.net.codec.UnknownProtocolException;

public final class MqttUnacceptableProtocolVersionException extends UnknownProtocolException {

	private static final long serialVersionUID = 169640476379221786L;

    public MqttUnacceptableProtocolVersionException() { }

    public MqttUnacceptableProtocolVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttUnacceptableProtocolVersionException(String message) {
        super(message);
    }

    public MqttUnacceptableProtocolVersionException(Throwable cause) {
        super(cause);
    }

}
