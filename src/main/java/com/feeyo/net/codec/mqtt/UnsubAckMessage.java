package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#unsuback">MQTTV3.1/unsuback</a>
 */
public final class UnsubAckMessage extends Message {

    public UnsubAckMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader) {
        super(fixedHeader, variableHeader, null);
    }

    @Override
    public MessageIdVariableHeader variableHeader() {
        return (MessageIdVariableHeader) super.variableHeader();
    }
}