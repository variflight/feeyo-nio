package com.feeyo.net.codec.http;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.feeyo.net.nio.NetSystem;

/**
 * 
 * Http Response encode
 * 
 * @author xuwenfeng
 * @author zhuam
 *
 */
public class HttpResponseEncoder {
	
	private static final String CRLF = "\r\n"; 	
	private static final String SP = " "; 		
	private static final String COLON = ":"; 	
	//
	private byte[] getHeaderBytes(HttpResponse response) {
		// headline
		StringBuilder headerSb = new StringBuilder(64);
		headerSb.append(response.getHttpVersion()).append(SP);
		headerSb.append(response.getStatusCode()).append(SP);
		headerSb.append(response.getReasonPhrase()).append(CRLF);
		//
		// TODO: 修正 Content-Length
		if (response.headers.get(HttpHeaderNames.CONTENT_LENGTH) == null) {
			byte[] content = response.getContent();
			if (content == null) {
				response.headers().put(HttpHeaderNames.CONTENT_LENGTH, "0");
			} else {
				response.headers().put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(content.length));
			}
		}
		// headers
		for (Map.Entry<String, String> h : response.headers().entrySet()) {
			headerSb.append(h.getKey()).append(COLON).append(SP);
			headerSb.append(h.getValue()).append(CRLF);
		}
		headerSb.append(CRLF);
		return headerSb.toString().getBytes();
	}
	
	public byte[] encodeToByteArray(HttpResponse response) {	
		if ( response == null )
			return null;
		//
    	byte[] headerBytes = getHeaderBytes( response );
    	byte[] contentBytes = response.getContent();
        if( contentBytes != null ) {
        	byte[] allBytes = new byte[ headerBytes.length + contentBytes.length  ]; 
	        System.arraycopy(headerBytes, 0, allBytes, 0, headerBytes.length);
	        System.arraycopy(contentBytes, 0, allBytes, headerBytes.length, contentBytes.length);
	        return allBytes;
        } else {
        	return headerBytes;
        }
	}
	
	public ByteBuffer encodeToByteBuffer(HttpResponse response) {
		if ( response == null )
			return null;
		//
		// Use direct byte buffer
		ByteBuffer buffer = null;
		try {
	    	byte[] headerBytes = getHeaderBytes( response );
	    	byte[] contentBytes = response.getContent();
	        if( contentBytes != null ) {
	        	buffer = NetSystem.getInstance().getBufferPool().allocate( headerBytes.length + contentBytes.length );
    			buffer.put( headerBytes );
    			buffer.put( contentBytes );
    			return buffer;
		        
	        } else {
	        	buffer = NetSystem.getInstance().getBufferPool().allocate( headerBytes.length );
    			buffer.put( headerBytes );
    			return buffer;
	        }
	        
		} catch(BufferOverflowException e) {		
			if ( buffer != null )
				NetSystem.getInstance().getBufferPool().recycle( buffer );
			throw e;
		}
	}	
}