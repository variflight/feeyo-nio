package com.feeyo.net.nio;

/**
 * 网络基础配置项
 */
public final class NetConfig {

	private int idleTimeout    = 5 * 60 * 1000;	// 单位毫秒
	
	private final int socketSoRcvbuf;
	private final int socketSoSndbuf;
	
	private int socketNoDelay = 1; 				// 0=false
	
	public NetConfig(int socketSoRcvbuf, int socketSoSndbuf) {
		this.socketSoRcvbuf = socketSoRcvbuf;
		this.socketSoSndbuf = socketSoSndbuf;
	}


	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public int getSocketSoRcvbuf() {
		return socketSoRcvbuf;
	}

	public int getSocketSoSndbuf() {
		return socketSoSndbuf;
	}

	public int getSocketNoDelay() {
		return socketNoDelay;
	}

	public void setSocketNoDelay(int socketNoDelay) {
		this.socketNoDelay = socketNoDelay;
	}
}