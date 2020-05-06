package com.feeyo.net.nio.ssl.test;

import java.io.IOException;

import com.feeyo.net.nio.NIOAcceptor;
import com.feeyo.net.nio.NIOReactorPool;
import com.feeyo.net.nio.NetConfig;
import com.feeyo.net.nio.NetSystem;
import com.feeyo.buffer.BufferPool;
import com.feeyo.buffer.bucket.BucketBufferPool;
import com.feeyo.net.nio.util.ExecutorUtil;

public class HttpsServStartup {

	public static void main(String[] args) throws IOException {
		
		//
		BufferPool bufferPool = new BucketBufferPool(1024 * 1024 * 40, 1024 * 1024 * 80, new int[] { 1024 });

		//
		new NetSystem(bufferPool, ExecutorUtil.create("BusinessExecutor-", 2),
				ExecutorUtil.create("TimerExecutor-", 2),
				ExecutorUtil.createScheduled("TimerSchedExecutor-", 1));

		NetConfig systemConfig = new NetConfig(1048576, 4194304);
		NetSystem.getInstance().setNetConfig(systemConfig);
		
		//
		NIOReactorPool nioReactorPool = new NIOReactorPool("reactor", 2);
		NIOAcceptor nioAcceptor = new NIOAcceptor("rpc", "0.0.0.0", 433, new HttpsServConnFactory(), nioReactorPool);
		nioAcceptor.start();

	}

}
