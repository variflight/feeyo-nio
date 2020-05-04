package com.feeyo.net.codec.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.util.CompositeByteArray;

import static com.feeyo.net.codec.mqtt.MqttCodecUtil.*;

/**
 * Decodes Mqtt messages from bytes
 * 
 * <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html"> the MQTT protocol specification v3.1</a>
 */
public class MqttDecoder implements Decoder<List<Message>> {
    //
    private enum State {
        READ_FIXED_HEADER,
        READ_VARIABLE_HEADER,
        READ_PAYLOAD
    }
	//
    private static final int MAX_BYTES_IN_MESSAGE = 1024 * 8;
	private static final int MAX_BYTES_IN_BUFFER = 1024 * 1024 * 16;
    ///
    //
    public CompositeByteArray buffer = new CompositeByteArray();
    public int offset = 0;
    public State state = State.READ_FIXED_HEADER;
    //
    private FixedHeader fixedHeader;
    private Object variableHeader;
    private int remainingLength;
    
    ///
    // MQTT协议:
    // [ Fixed Header | Variable Header | Payload ]
    //
    @Override
	public List<Message> decode(byte[] data) throws UnknownProtocolException {
    	//
    	if (data == null || data.length == 0)
			return Collections.emptyList();
    	//
		// append
    	buffer.add( data );
    	//
		if ( buffer.getByteCount() > MAX_BYTES_IN_BUFFER )
			throw new UnknownProtocolException("Buffer exceed " + MAX_BYTES_IN_BUFFER + " bytes, offset=" + offset);
		
		//
		List<Message> out = new ArrayList<>();
		//
		int off0 = offset;
		state = State.READ_FIXED_HEADER;
		//
		try {
			//
			for(;;) {
				//
				switch ( state ) {
				case READ_FIXED_HEADER:
					fixedHeader = decodeFixedHeader(buffer);
					remainingLength = fixedHeader.remainingLength();
					this.state = State.READ_VARIABLE_HEADER;
					//
				case READ_VARIABLE_HEADER:
					//
					int off1 = offset;
					variableHeader = decodeVariableHeader(buffer, fixedHeader);
					if ( remainingLength > MAX_BYTES_IN_MESSAGE ) 
						throw new UnknownProtocolException("too large message: " + remainingLength + " bytes");
					//
					remainingLength -= ( offset - off1 ); 	// numberOfBytesConsumed
					this.state = State.READ_PAYLOAD;
					//
				case READ_PAYLOAD:
					//
					int off2 = offset;
					Object payloadResult = decodePayload(buffer, fixedHeader.messageType(), remainingLength, variableHeader);
					remainingLength -= ( offset - off2 );	// numberOfBytesConsumed
					if (remainingLength != 0) 
						throw new UnknownProtocolException("non-zero remaining payload bytes: " + remainingLength + " (" + fixedHeader.messageType() + ')');
					//
					this.state = State.READ_FIXED_HEADER;
					//
					//
					Message message = MessageFactory.newMessage(fixedHeader, variableHeader, payloadResult);
					out.add(message);
					//
					// reset
					fixedHeader = null;
					variableHeader = null;
					//
					int length = buffer.remaining(offset);
					if ( length > 0 ) {
						//
						if ( offset != 0 && (offset * 1.0F / buffer.getByteCount() ) > 0.55F ) {
							//
							CompositeByteArray oldBuffer = buffer;
							CompositeByteArray newBuffer = new CompositeByteArray();
							newBuffer.add( oldBuffer.getData(offset, length) );
							//
							oldBuffer.clear();
							oldBuffer = null;
							buffer = newBuffer;
							offset = 0;
							state = State.READ_FIXED_HEADER;
						}
						
					} else {
						buffer.clear();
						offset = 0;
						state = State.READ_FIXED_HEADER;
						break;
					}
				}
			}
			
		} catch (IndexOutOfBoundsException e) {
			// ignore IndexOutOfBoundsException
			offset = off0;
		}
    	//
    	return out;
    }

