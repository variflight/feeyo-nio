package com.feeyo.net.codec.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.util.CompositeByteArray;
import com.feeyo.net.codec.util.CompositeByteArray.ByteArrayChunk;
import com.feeyo.util.internal.Utf8Util;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Bytes;

/**
 * Http request parse lite
 * 
 * @see https://www.w3.org/Protocols/rfc2616/rfc2616.html
 * 
 * @author zhuam
 */
public class HttpRequestPipeliningDecoder implements Decoder<List<HttpRequest>> {
	
	private static final byte CR = 13; 			// <CR, carriage return (13)>
	private static final byte LF = 10; 			// <LF, linefeed (10)>
	
	
	/*
	 *   Method         = "OPTIONS"                ; Section 9.2
     *                  | "GET"                    ; Section 9.3
     *                  | "HEAD"                   ; Section 9.4
     *                  | "POST"                   ; Section 9.5
     *                  | "PUT"                    ; Section 9.6
     *                  | "DELETE"                 ; Section 9.7
     *                  | "TRACE"                  ; Section 9.8
     *                  | "CONNECT"                ; Section 9.9
     *                  | extension-method
     *   extension-method = token
	 */
	private static final Set<String> METHOD_SET = ImmutableSet.of(
			"OPTIONS", "GET", "HEAD", "POST", 
			"PUT", "DELETE", "TRACE", "CONNECT");
	
	private static final int MAX_PACKET_SIZE = 1024 * 1024 * 32;


	private enum State {
		SKIP_CONTROL_CHARS,
        READ_HEADLINE,
        READ_HEADERS,
        READ_FIXED_LENGTH_CONTENT
	}
	
	//
	private CompositeByteArray dataBuffer = new CompositeByteArray();
	private int readOffset = 0;
	
	public void setReadOffset(int offset) {
		this.readOffset = offset;
	}

