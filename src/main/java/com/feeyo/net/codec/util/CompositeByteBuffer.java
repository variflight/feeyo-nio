package com.feeyo.net.codec.util;

import com.feeyo.buffer.BufferPool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CompositeByteBuffer {
    //
    private final BufferPool bufferPool;
    //
    private final List<ByteBufferChunk> chunks = new ArrayList<>();
    private ByteBufferChunk tail = null; // 尾
    private int byteCount = 0;

    public CompositeByteBuffer(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public void add(byte[] data, int offset, int length) {
        this.byteCount += length;
        //
        // TODO: 只读ByteBuffer, 不调整position/limit
        ByteBuffer buffer = bufferPool.allocate(length);
        buffer.put(data, offset, length);
        //
        ByteBufferChunk c;
        if (tail != null) {
            c = new ByteBufferChunk(buffer, length, tail.endIndex);
            tail.setNext(c);
        } else {
            c = new ByteBufferChunk(buffer, length, 0);
        }
        chunks.add(c);
        tail = c;
    }

    public void add(CompositeByteBuffer data, int offset, int length) {
        this.byteCount += length;
        //
        // TODO: 只读ByteBuffer, 不调整position/limit
        ByteBuffer buffer = bufferPool.allocate(length);
        data.copy(buffer, offset, length);
        buffer.flip();
        //
        ByteBufferChunk c;
        if (tail != null) {
            c = new ByteBufferChunk(buffer, length, tail.endIndex);
            tail.setNext(c);
        } else {
            c = new ByteBufferChunk(buffer, length, 0);
        }
        chunks.add(c);
        tail = c;
    }

    private void copy(ByteBuffer dstBuffer, int offset, int length) {
        for (int i = 0; i < length; i++) {
            dstBuffer.put(get(offset + i));
        }
    }
    //
    private ByteBufferChunk lastAccessed = null;
    //
    public ByteBufferChunk findChunk(int index) {
    	//
    	if(lastAccessed != null && lastAccessed.isInBoundary(index)) {
    		return lastAccessed;
    	}
        // 二分查找
        for (int low = 0, high = chunks.size(); low <= high; ) {
            int mid = low + high >>> 1;
            ByteBufferChunk c = chunks.get(mid);
            if (index >= c.beginIndex + c.length) {
                low = mid + 1;
            } else if (index < c.beginIndex) {
                high = mid - 1;
            } else {
                assert c.length != 0;
                lastAccessed = c;
                return c;
            }
        }
        throw new IndexOutOfBoundsException("Not enough data.");
    }
    //
    public byte get(int index) {
        ByteBufferChunk c = findChunk(index);
        return c.get(index);
    }
    //
    public void set(int index, byte b) {
    	 ByteBufferChunk c = findChunk(index);
         c.set(index, b);
    }
    /*
     * 从 index 位置开始查找 指定 byte
     */
    public int firstIndex(int index, byte value) {
        if (index + 1 > byteCount) {
            throw new IndexOutOfBoundsException(String.format("index: %d, (expected: range(0, %d))", index, byteCount));
        }
        ByteBufferChunk c = findChunk(index);
        return c.find(index, value);
    }
    /*
	 * 取出指定区间的byte[], 可能跨多个Chunk
	 * beginIndex 截取的开始位置, length 需要截取的长度
	 */
    public byte[] getData(int beginIndex, int length) {
        ByteBufferChunk c = findChunk(beginIndex);
        return getData(c, beginIndex, length);
    }

    public byte[] getAll(byte[] extraData, int extraDataOffset) {
        byte[] content;
        // content buffer内容为空
        int extraDataLength = extraData.length - extraDataOffset;
        if (byteCount == 0) {
            if (extraDataOffset == 0) {
                content = extraData;
            } else {
                content = new byte[extraDataLength];
                System.arraycopy(extraData, extraDataOffset, content, 0, extraDataLength);
            }
        } else {
            int contentLength = extraDataLength + byteCount;
            content = new byte[contentLength];
            int off = 0;

            for (int i = 0; i < chunks.size(); i++) {
                ByteBufferChunk c = chunks.get(i);
                for (int j = 0; j < c.length; j++) {
                    content[off++] = c.data.get(j);
                }
            }
            System.arraycopy(extraData, extraDataOffset, content, off, extraDataLength);
        }
        return content;
    }

    public byte[] getData(ByteBufferChunk chunk, int beginIndex, int length) {
        assert chunk != null;
        //
        byte[] dst = new byte[length];
        ByteBufferChunk c = chunk;
        int remaining = length;
        int dstPos = 0;
        //
        int srcPos = beginIndex - c.beginIndex;
        int srcLength;
        //
        while (c != null && remaining > 0) {
            // 是否第一次
            if (remaining < length) {
                // 否
                srcPos = 0;
                srcLength = Math.min(remaining, c.length);
            } else {
                srcLength = Math.min(remaining, c.length - srcPos);
            }
            //
            for (int i = 0; i < srcLength; i++) {
                dst[dstPos + i] = c.data.get(srcPos + i);
            }
            remaining = remaining - srcLength;
            dstPos = dstPos + srcLength;
            c = c.next;
        }
        return dst;
    }

    /**
     * 返回剩余可读字节数
     */
    public int remaining(int readIndex) {
        return Math.max(byteCount - readIndex, 0);
    }

    public int getByteCount() {
        return byteCount;
    }

    public void clear() {
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).clear();
            chunks.set(i, null);
        }
        chunks.clear();
        tail = null;
        lastAccessed = null;
        byteCount = 0;
    }

    /**
     * 包装了 ByteBuffer, 增加了 length 和 beginIndex 方便查找
     */
    public final class ByteBufferChunk {
        private ByteBuffer data;
        private int beginIndex;
        private int length;
        private int endIndex;
        //
        // TODO:优化遍历, 维护单向链表
        private ByteBufferChunk next; 
        //
        public ByteBufferChunk(ByteBuffer data, int length, int beginIndex) {
            this.data = data;
            this.length = length;
            this.beginIndex = beginIndex;
            this.endIndex = beginIndex + length;
        }

        public ByteBufferChunk getNext() {
            return next;
        }

        public void setNext(ByteBufferChunk next) {
            this.next = next;
        }

        // 边界检查
        public boolean isInBoundary(int index) {
            return index >= beginIndex && index < endIndex;
        }
        //
        public byte get(int index) {
            if (index < endIndex || next == null) {
                return data.get(index - beginIndex);
            }
            return next.get(index);
        }
        //
        public void set(int index, byte b) {
        	 if (index < endIndex || next == null) {
                 data.put(index - beginIndex, b);
                 return;
             }
        	 next.set(index, b);
        }

        /*
         * 从指定位置开始往后找value, 找到返回其偏移
         */
        public int find(int index, byte value) {
        	 // TODO: 利用链表和数组快速查找
            ByteBufferChunk c = this;
            while (c != null) {
                while (index < c.endIndex) {
                    if (value == c.get(index)) {
                        return index;
                    }
                    index++;
                }
                c = c.next;
            }
            return -1;
        }

        public void clear() {
            bufferPool.recycle(data);
            this.data = null;
            this.next = null;
        }
    }
}