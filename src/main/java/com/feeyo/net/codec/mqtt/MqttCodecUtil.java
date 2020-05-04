package com.feeyo.net.codec.mqtt;

import com.feeyo.net.codec.UnknownProtocolException;

final class MqttCodecUtil {

    private static final char[] TOPIC_WILDCARDS = {'#', '+'};
    private static final int MIN_CLIENT_ID_LENGTH = 1;
    private static final int MAX_CLIENT_ID_LENGTH = 23;

    //
    static boolean isValidPublishTopicName(String topicName) {
        // publish topic name must not contain any wildcard
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) 
                return false;
        }
        return true;
    }

    //
    static boolean isValidMessageId(int messageId) {
        return messageId != 0;
    }

    //
    static boolean isValidClientId(MqttVersion version, String clientId) {
        if (version == MqttVersion.MQTT_3_1) 
            return clientId != null && clientId.length() >= MIN_CLIENT_ID_LENGTH && clientId.length() <= MAX_CLIENT_ID_LENGTH;
        //
        // In 3.1.3.1 Client Identifier of MQTT 3.1.1 specification, The Server MAY allow ClientId’s
        // that contain more than 23 encoded bytes. And, The Server MAY allow zero-length ClientId.
        if (version == MqttVersion.MQTT_3_1_1) 
            return clientId != null;
        //
        throw new IllegalArgumentException(version + " is unknown mqtt version");
    }

    //
    static FixedHeader validateFixedHeader(FixedHeader fixedHeader) throws UnknownProtocolException {
        switch (fixedHeader.messageType()) {
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                if (fixedHeader.qosLevel() != MqttQoS.AT_LEAST_ONCE) 
                    throw new UnknownProtocolException(fixedHeader.messageType().name() + " message must have QoS 1");
            default:
                return fixedHeader;
        }
    }

    //
    static FixedHeader resetUnusedFields(FixedHeader fixedHeader) {
        switch (fixedHeader.messageType()) {
            case CONNECT:
            case CONNACK:
            case PUBACK:
            case PUBREC:
            case PUBCOMP:
            case SUBACK:
            case UNSUBACK:
            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
				if (fixedHeader.isDup() || fixedHeader.qosLevel() != MqttQoS.AT_MOST_ONCE || fixedHeader.isRetain()) 
					return new FixedHeader(fixedHeader.messageType(), false, MqttQoS.AT_MOST_ONCE, false, fixedHeader.remainingLength());
				//
                return fixedHeader;
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
				if (fixedHeader.isRetain()) 
					return new FixedHeader(fixedHeader.messageType(), fixedHeader.isDup(), fixedHeader.qosLevel(), false, fixedHeader.remainingLength());
				//
                return fixedHeader;
            default:
                return fixedHeader;
        }
    }
}
