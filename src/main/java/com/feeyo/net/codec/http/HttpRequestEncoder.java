package com.feeyo.net.codec.http;


import java.util.Map;

/**
 * HttpRequest encode
 * 
 * @author zhuam
 *
 */
public class HttpRequestEncoder {
	
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
	
	// encode
	// ----------------------------------------------------------------------------
	public byte[] encode(HttpRequest request) {
		if ( request == null )
			return null;
        // 
    	byte[] header = getHeaderBytes( request );
    	byte[] content = request.getContent();
        if( content != null ) {
        	//
        	byte[] allBytes = new byte[ header.length + content.length  ]; 
	        System.arraycopy(header, 0, allBytes, 0, header.length);
	        System.arraycopy(content, 0, allBytes, header.length, content.length);
	        return allBytes;
	        
        } else {
        	return header;
        }
	}
	
	

}