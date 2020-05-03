package com.feeyo.metrics;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import com.feeyo.metrics.util.LongAdder;

import static java.lang.Math.floor;


/**
 * A random sampling reservoir of a stream of {@code long}s. Uses Vitter's Algorithm R to produce a
 * statistically representative sample.
 *
 * @see <a href="http://www.cs.umd.edu/~samir/498/vitter.pdf">Random Sampling with a Reservoir</a>
 */
public class UniformHistogram extends Histogram {
	
	private static final int DEFAULT_SIZE = 1028;
	//
	private final AtomicLongArray values;
	private final AtomicLong atomic_count = new AtomicLong();
	
	// 1028个元素，可信度达到99.9％, 假设正态分布，误差率为5％左右
	public UniformHistogram() {
		super();
		//
		this.values = new AtomicLongArray( DEFAULT_SIZE );
		for (int i = 0; i < values.length(); i++) {
			values.set(i, 0);
		}
	}

	@Override
	public void update(long value) {
		//
		final long c = atomic_count.incrementAndGet();
		if (c <= values.length()) {
			values.set((int) c - 1, value);
		} else {
			final long r = ThreadLocalRandom.current().nextLong(c);
			if (r < values.length()) {
				values.set((int) r, value);
			}
		}
		
		//
		sum.add(value);
		if (value > max)
			max = value;
		
		if (value < min)
			min = value;
	}
	
	@Override
	public long count() {
		return atomic_count.get();
	}
	
	@Override
	public long avg() {
        long countSum = atomic_count.get();
        if (countSum == 0) {
            return 0;
        }
		return sum.sum() / atomic_count.get();
	}
	
	@Override
	public void reset() {
		this.count = new LongAdder();
		this.sum = new LongAdder();
		this.min = 0;
		this.max = 0;
		
		//
		for (int i = 0; i < values.length(); i++) {
			values.set(i, 0);
		}
		//
		atomic_count.set(0);
	}
	
	///
	private int size() {
        final long c = atomic_count.get();
        if (c > values.length()) {
            return values.length();
        }
        return (int) c;
    }
	
	//
	public long[] getValues() {
		final int s = size();
		long[] copy = new long[s];
		for (int i = 0; i < s; i++) {
			copy[i] = values.get(i);
		}
		Arrays.sort( copy );
		return copy;
	}
	
    public double getValue(long[] values, double quantile) {
    	//
        if (quantile < 0.0 || quantile > 1.0 || Double.isNaN(quantile)) {
            throw new IllegalArgumentException(quantile + " is not in [0..1]");
        }

        if (values.length == 0) {
            return 0.0;
        }

        final double pos = quantile * (values.length + 1);
        final int index = (int) pos;

        if (index < 1) {
            return values[0];
        }

        if (index >= values.length) {
            return values[values.length - 1];
        }

        final double lower = values[index - 1];
        final double upper = values[index];
        return lower + (pos - floor(pos)) * (upper - lower);
    }
    
    
    public double get50thPercentile(long[] values) {
        return getValue(values, 0.5);
    }
    
    public double get75thPercentile(long[] values) {
        return getValue(values, 0.75);
    }
    
    public double get95thPercentile(long[] values) {
        return getValue(values, 0.95);
    }
    
    public double get99thPercentile(long[] values) {
        return getValue(values, 0.99);
    }
    
    public double get999thPercentile(long[] values) {
        return getValue(values, 0.999);
    }
	
	
	@Override
	public String toString() {
		//
		long[] values = getValues();
		//
		StringBuffer sb = new StringBuffer(30);
		sb.append("sum=").append( sum() );
		sb.append(", count=").append( count() );
		sb.append(", max=").append( max() );
		sb.append(", min=").append( min() );
		sb.append(", avg=").append( avg() );
		sb.append(", ").append( String.format("50%%<=%2.2f", get50thPercentile(values)) );
		sb.append(", ").append( String.format("75%%<=%2.2f", get75thPercentile(values)) );
		sb.append(", ").append( String.format("95%%<=%2.2f", get95thPercentile(values)) );
		sb.append(", ").append( String.format("99%%<=%2.2f", get99thPercentile(values)) );
		return sb.toString();
	}

}