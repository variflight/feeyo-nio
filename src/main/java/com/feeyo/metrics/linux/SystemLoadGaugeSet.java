package com.feeyo.metrics.linux;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.feeyo.metrics.MetricSetCollect;
import com.feeyo.metrics.linux.util.FileUtils;
import com.feeyo.metrics.linux.util.FormatUtils;
import com.feeyo.metrics.Gauge;
import com.feeyo.metrics.Metric;

public class SystemLoadGaugeSet extends MetricSetCollect {

    private static final String DEFAULT_FILE_PATH = "/proc/loadavg";

    // Pattern.DOTALL to match pattern across multiple lines
    private static final Pattern loadPattern =
            Pattern.compile("^([\\d\\.]+)\\s+([\\d\\.]+)\\s+([\\d\\.]+)\\s+[\\d]+/[\\d]+\\s+([\\d]+).*$",
                    Pattern.DOTALL);

    private enum LoadAvg {
        ONE_MIN, FIVE_MIN, FIFTEEN_MIN
    }

    // store the system load average, in the order of 1min, 5min, 15min
    private float[] loadAvg;

    private String filePath;

    public SystemLoadGaugeSet() {
        this(DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS, DEFAULT_FILE_PATH);
    }

    public SystemLoadGaugeSet(long dataTTL, TimeUnit unit) {
        this(dataTTL, unit, DEFAULT_FILE_PATH);
    }

    public SystemLoadGaugeSet(long dataTTL, TimeUnit unit, String filePath) {
        super(dataTTL, unit);
        loadAvg = new float[LoadAvg.values().length];
        this.filePath = filePath;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        gauges.put("load.1min", new Gauge<Float>() {
            @Override
            public Float getValue() {
                refreshIfNecessary();
                return loadAvg[LoadAvg.ONE_MIN.ordinal()];
            }
        });

        gauges.put("load.5min", new Gauge<Float>() {
            @Override
            public Float getValue() {
                refreshIfNecessary();
                return loadAvg[LoadAvg.FIVE_MIN.ordinal()];
            }
        });

        gauges.put("load.15min", new Gauge<Float>() {
            @Override
            public Float getValue() {
                refreshIfNecessary();
                return loadAvg[LoadAvg.FIFTEEN_MIN.ordinal()];
            }
        });

        return gauges;
    }


    @Override
    protected void collect() {
        String loadResult = FileUtils.readFile(filePath);
        //
        Matcher loadMatcher = loadPattern.matcher(loadResult);
        if (loadMatcher.matches()) {
            loadAvg[LoadAvg.ONE_MIN.ordinal()] = FormatUtils.formatFloat(loadMatcher.group(1));
            loadAvg[LoadAvg.FIVE_MIN.ordinal()] = FormatUtils.formatFloat(loadMatcher.group(2));
            loadAvg[LoadAvg.FIFTEEN_MIN.ordinal()] = FormatUtils.formatFloat(loadMatcher.group(3));
        }
    }
}
