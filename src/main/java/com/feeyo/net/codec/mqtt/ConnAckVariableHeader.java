package com.feeyo.net.codec.mqtt;

public final class ConnAckVariableHeader {
	//
    private final ConnectReturnCode connectReturnCode;

    private final boolean sessionPresent;

    public ConnAckVariableHeader(ConnectReturnCode connectReturnCode, boolean sessionPresent) {
        this.connectReturnCode = connectReturnCode;
        this.sessionPresent = sessionPresent;
    }

    public ConnectReturnCode connectReturnCode() {
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
