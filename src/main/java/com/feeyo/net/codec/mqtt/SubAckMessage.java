package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#suback">MQTTV3.1/suback</a>
 */
public final class SubAckMessage extends Message {

	public SubAckMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader, SubAckPayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MessageIdVariableHeader variableHeader() {
		return (MessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public SubAckPayload payload() {
		return (SubAckPayload) super.payload();
	}
}
