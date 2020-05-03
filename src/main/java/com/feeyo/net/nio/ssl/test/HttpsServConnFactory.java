package com.feeyo.net.nio.ssl.test;

import com.feeyo.net.nio.ClosableConnection;
import com.feeyo.net.nio.ConnectionFactory;
import com.feeyo.net.nio.NetSystem;
import com.feeyo.net.nio.ssl.SslContextUtil;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class HttpsServConnFactory extends ConnectionFactory {
	
	@Override
	public ClosableConnection make(SocketChannel channel) throws IOException {
		HttpsServConnHandler httpsHandler = new HttpsServConnHandler();
		HttpsServConn c = new HttpsServConn(channel, SslContextUtil.getSslContext1());
		NetSystem.getInstance().setSocketParams(c);	// 设置连接的参数
        c.setHandler( httpsHandler );				// 设置NIOHandler
        c.setIdleTimeout( NetSystem.getInstance().getNetConfig().getIdleTimeout() );
		return c;
	}

}