    //
    // 解析固定报头。标志是一个字节，其余长度是可变字节
    private FixedHeader decodeFixedHeader(CompositeByteArray buffer) throws UnknownProtocolException {
    	// read 1 byte
        short b1 = (short) (buffer.get( offset++ ) & 0xFF);
        //
        MessageType messageType = MessageType.valueOf(b1 >> 4);		// 报文类型
        boolean dupFlag = (b1 & 0x08) == 0x08;								// DUP 用来在保证消息的可靠传输，如果设置为 1，则在下面的变长中增加MessageId，并且需要回复确认，以保证消息传输完成；
        int qosLevel = (b1 & 0x06) >> 1;									// QoS 发布消息的服务质量，即：保证消息传递的次数，00：最多一次， 01：至少一次，10：一次
        boolean retain = (b1 & 0x01) != 0;									// PUBLISH报文的保留标志
        //
        int remainingLength = 0;											// 剩余长度，包括可变报头和负载的数据
        int multiplier = 1;
        short digit;
        int loops = 0;
        do {
        	//
        	// 剩余长度字段使用一个变长度编码方案，对小于128的值它使用单字节编码， 
        	// 更大的值处理机制是每个字节的低7位用于编码数据，最高位是标志位，为1时，表示长度不足，需要使用二个字节继续保存，最大4个字节；
        	//
        	// 表示的范围如下：
        	// 1Byte，从 0(0x00) 到 127(0x7f)
        	// 2Byte，从 128(0x80,0x01) 到 16383(0xff,0x7f)
        	// 3Byte，从 16384(0x80,0x80,0x01) 到 2097151(0xff,0xff,0x7F)
        	// 4Byte，从 2097152 (0x80,0x80,0x80,0x01) 到 268435455(0xff,0xff,0xff,0x7F)
        	//
            digit = (short) (buffer.get(offset++) & 0xff);
            remainingLength += (digit & 127) * multiplier;
            multiplier *= 128;
            //
            loops++;
        } while ((digit & 128) != 0 && loops < 4);
        
        //
        // MQTT protocol limits Remaining Length to 4 bytes
        if (loops == 4 && (digit & 128) != 0) 
            throw new UnknownProtocolException("remaining length exceeds 4 digits (" + messageType + ')');
        //
        FixedHeader decodedFixedHeader = new FixedHeader(messageType, dupFlag, MqttQoS.valueOf(qosLevel), retain, remainingLength);
        return validateFixedHeader(resetUnusedFields(decodedFixedHeader));
    }

    //
    // 可变报头 Variable header
	private Object decodeVariableHeader(CompositeByteArray buffer, FixedHeader fixedHeader)
			throws UnknownProtocolException {
		//
		// 某些控制报文包含一个可变报头部分，固定报头和负载之间
		switch (fixedHeader.messageType()) {
		case CONNECT:
			return decodeConnectionVariableHeader(buffer);
		case CONNACK:
			return decodeConnAckVariableHeader(buffer);
		case SUBSCRIBE:
		case UNSUBSCRIBE:
		case SUBACK:
		case UNSUBACK:
		case PUBACK:
		case PUBREC:
		case PUBCOMP:
		case PUBREL:
			return decodeMessageIdVariableHeader(buffer);
		case PUBLISH:
			return decodePublishVariableHeader(buffer, fixedHeader);
		case PINGREQ:
		case PINGRESP:
		case DISCONNECT:
			// Empty variable header
			return null;
		}
		return null; // should never reach here
	}

