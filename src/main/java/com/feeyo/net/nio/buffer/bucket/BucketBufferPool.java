package com.feeyo.net.nio.buffer.bucket;

import com.feeyo.net.nio.buffer.BufferPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 堆外内存池
 * 
 * @author zhuam
 *
 */
public class BucketBufferPool extends BufferPool {
	
	private static Logger LOGGER = LoggerFactory.getLogger( BucketBufferPool.class );
	
	private List<AbstractBucket> _buckets;
	
	private long sharedOptsCount;
	
	public BucketBufferPool(long minBufferSize, long maxBufferSize, int decomposeBufferSize,
			int minChunkSize, int[] increments, int maxChunkSize) {
		
		super(minBufferSize, maxBufferSize, decomposeBufferSize, minChunkSize, increments, maxChunkSize);
		//
		int bucketCount;
		if (increments.length > 1) {
			bucketCount = increments.length;
		} else {
			bucketCount = maxChunkSize / increments[0];
		}
		
		this._buckets = new ArrayList<AbstractBucket>();
		
		// 平均分配初始化的桶size 
		long bucketBufferSize = minBufferSize / bucketCount;
		
		// 初始化桶 
		int chunkSize = 0;
		for (int i = 0; i < bucketCount; i++) {
			//
			if ( increments.length > 1 ) {
				chunkSize = increments[i];
			} else {
				chunkSize += increments[0];
			}
			
			
			int chunkCount = (int) (bucketBufferSize / chunkSize);
			boolean isExpand = (chunkSize <= 2097152); 	// 2MB内的块 支持自动扩容
			//
			// 测试结果 队列长度2048的时候效果就没那么显著了。
			AbstractBucket bucket;
			if (chunkCount > 2000) {
				bucket = new DefaultArrayBucket(this, chunkSize, chunkCount, isExpand);
			} else {
				bucket = new DefaultBucket(this, chunkSize, chunkCount, isExpand);
			}

			this._buckets.add(i, bucket);
		}

	}
	
	//根据size寻找 桶
	private AbstractBucket bucketFor(int size) {
		if (size <= minChunkSize)
			return null;
		//
		for(int i = 0; i < _buckets.size(); i++) {
			AbstractBucket bucket =  _buckets.get(i);
			if (bucket.getChunkSize() >= size)
				return bucket;
		}
		return null;
	}
	
	//TODO : debug err, TMD, add temp synchronized
	
	@Override
	public ByteBuffer allocate(int size) {		
	    	
		ByteBuffer byteBuf = null;
		//
		// 根据容量大小size定位到对应的桶Bucket
		AbstractBucket bucket = bucketFor(size);
		if ( bucket != null) {
			byteBuf = bucket.allocate();
		}
		
		// 堆内
		if (byteBuf == null) {
			byteBuf =  ByteBuffer.allocate( size );
		}
		return byteBuf;

	}

	@Override
	public void recycle(ByteBuffer buf) {
		if (buf == null) 
			return;
		//
		if( !buf.isDirect() ) 
			return;
      	//
		AbstractBucket bucket = bucketFor( buf.capacity() );
		if (bucket != null) {
			bucket.recycle( buf );
			sharedOptsCount++;
		//
		} else {
			LOGGER.warn("Trying to put a buffer, not created by this pool! Will be just ignored");
		}
	}

	public synchronized AbstractBucket[] buckets() {
		
		AbstractBucket[] tmp = new AbstractBucket[ _buckets.size() ];
		int i = 0;
		for(AbstractBucket b: _buckets) {
			tmp[i] = b;
			i++;
		}
		return tmp;
	}
	
	@Override
	public long getSharedOptsCount() {
		return sharedOptsCount;
	}

	@Override
	public long capacity() {
		return this.maxBufferSize;
	}

	@Override
	public long size() {
		return this.usedBufferSize.get();
	}

	@Override
	public int getChunkSize() {
		return this.getMinChunkSize();
	}

	@Override
	public ConcurrentHashMap<Long, Long> getNetDirectMemoryUsage() {
		return null;
	}
}