package com.feeyo.net.nio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feeyo.net.nio.buffer.BufferPool;


/**
 * 存放当前所有连接的信息，包括客户端和服务端等，以及Network部分所使用共用对象
 *
 * @author wuzhih
 * @author zhuam
 *
 */
public class NetSystem {
	
	private static Logger LOGGER = LoggerFactory.getLogger( NetSystem.class );

	private static NetSystem INSTANCE;
	
	//
	private final BufferPool bufferPool;
	
	// 用来执行那些耗时的任务
	private final ThreadPoolExecutor businessExecutor;
	
	// 用来执行定时任务
	private final NameableExecutor timerExecutor;
	private final ScheduledExecutorService timerSchedExecutor;
	
	private final ConcurrentHashMap<Long, ClosableConnection> allConnections = new ConcurrentHashMap<Long, ClosableConnection>();
	private NetConfig netConfig;

	public static NetSystem getInstance() {
		return INSTANCE;
	}

	public NetSystem(BufferPool bufferPool, 
			ThreadPoolExecutor businessExecutor, final NameableExecutor timerExecutor, ScheduledExecutorService timerSchedExecutor)
			throws IOException {
		//
		this.bufferPool = bufferPool;
		this.businessExecutor = businessExecutor;
		this.timerExecutor = timerExecutor;
		this.timerSchedExecutor = timerSchedExecutor;
		//
		INSTANCE = this;
		//
		// IDLE 连接检查, 关闭
		timerSchedExecutor.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {		
				//
				timerExecutor.execute(new Runnable() {
					@Override
					public void run() {
						checkConnections();
					}
				});
			}			
		}, 5 * 1000L, 1000L, TimeUnit.MILLISECONDS);	
	}

	//
	// Uses the shutdown pattern from http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
	public void shutdown() {
		try {
			// Disable new tasks from being submitted
			if ( timerSchedExecutor != null ) {
				timerSchedExecutor.shutdown(); 
				this.shutdown( timerSchedExecutor );
			}
			
			if ( timerExecutor != null ) {
				timerExecutor.shutdown();
				this.shutdown( timerExecutor );
			}
			
			if ( businessExecutor != null ) {
				businessExecutor.shutdown();
				this.shutdown( businessExecutor );
			}
			//
		} catch(Throwable e) {
			// ignored
		}
	}
	
	private void shutdown(ExecutorService executor) {
		try {
			// Wait a while for existing tasks to terminate
			if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
				executor.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
					LOGGER.warn(getClass().getSimpleName() + ": ExecutorService did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
	
	public BufferPool getBufferPool() {
		return bufferPool;
	}

	public NetConfig getNetConfig() {
		return netConfig;
	}

	public void setNetConfig(NetConfig netConfig) {
		this.netConfig = netConfig;
	}

	public ThreadPoolExecutor getBusinessExecutor() {
		return businessExecutor;
	}

	public NameableExecutor getTimerExecutor() {
		return timerExecutor;
	}
	
	public ScheduledExecutorService getTimerSchedExecutor() {
		return timerSchedExecutor;
	}

	/**
	 * 添加一个连接到系统中被监控
	 */
	public void addConnection(ClosableConnection c) {
		allConnections.put(c.getId(), c);
	}
	
	public void removeConnection(ClosableConnection c) {
		this.allConnections.remove( c.getId() );
	}

	public ConcurrentMap<Long, ClosableConnection> getAllConnectios() {
		return allConnections;
	}
	
	private AtomicBoolean checking = new AtomicBoolean(false);
	
	/**
	 * 定时执行该方法，回收部分资源。
	 */
	private void checkConnections() {
		
		if ( !checking.compareAndSet(false, true))
			return;
		
		try {
			//
			Iterator<Entry<Long, ClosableConnection>> it = allConnections.entrySet().iterator();
			while (it.hasNext()) {
				ClosableConnection c = it.next().getValue();
				// 删除空连接
				if (c == null) {
					it.remove();
					continue;
				}
				
				// 清理已关闭连接，否则空闲检查。
				if (c.isClosed()) {
					it.remove();
				} else {
					// very important ,for some data maybe not sent
					if ( c.isConnected() ) {
						c.doNextWriteCheck();
					}
					// idle check
					c.idleCheck();
				}
			}
			
		} finally {
			checking.set(false);
		}
	}
	
	public void setSocketParams(ClosableConnection con) throws IOException {
	    int sorcvbuf = this.netConfig.getSocketSoRcvbuf();
		int sosndbuf = this.netConfig.getSocketSoSndbuf();
		//int soNoDelay = this.netConfig.getSocketNoDelay();
		
		sorcvbuf = 4194304;
		sosndbuf = 4194304;
		
		SocketChannel socketChannel = con.getSocketChannel();
		socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, sorcvbuf);
		socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, sosndbuf);
		socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true); //soNoDelay == 1
	    socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
	    socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
	}
	
}