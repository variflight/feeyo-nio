package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#unsubscribe">
 *     MQTTV3.1/unsubscribe</a>
 */
public final class MqttUnsubscribeMessage extends MqttMessage {

	public MqttUnsubscribeMessage(MqttFixedHeader fixedHeader, MqttMessageIdVariableHeader variableHeader,
			MqttUnsubscribePayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MqttMessageIdVariableHeader variableHeader() {
		return (MqttMessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public MqttUnsubscribePayload payload() {
		return (MqttUnsubscribePayload) super.payload();
	}
}
