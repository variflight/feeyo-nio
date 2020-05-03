package com.feeyo.net.codec.mqtt.test;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.feeyo.net.nio.ClosableConnection;
import com.feeyo.net.nio.ConnectionFactory;
import com.feeyo.net.nio.NetSystem;

public class MqttConnectionFactory extends ConnectionFactory {

	@Override
	public ClosableConnection make(SocketChannel channel) throws IOException {
		MqttConnection c = new MqttConnection(channel);
		NetSystem.getInstance().setSocketParams(c);		// 设置连接的参数
        c.setHandler( new MqttConnectionHandler() );	// 设置NIOHandler
        c.setIdleTimeout( NetSystem.getInstance().getNetConfig().getIdleTimeout() );
		return c;
	}

}
