package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#connect">MQTTV3.1/connect</a>
 */
public final class ConnectMessage extends Message {

	public ConnectMessage(FixedHeader fixedHeader, ConnectVariableHeader variableHeader,
			ConnectPayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public ConnectVariableHeader variableHeader() {
		return (ConnectVariableHeader) super.variableHeader();
	}

	@Override
	public ConnectPayload payload() {
		return (ConnectPayload) super.payload();
	}
}
