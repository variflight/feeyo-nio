package com.feeyo.net.codec.mqtt;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.feeyo.net.codec.UnknownProtocolException;

import static com.feeyo.net.codec.mqtt.MqttCodecUtil.*;

/**
 * Encodes Mqtt messages into bytes following the protocol specification v3.1
 * as described here <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html">MQTTV3.1</a>
 */
public final class MqttEncoder {
	
	private static final byte[] EMPTY_BYTES = {};

    public static final MqttEncoder INSTANCE = new MqttEncoder();

    private MqttEncoder() { }


    /**
     * This is the main encoding method.
     * It's only visible for testing.
     *
     * @param byteBufAllocator Allocates ByteBuf
     * @param message MQTT message to encode
     * @return ByteBuf with encoded bytes
     */
    public ByteBuffer encode(Message message) throws UnknownProtocolException {

        switch (message.fixedHeader().messageType()) {
            case CONNECT:
                return encodeConnectMessage( (ConnectMessage) message);

            case CONNACK:
                return encodeConnAckMessage((ConnAckMessage) message);

            case PUBLISH:
                return encodePublishMessage((PublishMessage) message);

            case SUBSCRIBE:
                return encodeSubscribeMessage((SubscribeMessage) message);

            case UNSUBSCRIBE:
                return encodeUnsubscribeMessage((UnsubscribeMessage) message);

            case SUBACK:
                return encodeSubAckMessage((SubAckMessage) message);

            case UNSUBACK:
            case PUBACK:
            case PUBREC:
            case PUBREL:
            case PUBCOMP:
                return encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(message);

            case PINGREQ:
            case PINGRESP:
            case DISCONNECT:
                return encodeMessageWithOnlySingleByteFixedHeader(message);

            default:
                throw new IllegalArgumentException(
                        "Unknown message type: " + message.fixedHeader().messageType().value());
        }
    }

    private static ByteBuffer encodeConnectMessage(
            ConnectMessage message) throws UnknownProtocolException {
        int payloadBufferSize = 0;

        FixedHeader mqttFixedHeader = message.fixedHeader();
        ConnectVariableHeader variableHeader = message.variableHeader();
        ConnectPayload payload = message.payload();
        MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(variableHeader.name(),
                (byte) variableHeader.version());

        // as MQTT 3.1 & 3.1.1 spec, If the User Name Flag is set to 0, the Password Flag MUST be set to 0
        if (!variableHeader.hasUserName() && variableHeader.hasPassword()) {
            throw new UnknownProtocolException("Without a username, the password MUST be not set");
        }

        // Client id
        String clientIdentifier = payload.clientIdentifier();
        if (!isValidClientId(mqttVersion, clientIdentifier)) {
            throw new IdentifierRejectedException("invalid clientIdentifier: " + clientIdentifier);
        }
        byte[] clientIdentifierBytes = encodeStringUtf8(clientIdentifier);
        payloadBufferSize += 2 + clientIdentifierBytes.length;

        // Will topic and message
        String willTopic = payload.willTopic();
        byte[] willTopicBytes = willTopic != null ? encodeStringUtf8(willTopic) : EMPTY_BYTES;
        byte[] willMessage = payload.willMessageInBytes();
        byte[] willMessageBytes = willMessage != null ? willMessage : EMPTY_BYTES;
        if (variableHeader.isWillFlag()) {
            payloadBufferSize += 2 + willTopicBytes.length;
            payloadBufferSize += 2 + willMessageBytes.length;
        }

        String userName = payload.userName();
        byte[] userNameBytes = userName != null ? encodeStringUtf8(userName) : EMPTY_BYTES;
        if (variableHeader.hasUserName()) {
            payloadBufferSize += 2 + userNameBytes.length;
        }

        byte[] password = payload.passwordInBytes();
        byte[] passwordBytes = password != null ? password : EMPTY_BYTES;
        if (variableHeader.hasPassword()) {
            payloadBufferSize += 2 + passwordBytes.length;
        }

        // Fixed header
        byte[] protocolNameBytes = mqttVersion.protocolNameBytes();
        int variableHeaderBufferSize = 2 + protocolNameBytes.length + 4;
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
        //
        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variablePartSize);
        buf.put((byte) getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variablePartSize);
        buf.putShort((short) protocolNameBytes.length);
        buf.put(protocolNameBytes);
        buf.put((byte) variableHeader.version());
        buf.put((byte) getConnVariableHeaderFlag(variableHeader));
        buf.putShort((short) variableHeader.keepAliveTimeSeconds());

