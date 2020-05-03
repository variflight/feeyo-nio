package com.feeyo.metrics;

import com.feeyo.metrics.util.LongAdder;

// 计数器型指标，适用于记录调用总量等类型的数据
//
public class Counter implements Metric, Resetable {

	private LongAdder count = new LongAdder();

	// 计数器加1
	public void inc() {
		inc(1);
	}

	// 计数器加n
	public void inc(long n) {
		count.add(n);
	}

	public void dec() {
		dec(1);
	}

	// 计数器减n
	public void dec(long n) {
		count.add(-n);
	}

	public long count() {
		return count.sum();
	}
	
	@Override
	public void reset() {
		// 避免调用 LongAdder#reset, 这是一个相当耗CPU的操作
		count = new LongAdder();
	}

}
