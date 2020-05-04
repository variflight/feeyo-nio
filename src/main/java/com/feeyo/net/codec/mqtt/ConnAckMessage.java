package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connack">MQTTV3.1/connack</a>
 */
public final class ConnAckMessage extends Message {

    public ConnAckMessage(FixedHeader fixedHeader, ConnAckVariableHeader variableHeader) {
        super(fixedHeader, variableHeader);
    }

    @Override
    public ConnAckVariableHeader variableHeader() {
        return (ConnAckVariableHeader) super.variableHeader();
    }
}
