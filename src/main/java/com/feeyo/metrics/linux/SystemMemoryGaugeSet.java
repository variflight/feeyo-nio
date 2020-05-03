package com.feeyo.metrics.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feeyo.metrics.MetricSetCollect;
import com.feeyo.metrics.Gauge;
import com.feeyo.metrics.Metric;
import com.feeyo.metrics.RatioGauge;
import com.feeyo.metrics.linux.util.FileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A gauge set that is collecting system memory metrics
 */
public class SystemMemoryGaugeSet extends MetricSetCollect {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemMemoryGaugeSet.class);

    /**
     * Detail explanation for /proc/meminfo can be found here:
     *   https://access.redhat.com/solutions/406773
     */
    private static final String DEFAULT_FILE_PATH = "/proc/meminfo";

    private static final String SPACE_REGEX = "\\s+";

    /**
     * The equation: MemUsed = MemTotal - (MemFree + Buffers + Cached)
     */
    private static final String[] METRICS = {
            "mem.total",      // MemTotal
            "mem.used",       // MemTotal - (MemFree + Buffers + Cached)
            "mem.free",       // MemFree
            "mem.buffers",    // Buffers
            "mem.cached",     // Cached
            "mem.swap.total", // SwapTotal
            "mem.swap.used",  // SwapTotal - SwapFree
            "mem.swap.free",  // SwapFree
    };

    private String filePath;

    private long[] data;

    private Map<String, Metric> gauges;

    public SystemMemoryGaugeSet() {
        this(DEFAULT_FILE_PATH, DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS);
    }

    public SystemMemoryGaugeSet(String filePath) {
        this(filePath, DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS);
    }

    public SystemMemoryGaugeSet(long dataTTL, TimeUnit unit) {
        this(DEFAULT_FILE_PATH, dataTTL, unit);
    }

    public SystemMemoryGaugeSet(String filePath, long dataTTL, TimeUnit unit) {
        super(dataTTL, unit);
        this.filePath = filePath;
        this.data = new long[METRICS.length];
        this.gauges = new HashMap<String, Metric>();
        populateGauges();
    }

    @Override
    protected void collect() {
        try {
            List<String> lines = FileUtils.readFileAsStringArray(filePath);
            for (String line: lines) {
                if (line.startsWith("MemTotal:")) {
                    data[0] = Long.parseLong(line.split(SPACE_REGEX)[1]);
                } else if (line.startsWith("MemFree:")) {
                    data[2] = Long.parseLong(line.split(SPACE_REGEX)[1]);
                } else if (line.startsWith("Buffers:")) {
                    data[3] = Long.parseLong(line.split(SPACE_REGEX)[1]);
                } else if (line.startsWith("Cached:")) {
                    data[4] = Long.parseLong(line.split(SPACE_REGEX)[1]);
                } else if (line.startsWith("SwapTotal:")) {
                    data[5] = Long.parseLong(line.split(SPACE_REGEX)[1]);
                } else if (line.startsWith("SwapFree:")) {
                    data[7] = Long.parseLong(line.split(SPACE_REGEX)[1]);
                }
            }
            // calculate mem.used
            data[1] = data[0] - data[2] - data[3] - data[4];
            // calculate mem.swap.used
            data[6] = data[5] - data[7];
        } catch (IOException e) {
            LOGGER.warn("Error during reading file {}", filePath, e);
        }
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return gauges;
    }

    private void populateGauges() {
        for (int i = 0; i < METRICS.length; i++) {
            gauges.put(METRICS[i], new MemGauge(i));
        }

        final RatioGauge usedRatio = new RatioGauge() {
            @Override
            @SuppressWarnings("unchecked")
            protected Ratio getRatio() {
                Gauge<Long> memUsed = (Gauge<Long>)gauges.get("mem.used");
                Gauge<Long> memTotal = (Gauge<Long>)gauges.get("mem.total");
                return Ratio.of(memUsed.getValue().doubleValue(), memTotal.getValue().doubleValue());
            }
        };

        gauges.put("mem.used_ratio", usedRatio);
    }

    private class MemGauge implements Gauge<Long> {

        private int index;

        public MemGauge(int index) {
            this.index = index;
        }

        @Override
        public Long getValue() {
            try {
                refreshIfNecessary();
                return data[index];
            } catch (Exception e) {
                return 0L;
            }
        }
    }
}
