package com.feeyo.net.nio;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 连接ID生成器
 */
public class ConnectIdGenerator {

	private static ConnectIdGenerator instance = new ConnectIdGenerator();

	public static ConnectIdGenerator getINSTNCE() {
		return instance;
	}

	private AtomicLong connectId = new AtomicLong();

	public long getId() {
		return connectId.incrementAndGet();
	}
}
