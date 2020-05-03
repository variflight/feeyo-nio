package com.feeyo.net.nio.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ConcurrentModificationException;


/**
 * Selector工具类
 * Created by Hash Zhang on 2017/7/24.
 */
public class SelectorUtil {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectorUtil.class);
    
    public static final int REBUILD_COUNT_THRESHOLD = 512;
    public static final long MIN_SELECT_TIME_IN_NANO_SECONDS = 500000L;

    public static Selector rebuildSelector(final Selector oldSelector) throws IOException {
        final Selector newSelector;
        try {
            newSelector = Selector.open();
        } catch (Exception e) {
            LOGGER.warn("Failed to create a new Selector.", e);
            return null;
        }

        for (;;) {
            try {
                for (SelectionKey key: oldSelector.keys()) {
                    Object a = key.attachment();
                    try {
                        if (!key.isValid() || key.channel().keyFor(newSelector) != null) {
                            continue;
                        }
                        int interestOps = key.interestOps();
                        key.cancel();
                        key.channel().register(newSelector, interestOps, a);
                        
                        
                    } catch (Exception e) {
                        LOGGER.warn("Failed to re-register a Channel to the new Selector.", e);
                        // Q: 在这个catch里面是否需要处理attachment: Connection 的关闭 ? 假设当前key的channel真的register失败的话 ? 看netty里面是进行了channel的close的样子
                        // A: 其实不需要，当前NIO 本身的机制就可以关闭Connection。这里直接返回null，依赖本身的机制关闭相关的资源
                    }
                }
                
            // Q: 在什么情况下会发生并发修改异常ConcurrentModificationException ?
            // A: oldSelector.keys()返回UngrowableSet（只能Remove，不能add），这个方法会cancel掉key，cancel掉key的同时，将key加入Selector的removeSet，在下次select的时候，Selector会remove掉这些key。
            //    目前的NIO架构不会触发这个（一个Selector只对应一个线程操作，无论是Acceptor还是Connector还是Reactor），但考虑移植代码完整性还有以后新设计的安全性，保留这个原有设计
            } catch (ConcurrentModificationException e) {
                // Probably due to concurrent modification of the key set.
                continue;
            }
            break;
        }
        oldSelector.close();
        return newSelector;
    }
    
    
}
