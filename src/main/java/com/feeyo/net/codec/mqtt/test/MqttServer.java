package com.feeyo.net.codec.mqtt.test;

import java.io.IOException;

import com.feeyo.net.nio.NIOAcceptor;
import com.feeyo.net.nio.NIOReactorPool;
import com.feeyo.net.nio.NetConfig;
import com.feeyo.net.nio.NetSystem;
import com.feeyo.buffer.BufferPool;
import com.feeyo.buffer.bucket.BucketBufferPool;
import com.feeyo.net.nio.util.ExecutorUtil;

public class MqttServer {
	//
	public static final int DEFAULT_PORT = 1883; // Default port is 1883 for MQTT
	public static final int DEFAULT_TLS_PORT = 8883; // Default TLS port is 8883 for MQTT
	
	//
	// An MQTT server
	//
	public static void main(String[] args) throws IOException {
		//
		//
		BufferPool bufferPool = new BucketBufferPool(1024 * 1024 * 40, 1024 * 1024 * 80, new int[] { 1024 });

		//
		new NetSystem(bufferPool, ExecutorUtil.create("BusinessExecutor-", 2),
				ExecutorUtil.create("TimerExecutor-", 2),
				ExecutorUtil.createScheduled("TimerSchedExecutor-", 1));

		NetConfig systemConfig = new NetConfig(1048576, 4194304);
		NetSystem.getInstance().setNetConfig(systemConfig);

		NIOReactorPool reactorPool = new NIOReactorPool("nio", 1);
		NIOAcceptor acceptor = new NIOAcceptor("mqtt", "0.0.0.0", DEFAULT_PORT, new MqttConnectionFactory(), reactorPool);
		acceptor.start();
		
	}

}
