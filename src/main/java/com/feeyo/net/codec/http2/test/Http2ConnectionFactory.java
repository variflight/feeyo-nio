package com.feeyo.net.codec.http2.test;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.feeyo.net.nio.ClosableConnection;
import com.feeyo.net.nio.ConnectionFactory;
import com.feeyo.net.nio.NetSystem;

public class Http2ConnectionFactory extends ConnectionFactory {
	
	@Override
	public ClosableConnection make(SocketChannel channel) throws IOException {
		
		Http2Connection c = new Http2Connection(channel);
		NetSystem.getInstance().setSocketParams(c);	// 设置连接的参数
        c.setHandler( new Http2ConnectionHandler() );	// 设置NIOHandler
        c.setIdleTimeout( NetSystem.getInstance().getNetConfig().getIdleTimeout() );
		return c;
	}

}