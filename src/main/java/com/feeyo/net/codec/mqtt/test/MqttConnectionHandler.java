package com.feeyo.net.codec.mqtt.test;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.mqtt.ConnAckMessage;
import com.feeyo.net.codec.mqtt.ConnAckVariableHeader;
import com.feeyo.net.codec.mqtt.ConnectMessage;
import com.feeyo.net.codec.mqtt.ConnectPayload;
import com.feeyo.net.codec.mqtt.ConnectReturnCode;
import com.feeyo.net.codec.mqtt.MqttDecoder;
import com.feeyo.net.codec.mqtt.FixedHeader;
import com.feeyo.net.codec.mqtt.Message;
import com.feeyo.net.codec.mqtt.MessageIdVariableHeader;
import com.feeyo.net.codec.mqtt.MessageType;
import com.feeyo.net.codec.mqtt.PublishMessage;
import com.feeyo.net.codec.mqtt.MqttQoS;
import com.feeyo.net.codec.mqtt.SubscribeMessage;
import com.feeyo.net.codec.mqtt.UnsubscribeMessage;
import com.feeyo.net.codec.mqtt.MqttVersion;
import com.feeyo.net.nio.NIOHandler;

import static com.feeyo.net.codec.mqtt.ConnectReturnCode.*;
import static com.feeyo.net.codec.mqtt.MessageIdVariableHeader.from;
import static com.feeyo.net.codec.mqtt.MqttQoS.*;

public class MqttConnectionHandler implements NIOHandler<MqttConnection> {

	//
	private static final Logger LOGGER = LoggerFactory.getLogger(MqttConnectionHandler.class);

	//
	private MqttDecoder decoder = new MqttDecoder();

	@Override
	public void onConnected(MqttConnection con) throws IOException {
		// ignored
	}

	@Override
	public void onConnectFailed(MqttConnection con, Exception e) {
		// ignored
	}

	@Override
	public void onClosed(MqttConnection con, String reason) {
		// ignored
	}

	@Override
	public void handleReadEvent(MqttConnection con, byte[] data) throws IOException {
		try {
			List<Message> msgs = decoder.decode(data);
			for(Message msg: msgs) 
				handleMQTTMessage(con, msg);
			//
		} catch (UnknownProtocolException e) {
			LOGGER.warn("Unknow MQTT protocol, con: {}", con);
			con.close("Unknown MQTT protocol");
		}
	}

	//
	private void handleMQTTMessage(MqttConnection con, Message msg) {

		MessageType messageType = msg.fixedHeader().messageType();
		LOGGER.debug("Received MQTT message, type: {}, Conn: {}", messageType, con);

		switch (messageType) {
		case CONNECT:
			processConnect(con, (ConnectMessage) msg);
			break;
		case SUBSCRIBE:
			processSubscribe(con, (SubscribeMessage) msg);
			break;
		case UNSUBSCRIBE:
			processUnsubscribe(con, (UnsubscribeMessage) msg);
			break;
		case PUBLISH:
			processPublish(con, (PublishMessage) msg);
			break;
		case PUBREC:
			processPubRec(con, msg);
			break;
		case PUBCOMP:
			processPubComp(con, msg);
			break;
		case PUBREL:
			processPubRel(con, msg);
			break;
		case DISCONNECT:
			processDisconnect(con, msg);
			break;
		case PUBACK:
			processPubAck(con, msg);
			break;
		case PINGREQ:
			FixedHeader pingHeader = new FixedHeader(MessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
			Message pingResp = new Message(pingHeader);
			// send pingResp & close Connection
			//
			break;
		default:
			LOGGER.error("Unknown MessageType: {}, Conn: {}", messageType, con);
			break;
		}
	}

	private void processConnect(MqttConnection connection, ConnectMessage msg) {
		//
		ConnectPayload payload = msg.payload();
		String clientId = payload.clientIdentifier();
		final String username = payload.userName();
		
		if (isNotProtocolVersion(msg, MqttVersion.MQTT_3_1) && isNotProtocolVersion(msg, MqttVersion.MQTT_3_1_1)) {
            LOGGER.warn("MQTT protocol version is not valid. CId={} con: {}", clientId, connection);
            abortConnection(connection, CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
            return;
        }
		
		final boolean cleanSession = msg.variableHeader().isCleanSession();
		if (clientId == null || clientId.length() == 0) {
			///
		}
		
		// handle user authentication
		if (msg.variableHeader().hasUserName()) {
			byte[] pwd = null;
            if (msg.variableHeader().hasPassword()) {
                pwd = msg.payload().passwordInBytes();
            } 
		}
		
	}
	
	private void processSubscribe(MqttConnection connection, SubscribeMessage msg) {
		//
	}
	
	private void processUnsubscribe(MqttConnection connection, UnsubscribeMessage msg) {
		List<String> topics = msg.payload().topics();
	}
	
	private  void processPublish(MqttConnection connection, PublishMessage msg) {
        final MqttQoS qos = msg.fixedHeader().qosLevel();
	}
	
	private void processPubRec(MqttConnection connection, Message msg) {
        final int messageId = ((MessageIdVariableHeader) msg.variableHeader()).messageId();
	}
	
	private void processPubComp(MqttConnection connection, Message msg) {
        final int messageId = ((MessageIdVariableHeader) msg.variableHeader()).messageId();
	}
	
	private void processPubRel(MqttConnection connection, Message msg) {
		final int messageId = ((MessageIdVariableHeader) msg.variableHeader()).messageId();
	}
	
	private void processDisconnect(MqttConnection connection, Message msg) {
		connection.close("received DISCONNECT!");
	}
	
	private void processPubAck(MqttConnection connection, Message msg) {
		final int messageId = ((MessageIdVariableHeader) msg.variableHeader()).messageId();
	}
	
	private boolean isNotProtocolVersion(ConnectMessage msg, MqttVersion version) {
		return msg.variableHeader().version() != version.protocolLevel();
	}
	
	private void abortConnection(MqttConnection connection, ConnectReturnCode returnCode) {
        ConnAckMessage badProto = connAck(returnCode, false);
        connection.sendAndClose( badProto );
    }

    private ConnAckMessage connAck(ConnectReturnCode returnCode, boolean sessionPresent) {;
        return new ConnAckMessage( //
        		new FixedHeader(MessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,  false, 0),  //
        		new ConnAckVariableHeader(returnCode, sessionPresent));
    }

}
