package com.feeyo.net.codec.http2;

import java.io.IOException;
import java.util.List;


/**
 *  HTTP/2 Codec
 *  
 */
public class Http2RequestDecoder {
	
	private byte[] data = null;
	private int offset;
	
	private Listener listener;
    
    
	public Http2RequestDecoder(Listener listener) {
		//
		this.listener = listener;
	}
	
	public void handle(byte[] buf, int client_preface_offset) throws IOException {
		
		if ( this.data == null ) {
			this.data = buf;
			this.offset = client_preface_offset;
			
		} else {
			
			int oldDataLen = this.data.length - this.offset;
			if ( oldDataLen > 0 ) {
				byte[] newData = new byte[ oldDataLen + buf.length ];
				System.arraycopy(this.data, this.offset, newData, 0, oldDataLen);
				System.arraycopy(buf, 0, newData, oldDataLen, buf.length);
				this.data = newData;
				
			} else {
				this.data = buf;
			}
			
			this.offset = 0;
		}
		
		//
		while( nextFrame() ) { 
			System.out.println("offset = " + offset + ", length = " + this.data.length);
			/* ignore */ 
		}
		
	}
	
	private boolean nextFrame() throws IOException {
		
		// frame header size
		if ( data.length < 9 )
			return false;
		
		// none
		if ( data.length <= offset )
			return false;
		

		//  0                   1                   2                   3
	    //  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	    // |                 Length (24)                   |
	    // +---------------+---------------+---------------+
	    // |   Type (8)    |   Flags (8)   |
	    // +-+-+-----------+---------------+-------------------------------+
	    // |R|                 Stream Identifier (31)                      |
	    // +=+=============================================================+
	    // |                   Frame Payload (0...)                      ...
	    // +---------------------------------------------------------------+
		
		int length = (data[offset] & 0xff) << 16 
				| (data[offset + 1] & 0xff) << 8 
				| (data[offset + 2] & 0xff);
		offset += 3;
		
		if (length < 0 ||length > FrameType.INITIAL_MAX_FRAME_SIZE) {
			throw Util.ioException("FRAME_SIZE_ERROR: %s", length);
		}
		
		byte type = (byte) (data[offset] & 0xff);
		offset++;
		
		byte flags = (byte) ( data[offset] & 0xff);
		offset++;
		
		// Ignore reserved bit.
		int streamId = ((data[offset] & 0xff) << 24 
				| (data[offset+1] & 0xff) << 16 
				| (data[offset+2] & 0xff) << 8
				| (data[offset+3] & 0xff)) & 0x7fffffff;
		offset += 4;
		
		System.out.println("#frame header, streamId=" + streamId + ", length=" + length + ", type=" + type + ", flags=" + flags);
		
		
		switch (type) {
		case FrameType.DATA:
			readData(length, flags, streamId);
			break;

		case FrameType.HEADERS:
			readHeaders(length, flags, streamId);
			break;

		case FrameType.PRIORITY:
			readPriority(length, flags, streamId);
			break;

		case FrameType.RST_STREAM:
			readRstStream(length, flags, streamId);
			break;

		case FrameType.SETTINGS:
			readSettings(length, flags, streamId);
			break;

		case FrameType.PUSH_PROMISE:
			readPushPromise(length, flags, streamId);
			break;

		case FrameType.PING:
			readPing(length, flags, streamId);
			break;

		case FrameType.GOAWAY:
			readGoAway(length, flags, streamId);
			break;

		case FrameType.WINDOW_UPDATE:
			readWindowUpdate(length, flags, streamId);
			break;
		}
		
		return true;
	}

	// read frames
	//
	private void readData(int length, byte flags, int streamId) throws IOException {

		if (streamId == 0)
			throw Util.ioException("PROTOCOL_ERROR: TYPE_DATA streamId == 0");

		// TODO: checkState open or half-closed (local) or raise STREAM_CLOSED
		boolean inFinished = (flags & Flags.END_STREAM) != 0;
		boolean gzipped = (flags & Flags.COMPRESSED) != 0;
		if (gzipped) {
			throw Util.ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA");
		}
		
		short padding = 0;
		if ( (flags & Flags.PADDED) != 0 ) {
			padding = (short) (data[offset] & 0xff);
			offset++;
		}
		
		length = lengthWithoutPadding(length, flags, padding);
		

		byte[] dest = new byte[ length ];
		System.arraycopy(data, offset, dest, 0, length);
		offset += length;
		
		this.listener.onData(inFinished, streamId, dest, length);
		
		// skip padding
		offset += padding;
	}

