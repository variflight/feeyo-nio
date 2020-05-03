package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connect">MQTTV3.1/connect</a>
 */
public final class MqttConnectMessage extends MqttMessage {

	public MqttConnectMessage(MqttFixedHeader fixedHeader, MqttConnectVariableHeader variableHeader,
			MqttConnectPayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MqttConnectVariableHeader variableHeader() {
		return (MqttConnectVariableHeader) super.variableHeader();
	}

	@Override
	public MqttConnectPayload payload() {
		return (MqttConnectPayload) super.payload();
	}
}