	private ConnectVariableHeader decodeConnectionVariableHeader(CompositeByteArray buffer)
			throws UnknownProtocolException {
		String protocolName = decodeString(buffer);		// 协议名
		byte protocolLevel = buffer.get(offset++);		// 协议级别
		MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(protocolName, protocolLevel);

		int b1 = buffer.get(offset++) & 0xff;
		int keepAlive = decodeMsbLsb(buffer);
		boolean hasUserName = (b1 & 0x80) == 0x80;
		boolean hasPassword = (b1 & 0x40) == 0x40;
		boolean willRetain = (b1 & 0x20) == 0x20;
		int willQos = (b1 & 0x18) >> 3;
		boolean willFlag = (b1 & 0x04) == 0x04;
		boolean cleanSession = (b1 & 0x02) == 0x02;
		if (mqttVersion == MqttVersion.MQTT_3_1_1) {
            boolean zeroReservedFlag = (b1 & 0x01) == 0x0;
            if (!zeroReservedFlag) {
                // MQTT v3.1.1: The Server MUST validate that the reserved flag in the CONNECT Control Packet is
                // set to zero and disconnect the Client if it is not zero.
                // See http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc385349230
                throw new UnknownProtocolException("non-zero reserved flag");
            }
        }

		return new ConnectVariableHeader(mqttVersion.protocolName(), mqttVersion.protocolLevel(), hasUserName,
				hasPassword, willRetain, willQos, willFlag, cleanSession, keepAlive);
    }

	private ConnAckVariableHeader decodeConnAckVariableHeader(CompositeByteArray buffer) {
		boolean sessionPresent = (buffer.get(offset++) & 0xff & 0x01) == 0x01;		// 连接确认标志
		byte returnCode = buffer.get(offset++);										// 连接返回码
		return new ConnAckVariableHeader(ConnectReturnCode.valueOf(returnCode), sessionPresent);
	}

	private MessageIdVariableHeader decodeMessageIdVariableHeader(CompositeByteArray buffer)
			throws UnknownProtocolException {
		int messageId = decodeMessageId(buffer);
		return MessageIdVariableHeader.from(messageId);
	}

	private PublishVariableHeader decodePublishVariableHeader(CompositeByteArray buffer,
			FixedHeader fixedHeader) throws UnknownProtocolException {
		//
		String topicName = decodeString(buffer);
		if (!isValidPublishTopicName(topicName))
			throw new UnknownProtocolException("invalid publish topic name: " + topicName + " (contains wildcards)");
		//
		int messageId = -1;
		if (fixedHeader.qosLevel().value() > 0) 
			messageId = decodeMessageId(buffer);
		//
		return new PublishVariableHeader(topicName, messageId);
	}

	private int decodeMessageId(CompositeByteArray buffer) throws UnknownProtocolException {
		int messageId = decodeMsbLsb(buffer);
		if (!isValidMessageId(messageId))
			throw new UnknownProtocolException("invalid messageId: " + messageId);
		return messageId;
	}

    //
	// 解析有效载荷 Payload
	private Object decodePayload(CompositeByteArray buffer, MessageType messageType, int remainingLength, Object variableHeader)
			throws UnacceptableProtocolVersionException, IdentifierRejectedException {
		//
		// 有效载荷主要包含应用消息，每个控制报文类型不一样
		switch (messageType) {
		case CONNECT:
			return decodeConnectionPayload(buffer, (ConnectVariableHeader) variableHeader);
		case SUBSCRIBE:
			return decodeSubscribePayload(buffer, remainingLength);
		case SUBACK:
			return decodeSubackPayload(buffer, remainingLength);
		case UNSUBSCRIBE:
			return decodeUnsubscribePayload(buffer, remainingLength);
		case PUBLISH:
			return decodePublishPayload(buffer, remainingLength);
		default:
			// unknown payload , no byte consumed
			return null;
		}
	}