	private void readHeaders(int length, byte flags, int streamId) throws IOException {
		
		if (streamId == 0) throw 
			Util.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0");
		
	    boolean endStream = (flags & Flags.END_STREAM) != 0;
	    
	    short padding = 0;
		if ( (flags & Flags.PADDED) != 0 ) {
			padding = (short) (data[offset] & 0xff);
			offset++;
		}
		
		if ((flags & Flags.PRIORITY) != 0) {
			
			// priority
			int w1 = ((data[offset] & 0xff) << 24 
					| (data[offset + 1] & 0xff) << 16 
					| (data[offset + 2] & 0xff) << 8
					| (data[offset + 3] & 0xff));
			offset += 4;

			boolean exclusive = (w1 & 0x80000000) != 0;
			int streamDependency = (w1 & 0x7fffffff);
			int weight = (data[offset] & 0xff) + 1;
			offset++;

			//
			this.listener.onPriority(streamId, streamDependency, weight, exclusive);
			length -= 5; // account for above read.
		}
		
		List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);
		this.listener.onHeaders(endStream, streamId, -1, headerBlock);

	}
	
	private List<Header> readHeaderBlock(int length, short padding, byte flags, int streamId) throws IOException {
		
//		continuation.length = continuation.left = length;
//		continuation.padding = padding;
//		continuation.flags = flags;
//		continuation.streamId = streamId;
		

		// TODO: Concat multi-value headers with 0x0, except COOKIE, which uses
		// 0x3B, 0x20.
		// http://tools.ietf.org/html/draft-ietf-httpbis-http2-17#section-8.1.2.5
		byte[] headData = new byte[ length ];
		System.arraycopy(data, offset, headData, 0, headData.length);
		offset += length;
		
		HpackReader hpackReader = new HpackReader(4096, headData);
		hpackReader.readHeaders();
		return hpackReader.getAndResetHeaderList();
	}

	private void readPriority(int length, byte flags, int streamId) throws IOException {
		
		if (length != 5)
			throw Util.ioException("TYPE_PRIORITY length: %d != 5", length);
		
		if (streamId == 0)
			throw Util.ioException("TYPE_PRIORITY streamId == 0");
		
		int w1 = ((data[offset] & 0xff) << 24 
				| (data[offset + 1] & 0xff) << 16 
				| (data[offset + 2] & 0xff) << 8
				| (data[offset + 3] & 0xff));
		offset += 4;

		boolean exclusive = (w1 & 0x80000000) != 0;
		int streamDependency = (w1 & 0x7fffffff);
		int weight = (data[offset] & 0xff) + 1;
		offset++;

		//
		this.listener.onPriority(streamId, streamDependency, weight, exclusive);
	}

	
	private void readRstStream(int length, byte flags, int streamId) throws IOException {
		
		if (length != 4) throw 
			Util.ioException("TYPE_RST_STREAM length: %d != 4", length);
		
	    if (streamId == 0) 
	    	throw Util.ioException("TYPE_RST_STREAM streamId == 0");
	    
		int errorCode = ((data[offset] & 0xff) << 24 
				| (data[offset + 1] & 0xff) << 16
				| (data[offset + 2] & 0xff) << 8 
				| (data[offset + 3] & 0xff));
		offset += 4;
		
	    if ( !ErrorCode.check(errorCode) ) {
	      throw Util.ioException("TYPE_RST_STREAM unexpected error code: %d", errorCode);
	    }
	    
	    this.listener.onRstStream(streamId, errorCode);
	}

	private void readSettings(int length, byte flags, int streamId) throws IOException {
		
		if (streamId != 0)
			throw Util.ioException("TYPE_SETTINGS streamId != 0");
		
		if ((flags & Flags.ACK) != 0) {
			if (length != 0)
				throw Util.ioException("FRAME_SIZE_ERROR ack frame should be empty!");
			//
			this.listener.onAckSettings();
			return;
		}
		
		//
		if (length % 6 != 0) 
			throw Util.ioException("TYPE_SETTINGS length %% 6 != 0: %s", length);
		
	    Settings settings = new Settings();
	    for (int i = 0; i < length; i += 6) {
	    	
			int id = ((data[offset] & 0xff) << 8 
					| (data[offset + 1] & 0xff)) & 0xFFFF;
			offset += 2;

			int value = ((data[offset] & 0xff) << 24 
					| (data[offset + 1] & 0xff) << 16 
					| (data[offset + 2] & 0xff) << 8
					| (data[offset + 3] & 0xff));
			offset += 4;

			switch (id) {
			case 1: // SETTINGS_HEADER_TABLE_SIZE
				break;
			case 2: // SETTINGS_ENABLE_PUSH
				if (value != 0 && value != 1) {
					throw Util.ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1");
				}
				break;
			case 3: // SETTINGS_MAX_CONCURRENT_STREAMS
				id = 4; // Renumbered in draft 10.
				break;
			case 4: // SETTINGS_INITIAL_WINDOW_SIZE
				id = 7; // Renumbered in draft 10.
				if (value < 0) {
					throw Util.ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1");
				}
				break;
			case 5: // SETTINGS_MAX_FRAME_SIZE
				if (value < FrameType.INITIAL_MAX_FRAME_SIZE || value > 16777215) {
					throw Util.ioException("PROTOCOL_ERROR SETTINGS_MAX_FRAME_SIZE: %s", value);
				}
				break;
			case 6: // SETTINGS_MAX_HEADER_LIST_SIZE
				break; // Advisory only, so ignored.
			default:
				break; // Must ignore setting with unknown id.
			}
			settings.set(id, value);
	    }
	    
		// 
	    this.listener.onSettings(false, settings);
	}

	private void readPushPromise(int length, byte flags, int streamId) throws IOException {

		if (streamId == 0) {
			throw Util.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0");
		}
		
		short padding = 0;
		if ( (flags & Flags.PADDED) != 0 ) {
			padding = (short) (data[offset] & 0xff);
			offset++;
		}
		
		int promisedStreamId = ((data[offset] & 0xff) << 24 
				| (data[offset] & 0xff) << 16
				| (data[offset + 1] & 0xff) << 8 
				| (data[offset + 2] & 0xff)) & 0x7fffffff;
		offset += 4;
		
		length -= 4; // account for above read.
		length = lengthWithoutPadding(length, flags, padding);
		List<Header> headerBlock = readHeaderBlock(length, padding, flags, streamId);
		
		//
		this.listener.onPushPromise(streamId, promisedStreamId, headerBlock);
	}

	private void readPing(int length, byte flags, int streamId) throws IOException {
		
		if (length != 8)
			throw Util.ioException("TYPE_PING length != 8: %s", length);
		
		if (streamId != 0)
			throw Util.ioException("TYPE_PING streamId != 0");

		int payload1 = ((data[offset] & 0xff) << 24 
				| (data[offset + 1] & 0xff) << 16 
				| (data[offset + 2] & 0xff) << 8
				| (data[offset + 3] & 0xff));
		offset += 4;

		int payload2 = ((data[offset] & 0xff) << 24
				| (data[offset + 1] & 0xff) << 16 
				| (data[offset + 2] & 0xff) << 8
				| (data[offset + 3] & 0xff));
		offset += 4;
		
	    boolean ack = (flags & Flags.ACK) != 0;
	    
	    //
	    this.listener.onPing(ack, payload1, payload2);
	}

	private void readGoAway(int length, byte flags, int streamId) throws IOException {

		if (length < 8)
			throw Util.ioException("TYPE_GOAWAY length < 8: %s", length);
		
		if (streamId != 0)
			throw Util.ioException("TYPE_GOAWAY streamId != 0");
		
		int lastStreamId = ((data[offset] & 0xff) << 24 
				| (data[offset + 1] & 0xff) << 16
				| (data[offset + 2] & 0xff) << 8 
				| (data[offset + 3] & 0xff));
		offset += 4;

		int errorCode = ((data[offset] & 0xff) << 24 
				| (data[offset + 1] & 0xff) << 16
				| (data[offset + 2] & 0xff) << 8 
				| (data[offset + 3] & 0xff));
		offset += 4;

		int opaqueDataLength = length - 8;
		
		if ( !ErrorCode.check(errorCode)  ) {
			throw Util.ioException("TYPE_GOAWAY unexpected error code: %d", errorCode);
		}
		
		byte[] debugData = null;
		if (opaqueDataLength > 0) { // Must read debug data in order to not
									// corrupt the connection.
			debugData = new byte[ opaqueDataLength ];
			System.arraycopy(data, offset, debugData, 0, debugData.length);
			offset += opaqueDataLength;
		}
		
		//
		this.listener.onGoAway(lastStreamId, errorCode, debugData);
	}

	private void readWindowUpdate(int length, byte flags, int streamId) throws IOException {

		if (length != 4)
			throw Util.ioException("TYPE_WINDOW_UPDATE length !=4: %s", length);
		
		int w1 = ((data[offset] & 0xff) << 24 
				| (data[offset+1] & 0xff) << 16 
				| (data[offset+2] & 0xff) << 8
				| (data[offset+3] & 0xff));
		offset += 4;
		
		long increment = ( w1 & 0x7fffffffL);
		if (increment == 0)
			throw Util.ioException("windowSizeIncrement was 0", increment);
		
		// 
		this.listener.onWindowUpdate(streamId, increment);
	}
	
	//
	private int lengthWithoutPadding(int length, byte flags, short padding) throws IOException {
		if ((flags & Flags.PADDED) != 0)
			length--; // Account for reading the padding length.
		
		if (padding > length) {
			throw Util.ioException("PROTOCOL_ERROR padding %s > remaining length %s", padding, length);
		}
		return (short) (length - padding);
	}
	
	
	// 
	//
	public interface Listener {

		void onData(boolean inFinished, int streamId, byte[] data, int length) throws IOException;
		void onHeaders(boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock);
		void onRstStream(int streamId, int errorCode);
		void onSettings(boolean clearPrevious, Settings settings);
		void onAckSettings();
		void onPing(boolean ack, int payload1, int payload2);
		void onGoAway(int lastGoodStreamId, int errorCode, byte[] debugData);
		void onWindowUpdate(int streamId, long windowSizeIncrement);
		void onPriority(int streamId, int streamDependency, int weight, boolean exclusive);
		void onPushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException;
	}
	
}