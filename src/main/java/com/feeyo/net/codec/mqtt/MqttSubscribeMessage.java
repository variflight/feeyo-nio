package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#subscribe">
 *     MQTTV3.1/subscribe</a>
 */
public final class MqttSubscribeMessage extends MqttMessage {

	public MqttSubscribeMessage(MqttFixedHeader fixedHeader, MqttMessageIdVariableHeader variableHeader,
			MqttSubscribePayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MqttMessageIdVariableHeader variableHeader() {
		return (MqttMessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public MqttSubscribePayload payload() {
		return (MqttSubscribePayload) super.payload();
	}
}