package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#subscribe">
 *     MQTTV3.1/subscribe</a>
 */
public final class MqttSubscribeMessage extends Message {

	public MqttSubscribeMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader,
			MqttSubscribePayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MessageIdVariableHeader variableHeader() {
		return (MessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public MqttSubscribePayload payload() {
		return (MqttSubscribePayload) super.payload();
	}
}