	public List<HttpRequest> decode(byte[] data) throws UnknownProtocolException {
		//
		if (data == null || data.length == 0)
			return null;
		
		// append
		dataBuffer.add(data);
		
		//
		if ( dataBuffer.getByteCount() > MAX_PACKET_SIZE )
			throw new UnknownProtocolException("Packets exceed " + MAX_PACKET_SIZE + " bytes, readOffset=" + readOffset);
		
		//
		List<HttpRequest> requestList = null;
		
		//
		try {
			
			int offset = readOffset;
			
			//
			HttpRequest request = null;
			int contentLength = -1;
			State state = State.SKIP_CONTROL_CHARS;
			
			//
			for(;;) {
				//
				if ( state == State.SKIP_CONTROL_CHARS ) {
					// skip control chars
					offset = this.skipControlCharacters(dataBuffer, offset);
					state = State.READ_HEADLINE;
					
				} else if ( state == State.READ_HEADLINE ) {
					// parse headline
					byte[] line = null;
					//
					int end = this.findLF(dataBuffer, offset);
					if (end != -1) {
						line = dataBuffer.getData(offset, end + 1 - offset);
						offset = end + 1;
					}
					
					if ( line == null )
						break;

                    request = HttpRequest.parseFromBytes(line);
					if ( !METHOD_SET.contains( request.getMethod() ) )
						throw new UnknownProtocolException("Invalid method: " + request);

					//
					state = State.READ_HEADERS;
					continue;
					
				} else if ( state ==  State.READ_HEADERS ) {
					
					/*
					 * @see http://tools.ietf.org/html/rfc2616#section-5
					 * 
					 * Request       =    Request-Line              ; Section 5.1
					 *                    *(( general-header        ; Section 4.5
					 *                     | request-header         ; Section 5.3
					 *                     | entity-header ) CRLF)  ; Section 7.1
					 *                    CRLF
					 *                    [ message-body ]          ; Section 4.3
					 */
					
					boolean isHeadersEof = false;
					
					// parse headers
					for (;;) {
						byte[] line = null;
						//
						int end = this.findLF(dataBuffer, offset);
						if (end != -1) {
							line = dataBuffer.getData(offset, end + 1 - offset);
							offset = end + 1;
						}
						
						if ( line == null )
							break;
						//
						if ( line.length == 2 && line[0] == CR && line[1] == LF )  {
							isHeadersEof = true;
							break;
						}

                        int ssi = Bytes.indexOf(line, (byte) ':');
                        if (ssi > -1) {
                            String name = Utf8Util.readUtf8AndTrim(line, 0, ssi);
                            String value = Utf8Util.readUtf8AndTrim(line, ssi + 1, line.length);
                            request.addHeader(name, value);
                        }
					}

					if ( isHeadersEof ) {
						//
						contentLength = request.getContentLength();
						if (contentLength >= 0) 
							state = State.READ_FIXED_LENGTH_CONTENT;
						else
							throw new UnknownProtocolException("content-length less than zero!");
						continue;
						
					}  else {
						//
						throw new IndexOutOfBoundsException("Not enough data.");
					}
					
					
				} else if ( state == State.READ_FIXED_LENGTH_CONTENT ) {
					//
					int len = dataBuffer.getByteCount() - offset;
					if( contentLength > len ) {
						throw new IndexOutOfBoundsException("Not enough data.");
						
					} else {
						
						if ( requestList == null )
							requestList = new ArrayList<>(4);
						//
						if ( contentLength > 0 ) {				
							byte[] content = dataBuffer.getData(offset, contentLength);
							request.setContent( content );
							//
							offset += contentLength;
						} 
						//
						requestList.add( request );
						
						
						//
						int totalLength = dataBuffer.getByteCount();
						int remainingLength = totalLength - offset;
						if ( remainingLength > 0 ) {
							//
							// Balanced performance and memory heap	
							// 128kb & usage ratio > 0.55
							if ( totalLength > 131072 && offset != 0 && (offset * 1.0F / totalLength) > 0.55F ) {
								//
								CompositeByteArray oldBuffer = dataBuffer;
								CompositeByteArray newBuffer = new CompositeByteArray();
								newBuffer.add( oldBuffer.getData(offset, remainingLength) );
								
								oldBuffer.clear();
								oldBuffer = null;
								dataBuffer = newBuffer;
								
								// reset
								request = null;
								contentLength = -1;
								offset = 0;
								readOffset = 0;
								state = State.SKIP_CONTROL_CHARS;
								
							} else {
								// reset
								request = null;
								contentLength = -1;
								readOffset = offset;
								state = State.SKIP_CONTROL_CHARS;
							}
							
							//
							continue;
							
						} else {
							// reset
							dataBuffer.clear();
							readOffset = 0;
							state = State.SKIP_CONTROL_CHARS;
							break;
						}
					} 
				}
			}
			
		} catch (IndexOutOfBoundsException e) {
			// ignore IndexOutOfBoundsException
		}
		
		return requestList;
	}
	
	// skip control characters
	private int skipControlCharacters(CompositeByteArray dataBuffer, int offset) {
		//
		ByteArrayChunk chunk = dataBuffer.findChunk( offset );
		while (dataBuffer.getByteCount() > offset) {
			//
            int c = chunk.get(offset++) & 0xFF;
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
            	offset--;
                break;
            }
            
        	// Boundary check
			if ( !chunk.isInBoundary( offset ) )
				chunk = chunk.getNext();
			
			if ( chunk == null)
				break;
            
        }
		return offset;
	}
	
	// find LF
	private int findLF(CompositeByteArray dataBuffer, int offset) {
		// find end offset of line 
		int end = -1;
		//
		ByteArrayChunk chunk = dataBuffer.findChunk( offset );
		for (int i = offset; i < dataBuffer.getByteCount(); i++) {
			//
			if (chunk.get(i) == LF) {
				end = i;
				break;
			}
			
			// Boundary check 
			if ( !chunk.isInBoundary(i) )
				chunk = chunk.getNext();
			
			if ( chunk == null)
				break;
		}
		return end;
	}
	
	//
	public byte[] getData() {
		int length = dataBuffer.getByteCount();
		return dataBuffer.getData(0, length);
	}

	public int getBufferSize() {
		return dataBuffer.getByteCount();
	}
	
	public int getReadOffset() {
		return readOffset;
	}
}