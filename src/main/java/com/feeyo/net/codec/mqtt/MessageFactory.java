package com.feeyo.net.codec.mqtt;

/**
 * Utility class with factory methods to create different types of MQTT
 * messages.
 */
public final class MessageFactory {

	public static Message newMessage(FixedHeader fixedHeader, Object variableHeader, Object payload) {
		switch (fixedHeader.messageType()) {
		case CONNECT:
			return new ConnectMessage(fixedHeader, (ConnectVariableHeader) variableHeader,
					(ConnectPayload) payload);

		case CONNACK:
			return new ConnAckMessage(fixedHeader, (ConnAckVariableHeader) variableHeader);

		case SUBSCRIBE:
			return new SubscribeMessage(fixedHeader, (MessageIdVariableHeader) variableHeader,
					(SubscribePayload) payload);

		case SUBACK:
			return new SubAckMessage(fixedHeader, (MessageIdVariableHeader) variableHeader,
					(SubAckPayload) payload);

		case UNSUBACK:
			return new UnsubAckMessage(fixedHeader, (MessageIdVariableHeader) variableHeader);

		case UNSUBSCRIBE:
			return new UnsubscribeMessage(fixedHeader, (MessageIdVariableHeader) variableHeader,
					(UnsubscribePayload) payload);

		case PUBLISH:
			return new PublishMessage(fixedHeader, (PublishVariableHeader) variableHeader, (byte[]) payload);

		case PUBACK:
			return new PubAckMessage(fixedHeader, (MessageIdVariableHeader) variableHeader);
		case PUBREC:
		case PUBREL:
		case PUBCOMP:
			return new Message(fixedHeader, variableHeader);

		case PINGREQ:
		case PINGRESP:
		case DISCONNECT:
			return new Message(fixedHeader);

		default:
			throw new IllegalArgumentException("unknown message type: " + fixedHeader.messageType());
		}
	}

	public static Message newInvalidMessage(Throwable cause) {
		return new Message(null, null, null, cause);
	}

	public static Message newInvalidMessage(FixedHeader fixedHeader, Object variableHeader, Throwable cause) {
		return new Message(fixedHeader, variableHeader, null, cause);
	}

	private MessageFactory() {
	}
}
