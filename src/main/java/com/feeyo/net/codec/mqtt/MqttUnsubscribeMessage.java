package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#unsubscribe">
 *     MQTTV3.1/unsubscribe</a>
 */
public final class MqttUnsubscribeMessage extends Message {

	public MqttUnsubscribeMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader,
			MqttUnsubscribePayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MessageIdVariableHeader variableHeader() {
		return (MessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public MqttUnsubscribePayload payload() {
		return (MqttUnsubscribePayload) super.payload();
	}
}
