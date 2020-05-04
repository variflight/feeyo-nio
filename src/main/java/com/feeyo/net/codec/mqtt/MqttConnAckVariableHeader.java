package com.feeyo.net.codec.mqtt;

/**
 * Variable header of {@link MqttConnectMessage}
 */
public final class MqttConnAckVariableHeader {

    private final MqttConnectReturnCode connectReturnCode;

    private final boolean sessionPresent;

    public MqttConnAckVariableHeader(MqttConnectReturnCode connectReturnCode, boolean sessionPresent) {
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
    }

    public MqttConnectReturnCode connectReturnCode() {
        return connectReturnCode;
    }

    public boolean isSessionPresent() {
        return sessionPresent;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append("connectReturnCode=").append(connectReturnCode)
            .append(", sessionPresent=").append(sessionPresent)
            .append(']')
            .toString();
    }
}
