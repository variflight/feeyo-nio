package com.feeyo.net.codec.http2.test;

import java.io.IOException;

import com.feeyo.net.nio.NIOAcceptor;
import com.feeyo.net.nio.NIOReactorPool;
import com.feeyo.net.nio.NetSystem;
import com.feeyo.net.nio.NetConfig;
import com.feeyo.buffer.BufferPool;
import com.feeyo.buffer.bucket.BucketBufferPool;

import com.feeyo.net.nio.util.ExecutorUtil;

// @see https://simonecarletti.com/blog/2016/01/http2-curl-macosx/
//
// @see https://jiaolonghuang.github.io/2015/08/17/http2vshttp1.1/
//
public class Http2Server {

	public static void main(String[] args) throws IOException {

		//
		BufferPool bufferPool = new BucketBufferPool(1024 * 1024 * 40, 1024 * 1024 * 80, 
				new int[] { 1024 });

		//
		new NetSystem(bufferPool, ExecutorUtil.create("BusinessExecutor-", 2),
				ExecutorUtil.create("TimerExecutor-", 2),
				ExecutorUtil.createScheduled("TimerSchedExecutor-", 1));

		NetConfig systemConfig = new NetConfig(1048576, 4194304);
		NetSystem.getInstance().setNetConfig(systemConfig);

		NIOReactorPool reactorPool = new NIOReactorPool("nio", 1);
		NIOAcceptor acceptor = new NIOAcceptor("http2", "0.0.0.0", 8066, new Http2ConnectionFactory(), reactorPool);
		acceptor.start();
	}

}
