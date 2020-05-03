package com.feeyo.net.nio.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

public class NullBufferPool extends BufferPool {

	public NullBufferPool(long minBufferSize, long maxBufferSize, int decomposeBufferSize, int minChunkSize,
			int[] increments, int maxChunkSize) {
		super(minBufferSize, maxBufferSize, decomposeBufferSize, minChunkSize, increments, maxChunkSize);
	}

	@Override
	public ByteBuffer allocate(int size) {
		return ByteBuffer.allocate(size);
	}

	@Override
	public void recycle(ByteBuffer theBuf) {
		if ( theBuf != null ) {
			theBuf.clear();
			theBuf.order(ByteOrder.BIG_ENDIAN);
		}
		theBuf = null;
	}

	@Override
	public long capacity() {
		return 0;
	}

	@Override
	public long size() {
		return 0;
	}

	@Override
	public long getSharedOptsCount() {
		return 0;
	}

	@Override
	public ConcurrentHashMap<Long, Long> getNetDirectMemoryUsage() {
		return null;
	}

}
