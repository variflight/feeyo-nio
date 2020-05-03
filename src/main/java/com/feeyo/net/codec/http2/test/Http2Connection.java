package com.feeyo.net.codec.http2.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.DatatypeConverter;

import com.feeyo.net.codec.http2.ErrorCode;
import com.feeyo.net.codec.http2.FrameType;
import com.feeyo.net.codec.http2.Header;
import com.feeyo.net.codec.http2.Http2RequestDecoder;
import com.feeyo.net.codec.http2.Http2ResponseEncoder;
import com.feeyo.net.codec.http2.Http2Stream;
import com.feeyo.net.codec.http2.Settings;
import com.feeyo.net.nio.Connection;
import com.feeyo.net.nio.util.ByteUtil;

/**
 *  HTTP/2 connection 
 *  
 * @author zhuam
 */
public class Http2Connection extends Connection implements Http2RequestDecoder.Listener {
	
	
	public static final int HTTP_CLIENT_WINDOW_SIZE = 16 * 1024 * 1024;
	
	//
	private final Map<Integer, Http2Stream> streams = new ConcurrentHashMap<Integer, Http2Stream>();
	private int lastGoodStreamId;
	private int nextStreamId;
	
	
	private long bytesLeftInWriteWindow;
	private Settings remoteSettings;
	private Settings localSettings;
	
	private long unacknowledgedBytesRead = 0;
	
	private Http2ResponseEncoder encoder = new Http2ResponseEncoder();
	
	
	//
	//
	public Http2Connection(SocketChannel socketChannel) {
		super(socketChannel);

		// http://tools.ietf.org/html/draft-ietf-httpbis-http2-17#section-5.1.1
		this.nextStreamId = 2;
		
		this.remoteSettings = new Settings();
		this.localSettings = new Settings();
		
		this.localSettings.set(Settings.INITIAL_WINDOW_SIZE, Settings.DEFAULT_INITIAL_WINDOW_SIZE);
		this.localSettings.set(Settings.MAX_FRAME_SIZE, FrameType.INITIAL_MAX_FRAME_SIZE);
		this.localSettings.set(Settings.MAX_CONCURRENT_STREAMS, 100);
		this.localSettings.set(Settings.HEADER_TABLE_SIZE, 8192);
		
		this.bytesLeftInWriteWindow = localSettings.getInitialWindowSize();
	}
	
