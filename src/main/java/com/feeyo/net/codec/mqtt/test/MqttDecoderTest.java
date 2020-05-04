package com.feeyo.net.codec.mqtt.test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.mqtt.ConnAckMessage;
import com.feeyo.net.codec.mqtt.ConnAckVariableHeader;
import com.feeyo.net.codec.mqtt.ConnectMessage;
import com.feeyo.net.codec.mqtt.ConnectPayload;
import com.feeyo.net.codec.mqtt.ConnectReturnCode;
import com.feeyo.net.codec.mqtt.ConnectVariableHeader;
import com.feeyo.net.codec.mqtt.MqttDecoder;
import com.feeyo.net.codec.mqtt.MqttEncoder;
import com.feeyo.net.codec.mqtt.FixedHeader;
import com.feeyo.net.codec.mqtt.Message;
import com.feeyo.net.codec.mqtt.MessageIdVariableHeader;
import com.feeyo.net.codec.mqtt.MessageType;
import com.feeyo.net.codec.mqtt.PublishMessage;
import com.feeyo.net.codec.mqtt.PublishVariableHeader;
import com.feeyo.net.codec.mqtt.MqttQoS;
import com.feeyo.net.codec.mqtt.SubAckMessage;
import com.feeyo.net.codec.mqtt.SubAckPayload;
import com.feeyo.net.codec.mqtt.SubscribeMessage;
import com.feeyo.net.codec.mqtt.SubscribePayload;
import com.feeyo.net.codec.mqtt.TopicSubscription;
import com.feeyo.net.codec.mqtt.UnsubscribeMessage;
import com.feeyo.net.codec.mqtt.UnsubscribePayload;
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

	private static ConnectMessage createConnectMessage(MqttVersion mqttVersion) {
		return createConnectMessage(mqttVersion, USER_NAME, PASSWORD);
	}

	private static ConnectMessage createConnectMessage(MqttVersion mqttVersion, String username, String password) {
		
		
		 FixedHeader mqttFixedHeader =
                 new FixedHeader(MessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
         ConnectVariableHeader mqttConnectVariableHeader =
                 new ConnectVariableHeader(
                		 mqttVersion.protocolName(),
                		 mqttVersion.protocolLevel(),
                         true,
                         true,
                         true,
                         MqttQoS.AT_LEAST_ONCE.value(),
                         true,
                         true,
                         KEEP_ALIVE_SECONDS);
         ConnectPayload mqttConnectPayload =
                 new ConnectPayload(CLIENT_ID, WILL_TOPIC, WILL_MESSAGE.getBytes(), username, password.getBytes());
         return new ConnectMessage(mqttFixedHeader, mqttConnectVariableHeader, mqttConnectPayload);
	}

	private static ConnAckMessage createConnAckMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
				false, 0);
		ConnAckVariableHeader mqttConnAckVariableHeader = new ConnAckVariableHeader(
				ConnectReturnCode.CONNECTION_ACCEPTED, true);
		return new ConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
	}

	private static PublishMessage createPublishMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		PublishVariableHeader mqttPublishVariableHeader = new PublishVariableHeader("/abc", 1234);
		byte[] payload = "whatever".getBytes(StandardCharsets.UTF_8);
		return new PublishMessage(mqttFixedHeader, mqttPublishVariableHeader, payload);
	}

	private static SubscribeMessage createSubscribeMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		//
		List<TopicSubscription> topicSubscriptions = new LinkedList<TopicSubscription>();
		topicSubscriptions.add(new TopicSubscription("/abc", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new TopicSubscription("/def", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new TopicSubscription("/xyz", MqttQoS.EXACTLY_ONCE));

		SubscribePayload mqttSubscribePayload = new SubscribePayload(topicSubscriptions);
		return new SubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
	}

	private static SubAckMessage createSubAckMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		SubAckPayload mqttSubAckPayload = new SubAckPayload(1, 2, 0);
		return new SubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
	}

	private static UnsubscribeMessage createUnsubscribeMessage() {
		FixedHeader mqttFixedHeader = new FixedHeader(MessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MessageIdVariableHeader mqttMessageIdVariableHeader = MessageIdVariableHeader.from(12345);
		//
		List<String> topics = new LinkedList<String>();
		topics.add("/abc");
		topics.add("/def");
		topics.add("/xyz");

		UnsubscribePayload mqttUnsubscribePayload = new UnsubscribePayload(topics);
		return new UnsubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttUnsubscribePayload);
	}
}