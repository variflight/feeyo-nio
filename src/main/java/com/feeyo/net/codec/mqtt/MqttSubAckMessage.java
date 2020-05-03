package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#suback">MQTTV3.1/suback</a>
 */
public final class MqttSubAckMessage extends MqttMessage {

	public MqttSubAckMessage(MqttFixedHeader fixedHeader, MqttMessageIdVariableHeader variableHeader,
			MqttSubAckPayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MqttMessageIdVariableHeader variableHeader() {
		return (MqttMessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public MqttSubAckPayload payload() {
		return (MqttSubAckPayload) super.payload();
	}
}
