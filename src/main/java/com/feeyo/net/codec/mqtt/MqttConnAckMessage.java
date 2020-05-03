package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connack">MQTTV3.1/connack</a>
 */
public final class MqttConnAckMessage extends MqttMessage {

    public MqttConnAckMessage(MqttFixedHeader fixedHeader, MqttConnAckVariableHeader variableHeader) {
        super(fixedHeader, variableHeader);
    }

    @Override
    public MqttConnAckVariableHeader variableHeader() {
        return (MqttConnAckVariableHeader) super.variableHeader();
    }
}
