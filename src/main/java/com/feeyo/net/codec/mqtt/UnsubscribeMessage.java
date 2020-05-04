package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#unsubscribe">
 *     MQTTV3.1/unsubscribe</a>
 */
public final class UnsubscribeMessage extends Message {

	public UnsubscribeMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader,
			UnsubscribePayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MessageIdVariableHeader variableHeader() {
		return (MessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public UnsubscribePayload payload() {
		return (UnsubscribePayload) super.payload();
	}
}
