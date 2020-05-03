package com.feeyo.net.nio.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeakTrackingBufferPool extends BufferPool {
	
	private static final Logger LOG = LoggerFactory.getLogger(LeakTrackingBufferPool.class);

	//
	private final LeakDetector<ByteBuffer> leakDetector = new LeakDetector<ByteBuffer>() {
		@Override
		public String id(ByteBuffer resource) {
			StringBuilder out = new StringBuilder();
			out.append(resource.getClass().getSimpleName());
			out.append("@");
			out.append(Integer.toHexString(System.identityHashCode(resource)));
			return out.toString();
		}

		@Override
		protected void leaked(LeakInfo leakInfo) {
			leaked.incrementAndGet();
			LeakTrackingBufferPool.this.leaked(leakInfo);
		}
	};
	//
	private final BufferPool delegate;
	private final AtomicLong leakedReleases = new AtomicLong(0);
	private final AtomicLong leakedAcquires = new AtomicLong(0);
	private final AtomicLong leaked = new AtomicLong(0);

	public LeakTrackingBufferPool(BufferPool delegate) {
		//
		super(delegate.getMinBufferSize(), delegate.getMaxBufferSize(), delegate.getDecomposeBufferSize(),  //
				delegate.getMinChunkSize(), delegate.getIncrements(), delegate.getMaxChunkSize());
		this.delegate = delegate;
		//
		// -------------------Start & Stop leakDetector --------------------
		// 
		try {
			leakDetector.start();
		} catch (Exception x) {
			throw new RuntimeException(x);
		}
		//
		//
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					leakDetector.stop();
				} catch (Exception x) {
					throw new RuntimeException(x);
				}
			}
		});
	}

	@Override
	public ByteBuffer allocate(int size) {
		ByteBuffer buffer = delegate.allocate(size);
		boolean leaked = leakDetector.acquired(buffer);
		if ( !leaked) {
			leakedAcquires.incrementAndGet();
			LOG.info(String.format("ByteBuffer acquire %s leaked.acquired=%s", leakDetector.id(buffer),
					leaked ? "normal" : "LEAK"), new Throwable("LeakStack.Acquire"));
		}
		return buffer;
	}

	@Override
	public void recycle(ByteBuffer buffer) {
		if (buffer == null)
			return;
		boolean leaked = leakDetector.released(buffer);
		if (!leaked) {
			leakedReleases.incrementAndGet();
			LOG.info(String.format("ByteBuffer release %s leaked.released=%s", leakDetector.id(buffer),
					leaked ? "normal" : "LEAK"), new Throwable("LeakStack.Release"));
		}
		delegate.recycle(buffer);
	}

	public void clearTracking() {
		leakedAcquires.set(0);
		leakedReleases.set(0);
	}

	public long getLeakedAcquires() {
		return leakedAcquires.get();
	}

	public long getLeakedReleases() {
		return leakedReleases.get();
	}

	public long getLeakedResources() {
		return leaked.get();
	}

	protected void leaked(LeakDetector<ByteBuffer>.LeakInfo leakInfo) {
		LOG.warn("ByteBuffer " + leakInfo.getResourceDescription() + " leaked at:", leakInfo.getStackFrames());
	}

	@Override
	public long capacity() {
		return delegate.capacity();
	}

	@Override
	public long size() {
		return delegate.size();
	}

	@Override
	public long getSharedOptsCount() {
		return delegate.getSharedOptsCount();
	}

	@Override
	public ConcurrentHashMap<Long, Long> getNetDirectMemoryUsage() {
		return delegate.getNetDirectMemoryUsage();
	}
}