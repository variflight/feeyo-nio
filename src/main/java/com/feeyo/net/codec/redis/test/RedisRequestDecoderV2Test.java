package com.feeyo.net.codec.redis.test;

import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.redis.RedisRequest;
import com.feeyo.net.codec.redis.RedisRequestDecoderV2;

public class RedisRequestDecoderV2Test {
	
	 /**
     * 性能测试结果： <br>
     * 包长度为61(普通的请求: 一次鉴权+一次hashtable长度查询) <br>
     * 循环1kw次解析半包耗时31s, V1版本约为72s <br>
     * 循环1kw次解析整包耗时5s, V1版本约为3s <br>
     *
     * 包长度为565(批量请求) <br>
     * 循环1kw次解析半包耗时137s, V1版本约为207s <br>
     * 循环1kw次解析整包耗时约36s, V1版本约为27s <br>
     */
    public static void main(String[] args) {
        RedisRequestDecoderV2 decoder = new RedisRequestDecoderV2();
        long t = System.currentTimeMillis();

        for (int j = 0; j < 10000000; j++) {
            try {
                // 没有半包数据
                byte[] buff = "*2\r\n$4\r\nAUTH\r\n$5\r\npwd01\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHER\r\n".getBytes();

                // byte[] buff = ("*2\r\n$4\r\nAUTH\r\n$5\r\npwd01\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHER\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE0\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE1\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE2\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE3\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE4\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE5\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE6\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE7\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE8\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATHE9\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATH10\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATH11\r\n" +
                //         "*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATH12\r\n*2\r\n$4\r\nhlen\r\n$15\r\nSPECIAL_WEATH13\r\n").getBytes();
                // byte[] buff1 = new byte[ 213 ];
                // byte[] buff2 = new byte[ 155 ];
                // byte[] buff3 = new byte[ buff.length - buff1.length - buff2.length ];
                // System.arraycopy(buff, 0, buff1, 0, buff1.length);
                // System.arraycopy(buff, buff1.length, buff2, 0, buff2.length);
                // System.arraycopy(buff, buff1.length + buff2.length, buff3, 0, buff3.length);

                long t1 = System.currentTimeMillis();
                // decoder.decode(buff1);
                // decoder.decode( buff2 );
                // List<RedisRequest> reqList = decoder.decode( buff3 );
                List<RedisRequest> reqList = decoder.decode( buff );
                long t2 = System.currentTimeMillis();
                int diff = (int) (t2 - t1);
                if (diff > 2) {
                    System.out.println(" decode diff=" + diff + ", request=" + reqList.toString());
                }
            } catch (UnknownProtocolException e) {
                e.printStackTrace();
            }
        }
        System.out.println(System.currentTimeMillis() - t);
    }

}
