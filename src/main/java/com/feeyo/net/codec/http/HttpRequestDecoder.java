package com.feeyo.net.codec.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.codec.util.CompositeByteArray;
import com.feeyo.net.codec.util.CompositeByteArray.ByteArrayChunk;

/**
 * Http request parse
 * 
 * @author xuwenfeng
 */
public class HttpRequestDecoder implements Decoder<HttpRequest> {
	
	private static final byte CR = 13; 			// <CR, carriage return (13)>
	private static final byte LF = 10; 			// <LF, linefeed (10)>

	private enum State {
        SKIP_CONTROL_CHARS,
        READ_HEADLINE,
        READ_HEADERS,
        READ_VARIABLE_LENGTH_CONTENT,
        READ_FIXED_LENGTH_CONTENT,
        
        READ_CHUNK_SIZE,
        READ_CHUNKED_CONTENT,
        READ_CHUNK_DELIMITER,
        READ_CHUNK_FOOTER
	}
	
	//
	private CompositeByteArray dataBuffer = new CompositeByteArray();
	private CompositeByteArray transferChunkedBuffer = null;

	private State state;
	private int offset = 0;

	//
	private HttpRequest request;
	private int contentLength = 0;

	public HttpRequestDecoder() {
		this.state = State.SKIP_CONTROL_CHARS;
	}

	public HttpRequest decode(byte[] data) throws UnknownProtocolException {

		if (data == null || data.length == 0)
			return null;

		// append
		dataBuffer.add(data);
		
		//
		for(;;) {
			
			switch (state) {

			case SKIP_CONTROL_CHARS:{
				offset = skipControlCharacters(dataBuffer, offset);
				state = State.READ_HEADLINE;
			}
			
			case READ_HEADLINE:{
				//
				// parse headline
				byte[] line = readLine(dataBuffer, offset);
				if ( line == null)
					throw new IndexOutOfBoundsException("Data not enough");
				else 
					offset += line.length;

				request = HttpRequest.parseFromBytes(line);
				//
				state = State.READ_HEADERS;
			}
			
			case READ_HEADERS:{
				
				if (this.request == null)
					throw new UnknownProtocolException("Http request may be not null");
				
				boolean isHeadersEof = false;
				//
				// parse headers
				for (;;) {
					
					byte[] line = readLine(dataBuffer, offset);
					if (line == null) 
						break;
					else 
						offset += line.length;
					
					if (line.length ==  2 &&  line[0] == CR && line[1] == LF )  {
						isHeadersEof = true;
						break;
					}
					//
					//
					String lineStr = new String(line, UTF_8);
					int ssi = lineStr.indexOf(":");
					if (ssi != -1) {
						String name = lineStr.substring(0, ssi).trim().toLowerCase();
						String value = lineStr.substring(ssi + 1).trim();
						request.addHeader(name, value);
					}
				}
				
				if ( isHeadersEof ) {
					//
					if (request.containsHeader("transfer-encoding", "chunked", true)) {
						state = State.READ_CHUNK_SIZE;
					} else {
						String value = request.headers().get("content-length");
						contentLength = value != null ? Integer.parseInt(value) : -1;
						if (contentLength >= 0) {
							state = State.READ_FIXED_LENGTH_CONTENT;
						} else {
							state = State.READ_VARIABLE_LENGTH_CONTENT;
						}
					}
					//
					continue;
					
				} else {
					throw new IndexOutOfBoundsException("Not enough data.");
				}
			}
			
			case READ_FIXED_LENGTH_CONTENT:{
				//
				if (request == null)
					throw new UnknownProtocolException("Http request may be not null");

				if (contentLength <= 0)
					throw new UnknownProtocolException("Http content-length may be not less zero");

				//
				int len = dataBuffer.getByteCount() - offset;
				if( contentLength <= len ) {
					byte[] content = dataBuffer.getData(offset, contentLength);
					request.setContent( content );
					//
					reset();
					return request;
					
				} else {
					return null; 	// data not enough;
				}
			}
				
			case READ_VARIABLE_LENGTH_CONTENT:{
				//
				if (request == null) 
					throw new UnknownProtocolException("Http request may be not null");
				//
				int variableContentLength = dataBuffer.getByteCount() - offset;
				if( variableContentLength > 0) {
					byte[] content = dataBuffer.getData(offset, variableContentLength);
					request.setContent(content);
				}
				//
				reset();
				return request;
			}
				
			case READ_CHUNK_SIZE:{
				//
				if( transferChunkedBuffer == null) 
					transferChunkedBuffer = new CompositeByteArray();
				//
				byte[] chunkSizeHex = readLine(dataBuffer, offset);
				if(chunkSizeHex == null) {
					return null;
				} else {
					offset += chunkSizeHex.length;
					contentLength = Integer.parseInt(new String(chunkSizeHex, UTF_8).trim(), 16);
		        	state = contentLength == 0 ? State.READ_CHUNK_FOOTER : State.READ_CHUNKED_CONTENT;
				}
		        continue;
			}
			
			case READ_CHUNKED_CONTENT:{
				//
				if( contentLength <= 0 || dataBuffer.getByteCount() - offset < contentLength )
					return null;
				
				byte[] chunked = dataBuffer.getData(offset, contentLength);
				if( chunked == null ) {
					return null;
				} else {
					transferChunkedBuffer.add(chunked);
					offset += contentLength;
					state = State.READ_CHUNK_DELIMITER;
				}
				//
	            continue;
			}
			
			case READ_CHUNK_DELIMITER:{
				//delimiter - CRLF
				byte[] delimiter = readLine(dataBuffer, offset);
				if (delimiter == null) {
					return null;
				} else  {
					offset += delimiter.length;
					state = State.READ_CHUNK_SIZE;
				}
				continue;
			}
				
			case READ_CHUNK_FOOTER:{
				byte[] content = transferChunkedBuffer.getData(0, transferChunkedBuffer.getByteCount());
				request.setContent( content );
				//
				reset();
				return request;
			}
			default:
				reset();
				return request;
			}
		}
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

	private byte[] readLine(CompositeByteArray dataBuffer, int offset) {
		// find end offset of line
		int end = findLF(dataBuffer, offset);
		if (end != -1) {
			int length = end + 1 - offset;
			return dataBuffer.getData(offset, length);
		}
		return null;
	}
	
	//
	private void reset() {
		if (dataBuffer != null)
			dataBuffer.clear();

		if (transferChunkedBuffer != null)
			transferChunkedBuffer.clear();

		offset = 0;
		state = State.SKIP_CONTROL_CHARS;
	}

}