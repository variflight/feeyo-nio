package com.feeyo.net.codec.mqtt.test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.mqtt.MqttConnAckMessage;
import com.feeyo.net.codec.mqtt.MqttConnAckVariableHeader;
import com.feeyo.net.codec.mqtt.MqttConnectMessage;
import com.feeyo.net.codec.mqtt.MqttConnectPayload;
import com.feeyo.net.codec.mqtt.MqttConnectReturnCode;
import com.feeyo.net.codec.mqtt.MqttConnectVariableHeader;
import com.feeyo.net.codec.mqtt.MqttDecoder;
import com.feeyo.net.codec.mqtt.MqttEncoder;
import com.feeyo.net.codec.mqtt.FixedHeader;
import com.feeyo.net.codec.mqtt.Message;
import com.feeyo.net.codec.mqtt.MessageIdVariableHeader;
import com.feeyo.net.codec.mqtt.MessageType;
import com.feeyo.net.codec.mqtt.MqttPublishMessage;
import com.feeyo.net.codec.mqtt.MqttPublishVariableHeader;
import com.feeyo.net.codec.mqtt.MqttQoS;
import com.feeyo.net.codec.mqtt.MqttSubAckMessage;
import com.feeyo.net.codec.mqtt.MqttSubAckPayload;
import com.feeyo.net.codec.mqtt.MqttSubscribeMessage;
import com.feeyo.net.codec.mqtt.MqttSubscribePayload;
import com.feeyo.net.codec.mqtt.MqttTopicSubscription;
import com.feeyo.net.codec.mqtt.MqttUnsubscribeMessage;
import com.feeyo.net.codec.mqtt.MqttUnsubscribePayload;
import com.feeyo.net.codec.mqtt.MqttVersion;

//
// @see https://github.com/netty/netty/tree/4.1/codec-mqtt
//
public class MqttDecoderTest {

	private static final String CLIENT_ID = "RANDOM_TEST_CLIENT";
	private static final String WILL_TOPIC = "/my_will";
	private static final String WILL_MESSAGE = "gone";
	private static final String USER_NAME = "happy_user";
	private static final String PASSWORD = "123_or_no_pwd";

	private static final int KEEP_ALIVE_SECONDS = 600;

	private static final MqttDecoder decoder = new MqttDecoder();

	public static void main(String[] args) throws UnknownProtocolException {

		Message message = createConnectMessage(MqttVersion.MQTT_3_1);
		 message = createConnectMessage(MqttVersion.MQTT_3_1_1, null, PASSWORD);
		 message = createConnAckMessage();
		 message = createPublishMessage();
//		 message = createSubscribeMessage();
//		 message = createSubAckMessage();
//		 message = createUnsubscribeMessage();
//		 message = createMessageWithFixedHeader(MqttMessageType.PINGREQ);
//		 message = createConnAckMessage();
//		 message = createMessageWithFixedHeaderAndMessageIdVariableHeader(MqttMessageType.PUBACK); //
//		 message = createMessageWithFixedHeaderAndMessageIdVariableHeader(MqttMessageType.PUBREC); //
//		 message = createMessageWithFixedHeaderAndMessageIdVariableHeader(MqttMessageType.PUBREL); //
//		 message = createMessageWithFixedHeaderAndMessageIdVariableHeader(MqttMessageType.PUBCOMP); //
		//
		ByteBuffer byteBuf = MqttEncoder.INSTANCE.encode(message);
		byte[] buff = byteBuf.array();
		//
		byte[] buff1 = new byte[ buff.length - 8 ];
		byte[] buff2 = new byte[ buff.length - buff1.length ];
		System.arraycopy(buff, 0, buff1, 0, buff1.length);
		System.arraycopy(buff, buff1.length, buff2, 0, buff2.length);
		
		List<Message> msgs = decoder.decode( buff1 );								
		msgs = decoder.decode( buff2  );								
	
		for(Message m: msgs) {
			System.out.println( m.fixedHeader().messageType() + ", " + ((m.payload() != null && m.payload() instanceof byte[] ) ? new String((byte[])m.payload()): m.payload()) );
		}
		//
		System.out.println(msgs.size());

	}
	
	//
	// Factory methods of different MQTT
	// Message types to help testing
	//
	private static Message createMessageWithFixedHeader(MessageType messageType) {
		return new Message(new FixedHeader(messageType, false, MqttQoS.AT_MOST_ONCE, false, 0));
	}

	private static Message createMessageWithFixedHeaderAndMessageIdVariableHeader(MessageType messageType) {
		FixedHeader mqttFixedHeader = new FixedHeader(messageType, false,
				messageType == MessageType.PUBREL ? MqttQoS.AT_LEAST_ONCE : MqttQoS.AT_MOST_ONCE, false, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		return new Message(mqttFixedHeader, mqttMessageIdVariableHeader);
	}

	private static MqttConnectMessage createConnectMessage(MqttVersion mqttVersion) {
		return createConnectMessage(mqttVersion, USER_NAME, PASSWORD);
	}

	private static MqttConnectMessage createConnectMessage(MqttVersion mqttVersion, String username, String password) {
		
		
		 FixedHeader mqttFixedHeader =
                 new FixedHeader(MessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
         MqttConnectVariableHeader mqttConnectVariableHeader =
                 new MqttConnectVariableHeader(
                		 mqttVersion.protocolName(),
                		 mqttVersion.protocolLevel(),
                         true,
                         true,
                         true,
                         MqttQoS.AT_LEAST_ONCE.value(),
                         true,
                         true,
                         KEEP_ALIVE_SECONDS);
         MqttConnectPayload mqttConnectPayload =
                 new MqttConnectPayload(CLIENT_ID, WILL_TOPIC, WILL_MESSAGE.getBytes(), username, password.getBytes());
         return new MqttConnectMessage(mqttFixedHeader, mqttConnectVariableHeader, mqttConnectPayload);
	}

	private static MqttConnAckMessage createConnAckMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
				false, 0);
		MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(
				MqttConnectReturnCode.CONNECTION_ACCEPTED, true);
		return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
	}

	private static MqttPublishMessage createPublishMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader("/abc", 1234);
		byte[] payload = "whatever".getBytes(StandardCharsets.UTF_8);
		return new MqttPublishMessage(mqttFixedHeader, mqttPublishVariableHeader, payload);
	}

	private static MqttSubscribeMessage createSubscribeMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		//
		List<MqttTopicSubscription> topicSubscriptions = new LinkedList<MqttTopicSubscription>();
		topicSubscriptions.add(new MqttTopicSubscription("/abc", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new MqttTopicSubscription("/def", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new MqttTopicSubscription("/xyz", MqttQoS.EXACTLY_ONCE));

		MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(topicSubscriptions);
		return new MqttSubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
	}

	private static MqttSubAckMessage createSubAckMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(1, 2, 0);
		return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
	}

	private static MqttUnsubscribeMessage createUnsubscribeMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		//
		List<String> topics = new LinkedList<String>();
		topics.add("/abc");
		topics.add("/def");
		topics.add("/xyz");

		MqttUnsubscribePayload mqttUnsubscribePayload = new MqttUnsubscribePayload(topics);
		return new MqttUnsubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttUnsubscribePayload);
	}
}