package com.feeyo.net.codec.http;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.util.CompositeByteArray;
import com.feeyo.net.codec.util.CompositeByteArray.ByteArrayChunk;
import com.feeyo.util.internal.Utf8Util;
import com.google.common.primitives.Bytes;

/**
 * HttpResponse parse
 * 
 */
public class HttpResponseDecoder implements Decoder<HttpResponse> {
	
	private static final byte CR = 13; 			// <CR, carriage return (13)>
	private static final byte LF = 10; 			// <LF, linefeed (10)>

	private enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        READ_FIXED_LENGTH_CONTENT
	}
	
	//
	private CompositeByteArray readBuffer = new CompositeByteArray();
	private ByteArrayChunk readChunk = null;
	private int readIndex = 0;
	
	//
	private int contentLength = -1;

	//
	private State state;
	private HttpResponse response;

	public HttpResponseDecoder() {
		this.state = State.SKIP_CONTROL_CHARS;
	}
	
	
	/**
	 HTTP/1.1 200 OK
	 Content-Length: 2
		
	 ok
	 */
	public HttpResponse decode(byte[] data) {
		//
		if ( data == null || data.length == 0)
			return null;
		//
		readBuffer.add( data );
		//
		readChunk = readBuffer.findChunk(readIndex);
		//
		for(;;) {
			switch (state) {
			case SKIP_CONTROL_CHARS:{
				//
				// skip control characters
				while ( readBuffer.remaining( readIndex ) > 0 ) {
					int c = readChunk.get(readIndex++) & 0xFF;
					if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
						readIndex--;
						break;
					}
					//
					tryGetNextReadChunk(readIndex);
				}
				
				this.state = State.READ_INITIAL;
			}
			case READ_INITIAL:{
				// parse headline
				byte[] lineByteArray = readLine();
				if (lineByteArray == null)
					throw new RuntimeException("Data not enough");
				//
				response = HttpResponse.parseFromBytes(lineByteArray);
				//
				this.state = State.READ_HEADER;
			}
			case READ_HEADER:{
				if (this.response == null) {
					throw new IllegalArgumentException("Http response may be not null");
				}
				//
				// parse headers
				for (;;) {
					byte[] lineByteArray = readLine();
					if ( lineByteArray == null ) 
						break;
					if ( lineByteArray[0] == LF || (lineByteArray[0] == CR && lineByteArray[1] == LF) ) 
						break;

					//
					int ssi = Bytes.indexOf(lineByteArray, (byte) ':');
					if (ssi > -1) {
						String name = Utf8Util.readUtf8AndTrim(lineByteArray, 0, ssi);
						String value = Utf8Util.readUtf8AndTrim(lineByteArray, ssi + 1, lineByteArray.length);
                        response.addHeader(name, value);
					}
				}

				contentLength = response.getContentLength();
				if (contentLength >= 0) {
					state = State.READ_FIXED_LENGTH_CONTENT;
				}
				
				continue;
			}
			case READ_FIXED_LENGTH_CONTENT:{
				if (response == null)
					throw new IllegalArgumentException("Http response may be not null");

				if (contentLength < 0)
					throw new IllegalArgumentException("Http content may be not than zero");
				//
				int residueLength = readBuffer.remaining( readIndex );
				if( contentLength <= residueLength) {
					//
					if ( contentLength == residueLength && contentLength == 0 ) {
						reset();
						return response;
					}
					
					byte[] content = readBuffer.getData(readIndex, contentLength);
					response.setContent( content );
					reset();
					return response;
				} else {
					return null; 	// data not enough;
				}
			}
			default:
				reset();
				return response;
			}
		}
	}
	
	// 在遍历中改变 readIndex 可能需要更新 readByteChunk
    private void tryGetNextReadChunk(int index) {
    	 for(;;) {
         	if ( readChunk == null )
         		break;
         	
         	// 当 readIndex 达到最大长度时也不继续,防止空指针异常
     		if ( readBuffer.getByteCount() == index )
     			break;
     		
     		if ( readChunk.isInBoundary(index) ) 
     			break;
     		//
             readChunk = readChunk.getNext();
         }
    }

	private byte[] readLine() {
		int pos = -1;
		byte[] lineByteArray = null;
		//
		for (int idx = readIndex; idx < readBuffer.getByteCount(); idx++) {
			//
			tryGetNextReadChunk(idx);
			//
			if (readChunk.get(idx) == LF) {
				pos = idx;
				break;
			}
		}

		if (pos != -1) {
			// end of line found
			final int len = pos + 1 - readIndex;
			lineByteArray = readBuffer.getData(readIndex, len);
			readIndex = pos + 1;
		}
		//
		tryGetNextReadChunk(readIndex);
		return lineByteArray;
	}
	
	private void reset() {
		if (readBuffer != null)
			readBuffer.clear();
		//
		readIndex = 0;
		state = State.SKIP_CONTROL_CHARS;
	}
}
