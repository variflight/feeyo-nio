package com.feeyo.net.codec.mqtt.test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.mqtt.MqttConnAckMessage;
import com.feeyo.net.codec.mqtt.MqttConnectMessage;
import com.feeyo.net.codec.mqtt.MqttConnectReturnCode;
import com.feeyo.net.codec.mqtt.MqttDecoder;
import com.feeyo.net.codec.mqtt.MqttEncoder;
import com.feeyo.net.codec.mqtt.MqttFixedHeader;
import com.feeyo.net.codec.mqtt.MqttMessage;
import com.feeyo.net.codec.mqtt.MqttMessageBuilders;
import com.feeyo.net.codec.mqtt.MqttMessageIdVariableHeader;
import com.feeyo.net.codec.mqtt.MqttMessageType;
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

		MqttMessage message = createConnectMessage(MqttVersion.MQTT_3_1);
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
		
		List<MqttMessage> msgs = decoder.decode( buff1 );								
		msgs = decoder.decode( buff2  );								
	
		for(MqttMessage m: msgs) {
			System.out.println( m.fixedHeader().messageType() + ", " + ((m.payload() != null && m.payload() instanceof byte[] ) ? new String((byte[])m.payload()): m.payload()) );
		}
		//
		System.out.println(msgs.size());

	}
	
	//
	// Factory methods of different MQTT
	// Message types to help testing
	//
	private static MqttMessage createMessageWithFixedHeader(MqttMessageType messageType) {
		return new MqttMessage(new MqttFixedHeader(messageType, false, MqttQoS.AT_MOST_ONCE, false, 0));
	}

	private static MqttMessage createMessageWithFixedHeaderAndMessageIdVariableHeader(MqttMessageType messageType) {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(messageType, false,
				messageType == MqttMessageType.PUBREL ? MqttQoS.AT_LEAST_ONCE : MqttQoS.AT_MOST_ONCE, false, 0);
		MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);
		return new MqttMessage(mqttFixedHeader, mqttMessageIdVariableHeader);
	}

	private static MqttConnectMessage createConnectMessage(MqttVersion mqttVersion) {
		return createConnectMessage(mqttVersion, USER_NAME, PASSWORD);
	}

	private static MqttConnectMessage createConnectMessage(MqttVersion mqttVersion, String username, String password) {
		return MqttMessageBuilders.connect().clientId(CLIENT_ID).protocolVersion(mqttVersion).username(username)
				.password(password.getBytes()).willRetain(true).willQoS(MqttQoS.AT_LEAST_ONCE).willFlag(true)
				.willTopic(WILL_TOPIC).willMessage(WILL_MESSAGE.getBytes()).cleanSession(true)
				.keepAlive(KEEP_ALIVE_SECONDS).build();
	}

	private static MqttConnAckMessage createConnAckMessage() {
		return MqttMessageBuilders.connAck().returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED).sessionPresent(true)
				.build();
	}

	private static MqttPublishMessage createPublishMessage() {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MqttPublishVariableHeader mqttPublishVariableHeader = new MqttPublishVariableHeader("/abc", 1234);
		byte[] payload = "whatever".getBytes(StandardCharsets.UTF_8);
		return new MqttPublishMessage(mqttFixedHeader, mqttPublishVariableHeader, payload);
	}

	private static MqttSubscribeMessage createSubscribeMessage() {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);
		//
		List<MqttTopicSubscription> topicSubscriptions = new LinkedList<MqttTopicSubscription>();
		topicSubscriptions.add(new MqttTopicSubscription("/abc", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new MqttTopicSubscription("/def", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new MqttTopicSubscription("/xyz", MqttQoS.EXACTLY_ONCE));

		MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(topicSubscriptions);
		return new MqttSubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
	}

	private static MqttSubAckMessage createSubAckMessage() {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);
		MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(1, 2, 0);
		return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
	}

	private static MqttUnsubscribeMessage createUnsubscribeMessage() {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, true, 0);
		MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);
		//
		List<String> topics = new LinkedList<String>();
		topics.add("/abc");
		topics.add("/def");
		topics.add("/xyz");

		MqttUnsubscribePayload mqttUnsubscribePayload = new MqttUnsubscribePayload(topics);
		return new MqttUnsubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttUnsubscribePayload);
	}
}