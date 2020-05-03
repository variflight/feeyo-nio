package com.feeyo.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 吞吐率度量器
 * 
 * 一种用于度量一段时间内吞吐率的计量器。例如，最近 10s的 qps指标，
 * 这段时间内的吞吐率通过指数加权的方式计算移动平均得出。
 * 
 * @author zhuam
 *
 */
public class Meter implements Metric, Resetable {

	 // 保存最近N次精确计数对应的时间
    private final AtomicReference<Bucket> latestBucket;

    // 保存最近N次的精确计数, 采用环形队列避免数据的挪动
    private final BucketDeque buckets;
    
    // 每一次精确计数的之间的时间间隔，只能是1秒，5秒，10秒, 30秒, 60秒这几个数字
    private int interval;
    
	public Meter(int interval) {
		this.interval = interval;
		this.buckets = new BucketDeque( 11 );
		this.latestBucket = new AtomicReference<Bucket>(buckets.peek());
	}
    
	 /**
     * Mark the occurrence of an event.
     * 标记一次事件
     */
    public void mark() {
        mark(1);
    }
    
    /**
     * Mark the occurrence of a given number of events.
     * 标记n次事件
     * 
     * @param n the number of events
     */
    public void mark(long n) {
    	
		// align current timestamp
		long curTs = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) / interval * interval;
		Bucket oldBucket = latestBucket.get();
		if (curTs > latestBucket.get().timestamp) {
			// create a new bucket and evict the oldest one
			Bucket newBucket = new Bucket();
			newBucket.timestamp = curTs;
			if (latestBucket.compareAndSet(oldBucket, newBucket)) {
				// this is a single thread operation
				buckets.addLast(newBucket);
				oldBucket = newBucket;
			} else {
				oldBucket = latestBucket.get();
			}
		}
		// reduce the call to latestBucket.get() to avoid cache line invalidation
		// because internally latestBucket is a volatile object
		oldBucket.count.add(n);
    	
    }
    
    public Map<Long, Long> getQps() {
    	return getQps(0);
    }
    
    public Map<Long, Long> getQps(long startTime) {
    	
    	 Map<Long, Long> counts = new LinkedHashMap<Long, Long>();
         
         // calculateCurrentTimestamp
         long curTs = TimeUnit.MILLISECONDS.toSeconds( System.currentTimeMillis() ) / interval * interval;
         for (Bucket bucket: buckets.getBucketList()) {
             if (1000L * bucket.timestamp >= startTime && bucket.timestamp <= curTs) {
                 counts.put(1000L * bucket.timestamp, bucket.count.sum());
             }
         }
         return counts;
    }

    @Override
    public void reset() {
        // ignore
    }
}