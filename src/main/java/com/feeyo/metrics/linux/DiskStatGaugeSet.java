package com.feeyo.metrics.linux;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.feeyo.metrics.MetricSetCollect;
import com.feeyo.metrics.Gauge;
import com.feeyo.metrics.Metric;
import com.feeyo.metrics.RatioGauge;


/**
 * Collect disk related statistics
 */
public class DiskStatGaugeSet extends MetricSetCollect {

    private Map<String, Metric> gauges;

    private File file;
    private long totalSpace;
    private long freeSpace;

    public DiskStatGaugeSet() {
        this(DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS,  new File("/"));
    }

    public DiskStatGaugeSet(long dataTTL, TimeUnit unit) {
        this(dataTTL, unit, new File("/"));
    }

    public DiskStatGaugeSet(long dataTTL, TimeUnit unit, File file) {
        super(dataTTL, unit);
        //
        this.gauges = new HashMap<String, Metric>();
        
        // keep the partitions file array immutable
        this.file = file;
        this.totalSpace = 0;
        this.freeSpace = 0;
        
        populateGauges();
    }

    /**
     * According to the documentation, getUsableSpace() is more accurate than getFreeSpace()
     */
    @Override
    protected void collect() {
    	totalSpace = file.getTotalSpace();
    	freeSpace = file.getUsableSpace();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return gauges;
    }

    private void populateGauges() {
         gauges.put("disk.partition.total", new PartitionTotalGauge());
         gauges.put("disk.partition.free", new PartitionFreeGauge());
         gauges.put("disk.partition.used_ratio", new PartitionUsageGauge());
    }

    private class PartitionTotalGauge implements Gauge<Long> {

        @Override
        public Long getValue() {
            try {
                refreshIfNecessary();
                return totalSpace;
            } catch (Exception e) {
                return 0L;
            }
        }
    }

    private class PartitionFreeGauge implements Gauge<Long> {
        @Override
        public Long getValue() {
            try {
                refreshIfNecessary();
                return freeSpace;
            } catch (Exception e) {
                return 0L;
            }
        }
    }

    private class PartitionUsageGauge extends RatioGauge {
        @Override
        protected Ratio getRatio() {
            return Ratio.of((double)(totalSpace- freeSpace), (double)totalSpace);
        }
    }
}
