package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#publish">MQTTV3.1/publish</a>
 */
public class PublishMessage extends Message {

	public PublishMessage(FixedHeader fixedHeader, PublishVariableHeader variableHeader, byte[] payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public PublishVariableHeader variableHeader() {
		return (PublishVariableHeader) super.variableHeader();
	}

	@Override
	public byte[] payload() {
		return (byte[]) super.payload();
	}

	public PublishMessage copy() {
		return new PublishMessage(fixedHeader(), variableHeader(), payload());
	}

}
