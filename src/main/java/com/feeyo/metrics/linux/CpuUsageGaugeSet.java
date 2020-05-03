package com.feeyo.metrics.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feeyo.metrics.MetricSetCollect;
import com.feeyo.metrics.linux.util.FileUtils;
import com.feeyo.metrics.linux.util.FormatUtils;
import com.feeyo.metrics.Gauge;
import com.feeyo.metrics.Metric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CpuUsageGaugeSet extends MetricSetCollect {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpuUsageGaugeSet.class);

    private static final String DELIM = "\\s+";

    // Pattern.DOTALL to match pattern across multiple lines
    private static final Pattern cpuStatPattern =
            Pattern.compile("^.*cpu\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+)\\s+([\\d]+).*$",
                    Pattern.DOTALL);

    private static final String DEFAULT_FILE_PATH = "/proc/stat";

    // store the cpu usage, in the order of user, system, idle
    private float[] cpuUsage;

    private CpuInfo lastCollectedCpuInfo;

    private String filePath;

    private enum CpuUsage {
        USER, NICE, SYSTEM, IDLE, IOWAIT, IRQ, SOFTIRQ, STEAL, GUEST
    }

    /**
     * The "procs_running" line gives the number of processes currently running on CPUs.
     */
    private long processRunning;

    /**
     * The "procs_blocked" line gives the number of processes currently blocked, waiting for I/O to complete.
     */
    private long processBlocked;

    /**
     * The "intr" line gives counts of interrupts serviced since boot time,
     * for each of the possible system interrupts.
     * The first column is the total of all interrupts serviced;
     * each subsequent column is the total for that particular interrupt.
     */
    private long totalInterrupts;

    /**
     * The number of interrupts per second between collection
     * This is a equivalent implementation of 'in' in vmstat
     * https://www.thomas-krenn.com/en/wiki/Linux_Performance_Measurements_using_vmstat
     */
    private double interruptsRate;

    /**
     * The "ctxt" line gives the total number of context switches across all CPUs.
     */
    private long totalContextSwitches;

    /**
     * The number of context switches per second between collection
     * This is a equivalent implementation of 'cs' in vmstat
     * https://www.thomas-krenn.com/en/wiki/Linux_Performance_Measurements_using_vmstat
     */
    private double contextSwitchesRate;

    /**
     * Store all the metrics in this class
     */
    private Map<String, Metric> gauges = new HashMap<String, Metric>();

    public CpuUsageGaugeSet() {
        this(DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS, DEFAULT_FILE_PATH);
    }

    public CpuUsageGaugeSet(String filePath) {
        this(DEFAULT_DATA_TTL, TimeUnit.MILLISECONDS, filePath);
    }

    public CpuUsageGaugeSet(long dataTTL, TimeUnit unit) {
        this(dataTTL, unit, DEFAULT_FILE_PATH);
    }

    public CpuUsageGaugeSet(long dataTTL, TimeUnit unit, String filePath) {
        super(dataTTL, unit);
        //
        cpuUsage = new float[CpuUsage.values().length];
        lastCollectedCpuInfo = new CpuInfo();
        this.filePath = filePath;
        populateMetrics();
    }


    @Override
    public Map<String, Metric> getMetrics() {
        return gauges;
    }

    @Override
    protected void collect() {
        // collect again
        List<String> lines;
        try {
            lines = FileUtils.readFileAsStringArray(filePath);
        } catch (Exception e) {
            LOGGER.warn("Error during reading file {}", filePath, e);
            return;
        }

        for (String line: lines) {
            if (line.startsWith("cpu ")) {
                try {
                    CpuInfo current = collectCpuInfo(line);
                    //
                    cpuUsage[CpuUsage.USER.ordinal()] =    getUsage(current.userTime, lastCollectedCpuInfo.userTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.NICE.ordinal()] =    getUsage(current.niceTime, lastCollectedCpuInfo.niceTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.SYSTEM.ordinal()] =  getUsage(current.systemTime, lastCollectedCpuInfo.systemTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.IDLE.ordinal()] =    getUsage(current.idleTime, lastCollectedCpuInfo.idleTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.IOWAIT.ordinal()] =  getUsage(current.iowaitTime, lastCollectedCpuInfo.iowaitTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.IRQ.ordinal()] =     getUsage(current.irqTime, lastCollectedCpuInfo.irqTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.SOFTIRQ.ordinal()] = getUsage(current.softirqTime, lastCollectedCpuInfo.softirqTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.STEAL.ordinal()] =   getUsage(current.stealTime, lastCollectedCpuInfo.stealTime, current, lastCollectedCpuInfo);
                    cpuUsage[CpuUsage.GUEST.ordinal()] =   getUsage(current.guestTIme, lastCollectedCpuInfo.guestTIme, current, lastCollectedCpuInfo);
                    lastCollectedCpuInfo = current;
                } catch (Exception e) {
                    LOGGER.warn("Error parsing cpu info: ", e);
                }
            } else if (line.startsWith("intr")) {
                try {
                    String data = line.substring("intr ".length(), line.indexOf(' ', "intr ".length()));
                    long latestIntr = Long.parseLong(data);
                    if (totalInterrupts == 0) {
                        // first time
                        interruptsRate = 0.0d;
                    } else if (latestIntr >= totalInterrupts){
                        long duration = System.currentTimeMillis() - lastCollectTime.get();
                        interruptsRate = 1000.0d * (latestIntr - totalInterrupts) / duration;
                    } else {
                        LOGGER.warn("Invalid interrupt data, last collected {}, current {}, raw {}",
                                totalInterrupts, latestIntr, line);
                        interruptsRate = -1;
                    }
                    totalInterrupts = latestIntr;
                } catch (Exception e) {
                    interruptsRate = -1;
                    LOGGER.warn("Error parsing intr info: ", e);
                }
            } else if (line.startsWith("ctxt")) {
                try {
                    String[] data = line.split(DELIM);
                    long latestCtxt = Long.parseLong(data[1]);
                    if (totalContextSwitches == 0) {
                        contextSwitchesRate = 0.0d;
                    } else if (latestCtxt >= totalContextSwitches) {
                        long duration = System.currentTimeMillis() - lastCollectTime.get();
                        contextSwitchesRate = 1000.0d * (latestCtxt - totalContextSwitches) / duration;
                    } else {
                        LOGGER.warn("Invalid context data, last collected {}, current {}, raw {}",
                                totalContextSwitches, latestCtxt, line);
                        contextSwitchesRate = -1;
                    }
                    totalContextSwitches = latestCtxt;
                } catch (Exception e) {
                    contextSwitchesRate = -1;
                    LOGGER.warn("Error parsing context switch info: ", e);
                }
            } else if (line.startsWith("procs_running")) {
                try {
                    String[] data = line.split(DELIM);
                    processRunning = Long.parseLong(data[1]);
                } catch (NumberFormatException n) {
                    processRunning = -1;
                    LOGGER.warn("Invalid line of process running found, raw data: {}", line);
                } catch (Exception e) {
                    processRunning = -1;
                    LOGGER.warn("Error parsing process running info: ", e);
                }
            } else if (line.startsWith("procs_blocked")) {
                try {
                    String[] data = line.split(DELIM);
                    processBlocked = Long.parseLong(data[1]);
                } catch (Exception e) {
                    processBlocked = -1;
                    LOGGER.warn("Error parsing process blocked info: ", e);
                }
            }
        }

    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private void populateMetrics() {
        gauges.put("cpu.user", new CpuGauge(CpuUsage.USER.ordinal()));
        gauges.put("cpu.nice", new CpuGauge(CpuUsage.NICE.ordinal()));
        gauges.put("cpu.system", new CpuGauge(CpuUsage.SYSTEM.ordinal()));
        gauges.put("cpu.idle", new CpuGauge(CpuUsage.IDLE.ordinal()));
        gauges.put("cpu.iowait", new CpuGauge(CpuUsage.IOWAIT.ordinal()));
        gauges.put("cpu.irq", new CpuGauge(CpuUsage.IRQ.ordinal()));
        gauges.put("cpu.softirq", new CpuGauge(CpuUsage.SOFTIRQ.ordinal()));
        gauges.put("cpu.steal", new CpuGauge(CpuUsage.STEAL.ordinal()));
        gauges.put("cpu.guest", new CpuGauge(CpuUsage.GUEST.ordinal()));

        gauges.put("interrupts", new Gauge<Double>() {
            @Override
            public Double getValue() {
                refreshIfNecessary();
                return interruptsRate;
            }
        });

        gauges.put("context_switches", new Gauge<Double>() {
            @Override
            public Double getValue() {
                refreshIfNecessary();
                return contextSwitchesRate;
            }
        });

        gauges.put("process.running", new Gauge<Long>() {
            @Override
            public Long getValue() {
                refreshIfNecessary();
                return processRunning;
            }
        });

        gauges.put("process.blocked", new Gauge<Long>() {
            @Override
            public Long getValue() {
                refreshIfNecessary();
                return processBlocked;
            }
        });
    }

    private CpuInfo collectCpuInfo(String statResult) {
        CpuInfo info = new CpuInfo();
        Matcher statMatcher = cpuStatPattern.matcher(statResult);
        if (statMatcher.matches()) {
            for (int i = 1; i <= statMatcher.groupCount(); i++) {
                long time = Long.parseLong(statMatcher.group(i));
                switch (i) {
                    case 1:
                        info.userTime = time;
                        break;
                    case 2:
                        info.niceTime = time;
                        break;
                    case 3:
                        info.systemTime = time;
                        break;
                    case 4:
                        info.idleTime = time;
                        break;
                    case 5:
                        info.iowaitTime = time;
                        break;
                    case 6:
                        info.irqTime = time;
                        break;
                    case 7:
                        info.softirqTime = time;
                        break;
                    case 8:
                        info.stealTime = time;
                        break;
                    case 9:
                        info.guestTIme = time;
                        break;
                }
                info.totalTime += time;
            }
        }
        return info;
    }

    private float getUsage(long current, long last, CpuInfo curInfo, CpuInfo lastInfo) {
        try {
            float f = 100.0f * (current - last) / (curInfo.totalTime - lastInfo.totalTime);
            return FormatUtils.formatFloat(f);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private class CpuGauge implements Gauge<Float> {

        private int index;

        public CpuGauge(int index) {
            this.index = index;
        }

        @Override
        public Float getValue() {
            try {
                refreshIfNecessary();
                return cpuUsage[index];
            } catch (Exception e) {
                return 0.0f;
            }
        }
    }

    /**
     * https://www.kernel.org/doc/Documentation/filesystems/proc.txt
     */
    private class CpuInfo {
        /**
         * user: normal processes executing in user mode
         */
        long userTime;
        /**
         * nice: niced processes executing in user mode
         */
        long niceTime;
        /**
         * system: processes executing in kernel mode
         */
        long systemTime;
        /**
         * idle: twiddling thumbs
         */
        long idleTime;
        /**
         * iowait: In a word, iowait stands for waiting for I/O to complete. But there
             are several problems:
             1. Cpu will not wait for I/O to complete, iowait is the time that a task is
             waiting for I/O to complete. When cpu goes into idle state for
             outstanding task io, another task will be scheduled on this CPU.
             2. In a multi-core CPU, the task waiting for I/O to complete is not running
             on any CPU, so the iowait of each CPU is difficult to calculate.
             3. The value of iowait field in /proc/stat will decrease in certain
             conditions.
             So, the iowait is not reliable by reading from /proc/stat.
         */
        long iowaitTime;
        /**
         * irq: servicing interrupts
         */
        long irqTime;
        /**
         * softirq: servicing softirqs
         */
        long softirqTime;
        /**
         * steal: involuntary wait, since 2.6.11
         */
        long stealTime;
        /**
         * guest: running a normal guest, since 2.6.24
         */
        long guestTIme;
        /**
         * Total time
         */
        long totalTime;
    }

}