	//
	boolean pushedStream(int streamId) {
		return streamId != 0 && (streamId & 1) == 0;
	}
	
	
	public Http2Stream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
		return newStream(associatedStreamId, requestHeaders, out);
	}
	
	public Http2Stream newStream(List<Header> requestHeaders, boolean out) throws IOException {
		return newStream(0, requestHeaders, out);
	}

	private Http2Stream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
		boolean outFinished = !out;
		boolean inFinished = false;

		Http2Stream stream;
		int streamId;

		if (nextStreamId > Integer.MAX_VALUE / 2) {
			close( "refused stream !!!" );
		}
		
		if ( isClosed.get() ) {
			throw new IOException("connection is shutdown !!!");
		}
		
		streamId = nextStreamId;
		nextStreamId += 2;
		stream = new Http2Stream(streamId, this, outFinished, inFinished, null);

		if (stream.isOpen()) {
			streams.put(streamId, stream);
		}
		
		//
		if (associatedStreamId == 0) {
			ByteBuffer buf = encoder.encodeSynStreamFrame(outFinished, streamId, associatedStreamId, requestHeaders);
			this.write( buf );
			
		} else { 
			// HTTP/2 has a PUSH_PROMISE frame.
			ByteBuffer buf = encoder.encodePushPromiseFrame(associatedStreamId, streamId, requestHeaders);
			this.write( buf );
		}
		
		return stream;
	}
	
	public int getStreamCount() {
		return streams.size();
	}

	public Http2Stream getStream(int id) {
		return streams.get(id);
	}

	public Http2Stream removeStream(int streamId) {
		Http2Stream stream = streams.remove(streamId);
		return stream;
	}
	
	public int maxConcurrentStreams() {
		return localSettings.getMaxConcurrentStreams(Integer.MAX_VALUE);
	}
	
	@Override
	public void close(String reason) {
		//
		if (!isClosed.get()) {
			try {
				ByteBuffer buf = encoder.encodeGoAwayFrame(lastGoodStreamId, ErrorCode.REFUSED_STREAM_ERROR, new byte[0]);
				this.write( buf );
			} catch (IOException e) {
				// ignore
			}
		}
		super.close(reason);
	}
	
	//
	public void setRemoteSettings(String base64Settings) {
		
		int offset = 0;
		byte[] data = DatatypeConverter.parseBase64Binary( base64Settings );

		for (int i = 0; i < data.length; i += 6) {
			int id = ((data[offset] & 0xff) << 8 | (data[offset + 1] & 0xff)) & 0xFFFF;
			offset += 2;
			int value = ((data[offset] & 0xff) << 24 
					| (data[offset + 1] & 0xff) << 16 
					| (data[offset + 2] & 0xff) << 8 
					| (data[offset + 3] & 0xff));
			offset += 4;
			remoteSettings.set(id, value);
		}
	}
	
	//
	public void writeWindowUpdate() {
		ByteBuffer buf = encoder.encodeWindowUpdateFrame(0, 2147418112);
		this.write( buf );
	}
	
	//
	public void writeSettings() {
		ByteBuffer buf = encoder.encodeSettingsFrame(localSettings);
		this.write( buf );
	}
	
	public void writeSynReply(int streamId, boolean outFinished, List<Header> alternating) 
			throws IOException {
		ByteBuffer buf = encoder.encodeHeadersFrame(outFinished, streamId, alternating);
		this.write(buf);
	}
	
	
	// Writes are subject to the write window of the stream and the connection
	//
	public void writeData(int streamId, boolean outFinished, byte[] data, long byteCount)
		      throws IOException {
		
		if (byteCount == 0) { 
			ByteBuffer buf = encoder.encodeDataFrame(outFinished, streamId, data, 0);
			this.write(buf);
			
		} else  {
			
			// flow-controlled
			while (byteCount > 0) {
				int toWrite;

				if (bytesLeftInWriteWindow <= 0) {
					if (!streams.containsKey(streamId)) {
						throw new IOException("stream closed");
					}
				}
	
				toWrite = (int) Math.min(byteCount, bytesLeftInWriteWindow);
				toWrite = Math.min(toWrite, encoder.getMaxFrameSize());
				bytesLeftInWriteWindow -= toWrite;

				byteCount -= toWrite;
				ByteBuffer buf = encoder.encodeDataFrame(outFinished && byteCount == 0, streamId, data, toWrite);
				this.write(buf);
			}
		}
	}
	

	//  Process http2 frames
	//
	//
	@Override
	public void onData(boolean inFinished, int streamId, byte[] data, int length) throws IOException {

		if (pushedStream(streamId)) {
			// pushDataLater(streamId, source, length, inFinished);
			return;
		}

		Http2Stream stream = getStream(streamId);
		if (stream == null) {
			// writeSynReset
			ByteBuffer rstBuf = encoder.encodeRstStreamFrame(streamId, ErrorCode.PROTOCOL_ERROR);
			this.write(rstBuf);

			// updateConnectionFlowControl
			unacknowledgedBytesRead += length;
			if (unacknowledgedBytesRead >= remoteSettings.getInitialWindowSize() / 2) {
				//
				ByteBuffer windowUpdateBuf = encoder.encodeWindowUpdateFrame(0, unacknowledgedBytesRead);
				this.write(windowUpdateBuf);

				unacknowledgedBytesRead = 0;
			}

			// skip length;
			return;
		}

		stream.receiveData(data, length);
		
		if (inFinished) {
			stream.receiveFin();
		}
	}


	@Override
	public void onHeaders(boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock) {

		//
		if (pushedStream(streamId)) {
			return;
		}

		Http2Stream stream = getStream(streamId);
		if (stream == null) {
			// If the stream ID is less than the last created ID, assume it's already closed.
			if (streamId <= lastGoodStreamId)
				return;

			// If the stream ID is in the client's namespace, assume it's already closed.
			if (streamId % 2 == nextStreamId % 2)
				return;

			// Create a stream.
			stream = new Http2Stream(streamId, Http2Connection.this, false, inFinished, headerBlock);
			lastGoodStreamId = streamId;
			streams.put(streamId, stream);
		}
		
		//
	    stream.receiveHeaders(headerBlock);
	    
	    //
	    if (inFinished) 
	    	stream.receiveFin();
	}


	@Override
	public void onRstStream(int streamId, int errorCode) {

		if (pushedStream(streamId)) {
			// pushResetLater(streamId, errorCode);
			return;
		}

		Http2Stream rstStream = removeStream(streamId);
		if (rstStream != null) {
			rstStream.receiveRstStream(errorCode);
		}
	}


	@Override
	public void onSettings(boolean clearPrevious, Settings newSettings) {

		long delta = 0;
		Http2Stream[] streamsToNotify = null;

		int priorWriteWindowSize = localSettings.getInitialWindowSize();
		if (clearPrevious)
			localSettings.clear();
		localSettings.merge(newSettings);
		
		//
		int maxFrameSize = newSettings.getMaxFrameSize();
		if ( maxFrameSize != -1 )
			encoder.setMaxFrameSize( maxFrameSize );
		
		int headerTableSize = newSettings.getHeaderTableSize();
		if ( headerTableSize != -1) 
			encoder.setHeaderTableSize(headerTableSize);

		//
		ByteBuffer ackBuf = encoder.encodeAckSettingsFrame();
		
		byte[] bb = ackBuf.array();
		System.out.println("#SEND ack= " + ByteUtil.dump(bb, 0, bb.length));
		
		this.write( ackBuf );
		//

		int clientInitialWindowSize = localSettings.getInitialWindowSize();
		if (clientInitialWindowSize != -1 && clientInitialWindowSize != priorWriteWindowSize) {
			delta = clientInitialWindowSize - priorWriteWindowSize;

			if (!streams.isEmpty()) {
				streamsToNotify = streams.values().toArray(new Http2Stream[streams.size()]);
			}
		}

		//
		if (streamsToNotify != null && delta != 0) {
			for (Http2Stream stream : streamsToNotify) {
				stream.addBytesToWriteWindow(delta);
			}
		}
	}


	@Override
	public void onAckSettings() {
		// ignore
	}


	@Override
	public void onPing(final boolean ack, final int payload1, final int payload2) {
		if (ack) {
			//
		} else {
			// Send a reply to a client ping 
			ByteBuffer buf = encoder.encodePingFrame(ack, payload1, payload2);
			write(buf);
		}
	}


	@Override
	public void onGoAway(int lastGoodStreamId, int errorCode, byte[] debugData) {

		Http2Stream[] streamsCopy = streams.values().toArray(new Http2Stream[streams.size()]);

		for (Http2Stream stream : streamsCopy) {
			if (stream.getId() > lastGoodStreamId) {
				stream.receiveRstStream(ErrorCode.REFUSED_STREAM_ERROR);
				removeStream(stream.getId());
			}
		}
	}

	@Override
	public void onWindowUpdate(int streamId, long windowSizeIncrement) {
		if (streamId == 0) {
			bytesLeftInWriteWindow += windowSizeIncrement;
			
		} else {
			Http2Stream stream = getStream(streamId);
			if (stream != null) {
				stream.addBytesToWriteWindow(windowSizeIncrement);
			}
		}
	}

	@Override
	public void onPriority(int streamId, int streamDependency, int weight, boolean exclusive) {
		// ignore
	}

	@Override
	public void onPushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException {
		// pushRequestLater(promisedStreamId, requestHeaders);
	}
	
}
