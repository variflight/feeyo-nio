package com.feeyo.net.codec.mqtt;

/**
 * Utility class with factory methods to create different types of MQTT
 * messages.
 */
public final class MqttMessageFactory {

	public static MqttMessage newMessage(MqttFixedHeader fixedHeader, Object variableHeader, Object payload) {
		switch (fixedHeader.messageType()) {
		case CONNECT:
			return new MqttConnectMessage(fixedHeader, (MqttConnectVariableHeader) variableHeader,
					(MqttConnectPayload) payload);

		case CONNACK:
			return new MqttConnAckMessage(fixedHeader, (MqttConnAckVariableHeader) variableHeader);

		case SUBSCRIBE:
			return new MqttSubscribeMessage(fixedHeader, (MqttMessageIdVariableHeader) variableHeader,
					(MqttSubscribePayload) payload);

		case SUBACK:
			return new MqttSubAckMessage(fixedHeader, (MqttMessageIdVariableHeader) variableHeader,
					(MqttSubAckPayload) payload);

		case UNSUBACK:
			return new MqttUnsubAckMessage(fixedHeader, (MqttMessageIdVariableHeader) variableHeader);

		case UNSUBSCRIBE:
			return new MqttUnsubscribeMessage(fixedHeader, (MqttMessageIdVariableHeader) variableHeader,
					(MqttUnsubscribePayload) payload);

		case PUBLISH:
			return new MqttPublishMessage(fixedHeader, (MqttPublishVariableHeader) variableHeader, (byte[]) payload);

		case PUBACK:
			return new MqttPubAckMessage(fixedHeader, (MqttMessageIdVariableHeader) variableHeader);
		case PUBREC:
		case PUBREL:
		case PUBCOMP:
			return new MqttMessage(fixedHeader, variableHeader);

		case PINGREQ:
		case PINGRESP:
		case DISCONNECT:
			return new MqttMessage(fixedHeader);

		default:
			throw new IllegalArgumentException("unknown message type: " + fixedHeader.messageType());
		}
	}

	public static MqttMessage newInvalidMessage(Throwable cause) {
		return new MqttMessage(null, null, null, cause);
	}

	public static MqttMessage newInvalidMessage(MqttFixedHeader fixedHeader, Object variableHeader, Throwable cause) {
		return new MqttMessage(fixedHeader, variableHeader, null, cause);
	}

	private MqttMessageFactory() {
	}
}
