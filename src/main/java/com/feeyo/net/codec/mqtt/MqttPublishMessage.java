package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#publish">MQTTV3.1/publish</a>
 */
public class MqttPublishMessage extends Message {

	public MqttPublishMessage(FixedHeader fixedHeader, MqttPublishVariableHeader variableHeader, byte[] payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MqttPublishVariableHeader variableHeader() {
		return (MqttPublishVariableHeader) super.variableHeader();
	}

	@Override
	public byte[] payload() {
		return (byte[]) super.payload();
	}

	public MqttPublishMessage copy() {
		return new MqttPublishMessage(fixedHeader(), variableHeader(), payload());
	}

}
