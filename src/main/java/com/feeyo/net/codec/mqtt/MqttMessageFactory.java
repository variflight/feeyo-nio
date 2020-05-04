package com.feeyo.net.codec.mqtt;

/**
 * Utility class with factory methods to create different types of MQTT
 * messages.
 */
public final class MqttMessageFactory {

	public static Message newMessage(FixedHeader fixedHeader, Object variableHeader, Object payload) {
		switch (fixedHeader.messageType()) {
		case CONNECT:
			return new MqttConnectMessage(fixedHeader, (MqttConnectVariableHeader) variableHeader,
					(MqttConnectPayload) payload);

		case CONNACK:
			return new MqttConnAckMessage(fixedHeader, (MqttConnAckVariableHeader) variableHeader);

		case SUBSCRIBE:
			return new MqttSubscribeMessage(fixedHeader, (MessageIdVariableHeader) variableHeader,
					(MqttSubscribePayload) payload);

		case SUBACK:
			return new MqttSubAckMessage(fixedHeader, (MessageIdVariableHeader) variableHeader,
					(MqttSubAckPayload) payload);

		case UNSUBACK:
			return new MqttUnsubAckMessage(fixedHeader, (MessageIdVariableHeader) variableHeader);

		case UNSUBSCRIBE:
			return new MqttUnsubscribeMessage(fixedHeader, (MessageIdVariableHeader) variableHeader,
					(MqttUnsubscribePayload) payload);

		case PUBLISH:
			return new MqttPublishMessage(fixedHeader, (MqttPublishVariableHeader) variableHeader, (byte[]) payload);

		case PUBACK:
			return new MqttPubAckMessage(fixedHeader, (MessageIdVariableHeader) variableHeader);
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

	private MqttMessageFactory() {
	}
}
