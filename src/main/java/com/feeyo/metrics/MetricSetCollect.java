package com.feeyo.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MetricSetCollect implements MetricSet {

    protected static long DEFAULT_DATA_TTL = 5000;

    // The time (in milli-seconds) to live of cached data
    protected long dataTTL;

    // The last collect time
    protected AtomicLong lastCollectTime;

    // The lock used to collect metric
    private final Object collectLock = new Object();

    public MetricSetCollect() {
       this(DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS);
    }

    public MetricSetCollect(long dataTTL, TimeUnit unit) {
        this.dataTTL = unit.toMillis(dataTTL);
        this.lastCollectTime = new AtomicLong( System.currentTimeMillis() );
    }

    /**
     * Do not collect data if our cached copy of data is valid.
     * The purpose is to minimize the cost to collect system metric.
     */
    public void refreshIfNecessary() {
    	
    	long nowTime = System.currentTimeMillis();
    	
        if ( nowTime - lastCollectTime.get() > dataTTL) {
            synchronized (collectLock) {
                // double check, in case other thread has already entered.
                if ( nowTime - lastCollectTime.get() > dataTTL) {
                    collect();
                    // update the last collect time stamp
                    lastCollectTime.set( nowTime );
                }
            }
        }
    }

    protected abstract void collect();
}
