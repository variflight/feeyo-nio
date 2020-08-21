package com.feeyo.metrics;

import com.feeyo.metrics.util.LongAdder;

/**
 * 分桶计数功能，统计一定时间间隔内的计数
 */
class Bucket {

    /**
     * The timestamp of this bucket
     */
    long timestamp = -1L;

    /**
     * The counter for the bucket, can be updated concurrently
     */
     LongAdder count = new LongAdder();
}