package com.feeyo.metrics;

import com.feeyo.metrics.util.LongAdder;

/**
 * The abstraction of a bucket for collecting statistics
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