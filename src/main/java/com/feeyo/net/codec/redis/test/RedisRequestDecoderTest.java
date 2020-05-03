package com.feeyo.net.codec.redis.test;

import java.util.List;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.redis.RedisRequest;
import com.feeyo.net.codec.redis.RedisRequestDecoder;

public class RedisRequestDecoderTest {
	
	
	public static void main(String[] args) {
		
		RedisRequestDecoder decoder = new RedisRequestDecoder();
		long t = System.currentTimeMillis();

	    for(int j = 0; j < 10000000; j++) {	    	
	    	try {	    		
	    		byte[] buff = "*2\r\n$3\r\nGET\r\n$2\r\naa\r\n".getBytes();
	    		buff = "  *1\r\n$5\r\nMULTI\r\n*2\r\n$4\r\nLLEN\r\n$6\r\ncelery\r\n*1\r\n$4\r\nEXEC\r\n".getBytes();
	    		buff = "*2\r\n$4\r\nKEYS\r\n$1\r\\*\r\n".getBytes();
//	    		buff = "QUIT\r\n".getBytes();
//	    		buff = "*1\r\n$4\r\nPING\r\n".getBytes();
//	    		buff = "set test 4\r\ntest\r\n".getBytes();
//	    		buff = "set test testxxx\r\nset test te\r\n".getBytes();
	    		
	    		byte[] buff1 = new byte[ buff.length - 8 ];
	    		byte[] buff2 = new byte[ buff.length - buff1.length ];
	    		System.arraycopy(buff, 0, buff1, 0, buff1.length);
	    		System.arraycopy(buff, buff1.length, buff2, 0, buff2.length);
	    		
//	    		byte[] buff1 = buff;
//	    		byte[] buff2  = new byte[buff.length + buff1.length];
//	    		System.arraycopy(buff, 0, buff2, 0, buff.length);
//	    		System.arraycopy(buff1, 0, buff2, buff.length, buff1.length);
	    		
	    		
	    		
	    		long t1 = System.currentTimeMillis();
				List<RedisRequest> reqs = decoder.decode( buff1 );								
				reqs = decoder.decode( buff2  );								
				long t2 = System.currentTimeMillis();
		    	int diff = (int)(t2-t1);
		    	if ( diff > 1) {
		    		System.out.println(" decode diff=" + diff + ", req=" + reqs.toString() );
		    	}
				
			} catch (UnknownProtocolException e) {
				e.printStackTrace();
			}
	    }  
	    System.out.println(System.currentTimeMillis() - t);
	
	}

}
