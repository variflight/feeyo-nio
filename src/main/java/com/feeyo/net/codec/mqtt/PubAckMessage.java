package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#puback">MQTTV3.1/puback</a>
 */
public final class PubAckMessage extends Message {

    public PubAckMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader) {
        super(fixedHeader, variableHeader);
    }

    @Override
    public MessageIdVariableHeader variableHeader() {
        return (MessageIdVariableHeader) super.variableHeader();
    }
}
