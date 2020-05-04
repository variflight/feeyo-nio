package com.feeyo.net.codec.mqtt.test;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.mqtt.MqttEncoder;
import com.feeyo.net.codec.mqtt.Message;
import com.feeyo.net.nio.Connection;

public class MqttConnection extends Connection {

	public MqttConnection(SocketChannel socketChannel) {
		super(socketChannel);
	}
	
	//
	public void send(Message message) {
		try {
			ByteBuffer data = MqttEncoder.INSTANCE.encode(message);
			this.write(data);
		} catch (UnknownProtocolException e) {
			// ignored
		}
	}
	
	public void sendAndClose(Message message)  {
		this.send(message);
		this.close("");
	}

}
