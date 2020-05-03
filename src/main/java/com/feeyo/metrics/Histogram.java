package com.feeyo.metrics;


import com.feeyo.metrics.util.LongAdder;

/**
 * 
 * 直方分布度量器
 * 
 * 直方分布指标，例如，可以用于统计某个接口的响应时间，max、min、avg、sum、count 
 */
public class Histogram implements Metric, Resetable {
    //
	protected LongAdder count;
	protected LongAdder sum;
	
	protected long min;
	protected long max;
	
    //
    public Histogram() {
    	this.count = new LongAdder();
		this.sum = new LongAdder();
		this.min = Long.MAX_VALUE;
		this.max = 0;
    }

	public void update(int value) {
		 update((long) value);
	}

	public void update(long value) {
		//
		count.add(1);
		sum.add(value);

		if (value > max)
			max = value;

		if (value < min)
			min = value;
	}
	
    
	public long count() {
		return count.sum();
	}
    
	public long sum() {
		return sum.sum();
	}
	
	public long min() {
		return min;
	}
	
	public long max() {
		return max;
	}
	
	public long avg() {
        long countSum = count.sum();
        if (countSum == 0) {
            return 0;
        }
		return sum.sum() / count.sum();
	}

	@Override
	public void reset() {
		this.count = new LongAdder();
		this.sum = new LongAdder();
		this.min = 0;
		this.max = 0;
	}
	
	@Override
	public String toString() {
		return String.format("cnt=%s, max/avg=%s/%s", count(), max(), avg());
	}
}