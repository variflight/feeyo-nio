package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#unsuback">MQTTV3.1/unsuback</a>
 */
public final class MqttUnsubAckMessage extends MqttMessage {

    public MqttUnsubAckMessage(MqttFixedHeader fixedHeader, MqttMessageIdVariableHeader variableHeader) {
        super(fixedHeader, variableHeader, null);
    }

    @Override
    public MqttMessageIdVariableHeader variableHeader() {
        return (MqttMessageIdVariableHeader) super.variableHeader();
    }
}