        // Payload
        buf.putShort((short) clientIdentifierBytes.length);
        buf.put(clientIdentifierBytes, 0, clientIdentifierBytes.length);
        if (variableHeader.isWillFlag()) {
            buf.putShort((short) willTopicBytes.length);
            buf.put(willTopicBytes, 0, willTopicBytes.length);
            buf.putShort((short) willMessageBytes.length);
            buf.put(willMessageBytes, 0, willMessageBytes.length);
        }
        if (variableHeader.hasUserName()) {
            buf.putShort((short) userNameBytes.length);
            buf.put(userNameBytes, 0, userNameBytes.length);
        }
        if (variableHeader.hasPassword()) {
            buf.putShort((short) passwordBytes.length);
            buf.put(passwordBytes, 0, passwordBytes.length);
        }
        return buf;
    }

    private static int getConnVariableHeaderFlag(ConnectVariableHeader variableHeader) {
        int flagByte = 0;
        if (variableHeader.hasUserName()) {
            flagByte |= 0x80;
        }
        if (variableHeader.hasPassword()) {
            flagByte |= 0x40;
        }
        if (variableHeader.isWillRetain()) {
            flagByte |= 0x20;
        }
        flagByte |= (variableHeader.willQos() & 0x03) << 3;
        if (variableHeader.isWillFlag()) {
            flagByte |= 0x04;
        }
        if (variableHeader.isCleanSession()) {
            flagByte |= 0x02;
        }
        return flagByte;
    }

    private static ByteBuffer encodeConnAckMessage(
            ConnAckMessage message) {
    	ByteBuffer buf = ByteBuffer.allocate(4);
        buf.put((byte) getFixedHeaderByte1(message.fixedHeader()));
        buf.put((byte) 2);
        buf.put((byte) (message.variableHeader().isSessionPresent() ? 0x01 : 0x00));
        buf.put(message.variableHeader().connectReturnCode().byteValue());

        return buf;
    }

    private static ByteBuffer encodeSubscribeMessage(
            SubscribeMessage message) {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = 0;

        FixedHeader mqttFixedHeader = message.fixedHeader();
        MessageIdVariableHeader variableHeader = message.variableHeader();
        SubscribePayload payload = message.payload();

        for (TopicSubscription topic : payload.topicSubscriptions()) {
            String topicName = topic.topicName();
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            payloadBufferSize += 2 + topicNameBytes.length;
            payloadBufferSize += 1;
        }

        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variablePartSize);
        buf.put((byte) getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variablePartSize);

        // Variable Header
        int messageId = variableHeader.messageId();
        buf.putShort((short) messageId);

        // Payload
        for (TopicSubscription topic : payload.topicSubscriptions()) {
            String topicName = topic.topicName();
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            buf.putShort((short) topicNameBytes.length);
            buf.put(topicNameBytes, 0, topicNameBytes.length);
            buf.put((byte) topic.qualityOfService().value());
        }

        return buf;
    }

    private static ByteBuffer encodeUnsubscribeMessage(
            UnsubscribeMessage message) {
    	
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = 0;

        FixedHeader mqttFixedHeader = message.fixedHeader();
        MessageIdVariableHeader variableHeader = message.variableHeader();
        UnsubscribePayload payload = message.payload();

        for (String topicName : payload.topics()) {
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            payloadBufferSize += 2 + topicNameBytes.length;
        }

        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variablePartSize);
        buf.put((byte) getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variablePartSize);

        // Variable Header
        int messageId = variableHeader.messageId();
        buf.putShort((short) messageId);

        // Payload
        for (String topicName : payload.topics()) {
            byte[] topicNameBytes = encodeStringUtf8(topicName);
            buf.putShort((short) topicNameBytes.length);
            buf.put(topicNameBytes, 0, topicNameBytes.length);
        }

        return buf;
    }

    private static ByteBuffer encodeSubAckMessage(
            SubAckMessage message) {
        int variableHeaderBufferSize = 2;
        int payloadBufferSize = message.payload().grantedQoSLevels().size();
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);
        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variablePartSize);
        buf.put((byte) getFixedHeaderByte1(message.fixedHeader()));
        writeVariableLengthInt(buf, variablePartSize);
        buf.putShort((short) message.variableHeader().messageId());
        for (int qos : message.payload().grantedQoSLevels()) {
            buf.put((byte) qos);
        }

        return buf;
    }

    private static ByteBuffer encodePublishMessage(
            PublishMessage message) {
    	//
        FixedHeader mqttFixedHeader = message.fixedHeader();
        PublishVariableHeader variableHeader = message.variableHeader();
        //
        byte[] payload = message.payload();
        String topicName = variableHeader.topicName();
        byte[] topicNameBytes = encodeStringUtf8(topicName);

        int variableHeaderBufferSize = 2 + topicNameBytes.length +
                (mqttFixedHeader.qosLevel().value() > 0 ? 2 : 0);
        int payloadBufferSize = payload.length;
        int variablePartSize = variableHeaderBufferSize + payloadBufferSize;
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variablePartSize);

        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variablePartSize);
        buf.put((byte) getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variablePartSize);
        buf.putShort((short) topicNameBytes.length);
        buf.put(topicNameBytes);
        if (mqttFixedHeader.qosLevel().value() > 0) {
            buf.putShort((short) variableHeader.packetId());
        }
        buf.put(payload);

        return buf;
    }

    private static ByteBuffer encodeMessageWithOnlySingleByteFixedHeaderAndMessageId(
            Message message) {
        FixedHeader mqttFixedHeader = message.fixedHeader();
        MessageIdVariableHeader variableHeader = (MessageIdVariableHeader) message.variableHeader();
        int msgId = variableHeader.messageId();

        int variableHeaderBufferSize = 2; // variable part only has a message id
        int fixedHeaderBufferSize = 1 + getVariableLengthInt(variableHeaderBufferSize);
        ByteBuffer buf = ByteBuffer.allocate(fixedHeaderBufferSize + variableHeaderBufferSize);
        buf.put((byte) getFixedHeaderByte1(mqttFixedHeader));
        writeVariableLengthInt(buf, variableHeaderBufferSize);
        buf.putShort((short) msgId);

        return buf;
    }

    private static ByteBuffer encodeMessageWithOnlySingleByteFixedHeader(
            
            Message message) {
        FixedHeader mqttFixedHeader = message.fixedHeader();
        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.put((byte) getFixedHeaderByte1(mqttFixedHeader));
        buf.put((byte) 0);

        return buf;
    }

    private static int getFixedHeaderByte1(FixedHeader header) {
        int ret = 0;
        ret |= header.messageType().value() << 4;
        if (header.isDup()) {
            ret |= 0x08;
        }
        ret |= header.qosLevel().value() << 1;
        if (header.isRetain()) {
            ret |= 0x01;
        }
        return ret;
    }

    //
    private static void writeVariableLengthInt(ByteBuffer buf, int num) {
        do {
            int digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            buf.put((byte)digit);
        } while (num > 0);
    }

    private static int getVariableLengthInt(int num) {
        int count = 0;
        do {
            num /= 128;
            count++;
        } while (num > 0);
        return count;
    }

    private static byte[] encodeStringUtf8(String s) {
      return s.getBytes(StandardCharsets.UTF_8);
    }
}
