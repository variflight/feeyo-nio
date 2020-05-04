package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#subscribe">
 *     MQTTV3.1/subscribe</a>
 */
public final class SubscribeMessage extends Message {

	public SubscribeMessage(FixedHeader fixedHeader, MessageIdVariableHeader variableHeader,
			SubscribePayload payload) {
		super(fixedHeader, variableHeader, payload);
	}

	@Override
	public MessageIdVariableHeader variableHeader() {
		return (MessageIdVariableHeader) super.variableHeader();
	}

	@Override
	public SubscribePayload payload() {
		return (SubscribePayload) super.payload();
	}
}