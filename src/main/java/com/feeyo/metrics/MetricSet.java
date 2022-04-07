package com.feeyo.metrics;

import java.util.Map;

public interface MetricSet extends Metric {
	
    /**
     * A map of metric names to metrics.
     */
	 Map<String, Metric> getMetrics();
}
