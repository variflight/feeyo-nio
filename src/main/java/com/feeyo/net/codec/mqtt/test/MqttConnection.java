package com.feeyo.net.codec.mqtt.test;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.mqtt.MqttEncoder;
import com.feeyo.net.codec.mqtt.MqttMessage;
import com.feeyo.net.nio.Connection;

public class MqttConnection extends Connection {

	public MqttConnection(SocketChannel socketChannel) {
		super(socketChannel);
	}
	
	//
	public void send(MqttMessage message) {
		try {
			ByteBuffer data = MqttEncoder.INSTANCE.encode(message);
			this.write(data);
		} catch (UnknownProtocolException e) {
			// ignored
		}
	}
	
	public void sendAndClose(MqttMessage message)  {
		this.send(message);
		this.close("");
	}

}
