package com.feeyo.net.nio.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.feeyo.net.nio.NameableExecutor;
import com.feeyo.net.nio.NameableThreadFactory;

/**
 * 生成一个有名字的（Nameable）Executor，容易进行跟踪和监控
 * 
 * @author wuzh
 */
public class ExecutorUtil {

	public static final NameableExecutor create(String name, int size) {
        return create(name, size, true);
    }

    public static final NameableExecutor create(String name, int size, boolean isDaemon) {
        NameableThreadFactory factory = new NameableThreadFactory(name, isDaemon);
        return new NameableExecutor(name, size, new LinkedTransferQueue<Runnable>(), factory);
    }
    
    public static final NameableExecutor create(String name, int corePoolSize, int maximumPoolSize, int keepalive, 
    		TimeUnit unit, BlockingQueue<Runnable> queue, boolean isDaemon) {
        NameableThreadFactory factory = new NameableThreadFactory(name, isDaemon);
        return new NameableExecutor(name, corePoolSize, maximumPoolSize, keepalive, unit, queue, factory);
    }
    
    public static final ScheduledExecutorService createScheduled(String name, int corePoolSize) {
    	return Executors.newScheduledThreadPool(corePoolSize, new NameableThreadFactory(name, true));
    }
}