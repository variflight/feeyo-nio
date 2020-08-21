package com.feeyo.net.codec.http;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.feeyo.net.nio.NetSystem;

public class HttpRequestEncoderV2 {
	
	private static final String CRLF = "\r\n"; 	
	private static final String SP = " "; 		
	private static final String COLON = ":"; 	
	
	private byte[] getHeaderBytes(HttpRequest request) {
		//
		//  httpHeader ( host & headers)
		// ----------------------------------------------------
		StringBuffer headerSb = new StringBuffer();
		headerSb.append( request.getMethod() ).append( SP );
		headerSb.append( UriUtil.parsePath( request.getUri() ) ).append( SP );
		headerSb.append( request.getHttpVersion() ).append(CRLF);
		//
	    for (Map.Entry<String, String> h : request.headers().entrySet()) {
			headerSb.append(h.getKey()).append(COLON).append(SP);
			headerSb.append(h.getValue()).append(CRLF);
		}
	    headerSb.append(CRLF);
	    
	    return headerSb.toString().getBytes();
	}
	
	
	public ByteBuffer encode(HttpRequest request) {
		if ( request == null )
			return null;
		//
		// Use direct byte buffer
		ByteBuffer buffer = null;
		try {
	        // 
	    	byte[] header = getHeaderBytes( request );
	    	byte[] content = request.getContent();
	        if( content != null ) {
	        	buffer = NetSystem.getInstance().getBufferPool().allocate( header.length + content.length );
    			buffer.put( header );
    			buffer.put( content );
    			return buffer;
	        } else {
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
