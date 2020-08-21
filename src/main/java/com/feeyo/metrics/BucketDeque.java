package com.feeyo.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * 可以理解为一个时间窗口，用于保存最近N个时间间隔内的Bucket
 * 
 * 队列的更新应当是单线程的, 队列的遍历不会抛出{@link java.util.ConcurrentModificationException}
 */
class BucketDeque {

    private final Bucket[] queue;

    private int current = 0;

    BucketDeque(int length) {
        queue = new Bucket[length];
        for (int i = 0; i < length; i++) {
            queue[i] = new Bucket();
        }
    }

    void addLast(Bucket e) {
        current = (current + 1) % queue.length;
        queue[current] = e;
    }

    Bucket peek() {
        return queue[current];
    }

    /**
     * Example1:
     *      10:00   10:01  10:02   09:57   09:58   09:59
     *      70      80     90      40      50      60
     *              |       \
     *            startPos  latestIndex
     * Example2:
     *      10:00   09:55  09:56   09:57   09:58   09:59
     *      70      20     30      40      50      60
     *      |                                      |
     *      latestIndex                            startPos
     */
    List<Bucket> getBucketList() {
        int length = queue.length-1;
        List<Bucket> bucketList = new ArrayList<Bucket>();
        int startPos = current;
        long startTs = queue[current].timestamp;
        if (startPos < 0) {
            startPos = 0;
        }
        for (int i = startPos; i >= 0 && startPos - i < length; i--) {
            bucketList.add(queue[i]);
        }
        for (int i = length; i > startPos + 1; i--) {
            if (queue[i].timestamp > startTs) {
                // the current index has been update during this iteration
                // therefore the data shall not be collected
            } else {
                bucketList.add(queue[i]);
            }
        }
        return bucketList;
    }
}