	//
	private ConnectPayload decodeConnectionPayload(CompositeByteArray buffer,
			ConnectVariableHeader connectVariableHeader)
			throws UnacceptableProtocolVersionException, IdentifierRejectedException {
		//
		String clientId = decodeString(buffer);
		MqttVersion mqttVersion = MqttVersion.fromProtocolNameAndLevel(connectVariableHeader.name(), (byte) connectVariableHeader.version());
		if (!isValidClientId(mqttVersion, clientId)) 
			throw new IdentifierRejectedException("invalid clientIdentifier: " + clientId);
		//
		String willTopic = null;
		byte[] willMessage = null;
		if (connectVariableHeader.isWillFlag()) {
			willTopic = decodeString(buffer, 0, 32767);
			willMessage = decodeByteArray(buffer);
		}
		//
		String userName = connectVariableHeader.hasUserName() ? decodeString(buffer) : null;
		byte[] password = connectVariableHeader.hasPassword() ? decodeByteArray(buffer): null;
		//
		return new ConnectPayload(clientId, 
				willTopic != null ? willTopic : null,
				willMessage != null ? willMessage : null,
				userName != null ? userName : null,
				password != null ? password : null);
	}

	//
	private SubscribePayload decodeSubscribePayload(CompositeByteArray buffer, int remainingLength) {
		List<TopicSubscription> subscribeTopics = new ArrayList<TopicSubscription>();
		//
		int numberOfBytesConsumed = 0;
		while (numberOfBytesConsumed < remainingLength) {
			int old = offset;
			String topicName = decodeString(buffer);
			numberOfBytesConsumed += (offset - old);
			int qos = buffer.get(offset++) & 0xFF & 0x03;
			numberOfBytesConsumed++;
			subscribeTopics.add(new TopicSubscription(topicName, MqttQoS.valueOf(qos)));
		}
		return new SubscribePayload(subscribeTopics);
	}

	//
	private SubAckPayload decodeSubackPayload(CompositeByteArray buffer, int remainingLength) {
		List<Integer> grantedQos = new ArrayList<Integer>();
		//
		int numberOfBytesConsumed = 0;
		while (numberOfBytesConsumed < remainingLength) {
			int qos = buffer.get(offset++) & 0xFF;
			if (qos != MqttQoS.FAILURE.value()) {
				qos &= 0x03;
			}
			numberOfBytesConsumed++;
			grantedQos.add(qos);
		}
		return new SubAckPayload(grantedQos);
	}

	private UnsubscribePayload decodeUnsubscribePayload(CompositeByteArray buffer, int remainingLength) {
		List<String> unsubscribeTopics = new ArrayList<String>();
		//
		int numberOfBytesConsumed = 0;
		while (numberOfBytesConsumed < remainingLength) {
			int old = offset;
			String topicName = decodeString(buffer);
			numberOfBytesConsumed += (offset - old);
			unsubscribeTopics.add(topicName);
		}
		return new UnsubscribePayload(unsubscribeTopics);
	}

	private byte[] decodePublishPayload(CompositeByteArray buffer, int remainingLength) {
		byte[] data = buffer.getData(offset, remainingLength);
		offset += remainingLength;
		return data;
	}

	private String decodeString(CompositeByteArray buffer) {
		return decodeString(buffer, 0, Integer.MAX_VALUE);
	}

	private String decodeString(CompositeByteArray buffer, int minBytes, int maxBytes) {
		int size = decodeMsbLsb(buffer);
		if (size < minBytes || size > maxBytes) {
			// skip Bytes
			offset += size;
			return null;
		}
		//
		byte[] data = buffer.getData(offset, size);
		offset += size;
		return new String(data, StandardCharsets.UTF_8);
	}

	private byte[] decodeByteArray(CompositeByteArray buffer) {
		int size = decodeMsbLsb(buffer);
		byte[] data = buffer.getData(offset, size);
		offset += size;
		return data;
	}

	private int decodeMsbLsb(CompositeByteArray buffer) {
		short msbSize = (short) (buffer.get(offset++) & 0xff);
		short lsbSize = (short) (buffer.get(offset++) & 0xff);
		int result = msbSize << 8 | lsbSize;
		if (result < 0 || result > 65535) 
			result = -1;
		return result;
	}
}