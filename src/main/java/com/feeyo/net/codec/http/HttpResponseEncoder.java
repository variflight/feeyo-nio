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
	
	
	private byte[] getHeaderBytes(HttpResponse response) {
		// headline
		StringBuffer headerSb = new StringBuffer();
		headerSb.append( response.getHttpVersion() ).append(SP);
        headerSb.append( response.getStatusCode() ).append(SP);
        headerSb.append( response.getReasonPhrase() ).append(CRLF);
        
       // TODO: 修正 Content-Length
       if ( response.headers.get( HttpHeaderNames.CONTENT_LENGTH ) == null ) {
    	   byte[] content = response.getContent();
    	   if ( content == null ) {
    		   response.headers().put(HttpHeaderNames.CONTENT_LENGTH, "0");
    	   } else {
    		   response.headers().put(HttpHeaderNames.CONTENT_LENGTH, String.valueOf( content.length ) );   
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
		
		// header
    	byte[] header = getHeaderBytes( response );
    	
		// content
    	byte[] content = response.getContent();
        if( content != null ) {
        	byte[] allBytes = new byte[ header.length + content.length  ]; 
	        System.arraycopy(header, 0, allBytes, 0, header.length);
	        System.arraycopy(content, 0, allBytes, header.length, content.length);
	        return allBytes;
	        
        } else {
        	return header;
        }
	}
	
	public ByteBuffer encodeToByteBuffer(HttpResponse response) {
		if ( response == null )
			return null;
		
		// Use direct byte buffer
		ByteBuffer buffer = null;
		try {
	        // 
	    	byte[] header = getHeaderBytes( response );
	    	byte[] content = response.getContent();
	        if( content != null ) {
	        	buffer = NetSystem.getInstance().getBufferPool().allocate( header.length + content.length );
    			buffer.put( header );
    			buffer.put( content );
    			return buffer;
		        
	        } else {
	        	//
	        	buffer = NetSystem.getInstance().getBufferPool().allocate( header.length );
    			buffer.put( header );
    			return buffer;
	        }
	        
		} catch(BufferOverflowException e) {		
			if ( buffer != null )
				NetSystem.getInstance().getBufferPool().recycle( buffer );
			throw e;
		}
	}	
}
