package com.feeyo.net.codec.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;


// Writes HTTP/2 transport frames
//
public class Http2ResponseEncoder {

	private static final int HEADER_SIZE = 9;
	
	//
	private int maxFrameSize = FrameType.INITIAL_MAX_FRAME_SIZE;
	private int headerTableSize = -1;
	
	
	public int getMaxFrameSize() {
		return maxFrameSize;
	}

	public void setMaxFrameSize(int maxFrameSize) {
		this.maxFrameSize = maxFrameSize;
	}

	public int getHeaderTableSize() {
		return headerTableSize;
	}

	public void setHeaderTableSize(int headerTableSize) {
		this.headerTableSize = headerTableSize;
	}

	///
	public ByteBuffer frameHeader(int streamId, int length, byte type, byte flags) {
		
		ByteBuffer buf = ByteBuffer.allocate( HEADER_SIZE );

		// length
		buf.put((byte) ((length >>> 16) & 0xff));
		buf.put((byte) ((length >>> 8) & 0xff));
		buf.put((byte) (length & 0xff));

		if (length > maxFrameSize) {
			throw Util.illegalArgument("FRAME_SIZE_ERROR length > %d: %d", maxFrameSize, length);
		}
		if ((streamId & 0x80000000) != 0)
			throw Util.illegalArgument("reserved bit set: %s", streamId);

		buf.put((byte) (type & 0xff));
		buf.put((byte) (flags & 0xff));
		buf.putInt(streamId & 0x7fffffff);
		return buf;
	}

	//
	public ByteBuffer encodeDataFrame(boolean outFinished, int streamId, byte[] data, int byteCount) throws IOException {
		byte flags = Flags.NONE;
		if (outFinished)
			flags |= Flags.END_STREAM;
		return encodeDataFrame(streamId, flags, data, byteCount);
	}

	//
	public ByteBuffer encodeDataFrame(int streamId, byte flags, byte[] data, int byteCount) {
		
		byte type = FrameType.DATA;
		
		ByteBuffer frameHeaderBuf = frameHeader(streamId, byteCount, type, flags);
		frameHeaderBuf.position(0);
		
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  byteCount);
		buf.put( frameHeaderBuf );
		
		if (byteCount > 0) {
			byte[] dest = new byte[byteCount];
			System.arraycopy(data, 0, dest, 0, dest.length);
			buf.put(dest);
		}
		return buf;
	}

	public ByteBuffer encodeWindowUpdateFrame(int streamId, long windowSizeIncrement) {

		if (windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL) {
			throw Util.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s",
					windowSizeIncrement);
		}

		int lenght = 4;
		byte type = FrameType.WINDOW_UPDATE;
		byte flags =  Flags.NONE;
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  4);
		ByteBuffer frameHeaderBuf = frameHeader(streamId, lenght, type, flags);
		frameHeaderBuf.position(0);
		buf.put( frameHeaderBuf );
		buf.putInt((int) windowSizeIncrement);
		return buf;
	}

	public ByteBuffer encodeSettingsFrame(Settings settings) {

		int length = settings.size() * 6;
		byte type = FrameType.SETTINGS;
		byte flags =  Flags.NONE;
		int streamId = 0;
		
		ByteBuffer frameHeaderBuf = frameHeader(streamId, length, type, flags);

		ByteBuffer buf = ByteBuffer.allocate(9);
		frameHeaderBuf.position(0);
		buf.put( frameHeaderBuf );
		
		for (int i = 0; i < Settings.COUNT; i++) {
			if (!settings.isSet(i))
				continue;
			
			ByteBuffer newBuf = ByteBuffer.allocate( buf.capacity() + 6);
			buf.position( 0 );
			newBuf.put( buf );
			
			buf = newBuf;
			
			int id = i;
			if (id == 4) {
				id = 3; // SETTINGS_MAX_CONCURRENT_STREAMS renumbered.
			} else if (id == 7) {
				id = 4; // SETTINGS_INITIAL_WINDOW_SIZE renumbered.
			}
			buf.putShort((short) id);
			buf.putInt(settings.get(i));
		}
		return buf;
	}
	
	

	public ByteBuffer encodeAckSettingsFrame() {

		int length = 0;
		byte type = FrameType.SETTINGS;
		byte flags = Flags.ACK;
		int streamId = 0;

		ByteBuffer frameHeaderBuf = frameHeader(streamId, length, type, flags);
		return frameHeaderBuf;
	}
	
	public ByteBuffer encodePingFrame(boolean ack, int payload1, int payload2) {

		int length = 8;
		byte type = FrameType.PING;
		byte flags = ack ? Flags.ACK : Flags.NONE;
		int streamId = 0;
		
		ByteBuffer frameHeaderBuf = frameHeader(streamId, length, type, flags);
		frameHeaderBuf.position(0);
		
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  4 + 4);
		buf.put( frameHeaderBuf );
		buf.putInt(payload1);
		buf.putInt(payload2);
		return buf;
	}

	public ByteBuffer encodeGoAwayFrame(int lastGoodStreamId, int errorCode, byte[] debugData) throws IOException {

		if ( errorCode == -1)
			throw Util.illegalArgument("errorCode.httpCode == -1");

		int length = 8 + debugData.length;
		byte type = FrameType.GOAWAY;
		byte flags = Flags.NONE;
		int streamId = 0;
		
		ByteBuffer frameHeaderBuf = frameHeader(streamId, length, type, flags);
		frameHeaderBuf.position(0);
		
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  4 + 4 + (debugData != null ? debugData.length : 0));
		buf.put(frameHeaderBuf);
		buf.putInt(lastGoodStreamId);
		buf.putInt(errorCode);
		if (debugData.length > 0) {
			buf.put(debugData);
		}
		return buf;
	}

	public ByteBuffer encodeRstStreamFrame(int streamId, int errorCode) {

		if (errorCode == -1)
			throw new IllegalArgumentException();

		int length = 4;
		byte type = FrameType.RST_STREAM;
		byte flags = Flags.NONE;

		ByteBuffer frameHeaderBuf = frameHeader(streamId, length, type, flags);
		frameHeaderBuf.position(0);
		
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  4);
		buf.put(frameHeaderBuf);
		buf.putInt(errorCode);

		return buf;
	}

	public ByteBuffer encodePushPromiseFrame(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HpackWriter hpackWriter = new HpackWriter(out);
		if (this.headerTableSize != -1)
			hpackWriter.setHeaderTableSizeSetting(headerTableSize);

		hpackWriter.writeHeaders(requestHeaders);

		byte[] hpackData = out.toByteArray();
		long byteCount = hpackData.length;
		int length = (int) Math.min(maxFrameSize - 4, byteCount);
		byte type = FrameType.PUSH_PROMISE;
		byte flags = byteCount == length ? Flags.END_HEADERS : 0;

	
		ByteBuffer frameHeaderBuf = frameHeader(streamId, length + 4, type, flags);
		frameHeaderBuf.position(0);
		
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  4 + length);
		buf.put(frameHeaderBuf);
		buf.putInt(promisedStreamId & 0x7fffffff);

		int hpackOffset = 0;
		byte[] hpackBuf1 = new byte[length];
		System.arraycopy(hpackData, hpackOffset, hpackBuf1, 0, hpackBuf1.length);
		hpackOffset += length;

		buf.put(hpackBuf1);

		// Write continuation frames
		if (byteCount > length) {

			long cc = byteCount - length;
			while (byteCount > 0) {

				int frameLen = (int) Math.min(maxFrameSize, cc);
				cc -= frameLen;

				frameHeaderBuf = frameHeader(streamId, frameLen, FrameType.CONTINUATION, cc == 0 ? Flags.END_HEADERS : 0);
				frameHeaderBuf.position(0);
				
				// grow
				buf = ByteBuffer.allocate(buf.capacity() + HEADER_SIZE +  frameLen);
				buf.put(frameHeaderBuf);

				byte[] hpackBuf2 = new byte[frameLen];
				System.arraycopy(hpackData, hpackOffset, hpackBuf2, 0, hpackBuf2.length);

				buf.put(hpackBuf2);
			}
		}

		return buf;
	}

	public ByteBuffer encodeSynStreamFrame(boolean outFinished, int streamId, int associatedStreamId,
			List<Header> headerBlock) throws IOException {

		return encodeHeadersFrame(outFinished, streamId, headerBlock);
	}

	public ByteBuffer encodeSynReplyFrame(boolean outFinished, int streamId, List<Header> headerBlock)
			throws IOException {

		return encodeHeadersFrame(outFinished, streamId, headerBlock);
	}

	//
	public ByteBuffer encodeHeadersFrame(boolean outFinished, int streamId, List<Header> headerBlock)
			throws IOException {

		//
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		HpackWriter hpackWriter = new HpackWriter(out);
		if (this.headerTableSize != -1)
			hpackWriter.setHeaderTableSizeSetting(headerTableSize);
		hpackWriter.writeHeaders(headerBlock);

		byte[] hpackData = out.toByteArray();
		long byteCount = hpackData.length;
		int length = (int) Math.min(maxFrameSize, byteCount);
		byte type = FrameType.HEADERS;
		byte flags = byteCount == length ? Flags.END_HEADERS : 0;
		if (outFinished)
			flags |= Flags.END_STREAM;

		//
		ByteBuffer frameHeaderBuf = frameHeader(streamId, length, type, flags);
		frameHeaderBuf.position(0);
		
		//
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE +  length);
		buf.put(frameHeaderBuf);

		//
		int hpackOffset = 0;
		byte[] hpackBuf1 = new byte[length];
		System.arraycopy(hpackData, hpackOffset, hpackBuf1, 0, hpackBuf1.length);
		hpackOffset += length;

		buf.put(hpackBuf1);

		// Write continuation frames
		if (byteCount > length) {

			long cc = byteCount - length;
			while (byteCount > 0) {

				int frameLen = (int) Math.min(maxFrameSize, cc);
				cc -= frameLen;

				frameHeaderBuf = frameHeader(streamId, frameLen, FrameType.CONTINUATION, cc == 0 ? Flags.END_HEADERS : 0);
				frameHeaderBuf.position(0);
				
				// grow
				buf = ByteBuffer.allocate(buf.capacity() + HEADER_SIZE +  frameLen);
				buf.put(frameHeaderBuf);

				byte[] hpackBuf2 = new byte[frameLen];
				System.arraycopy(hpackData, hpackOffset, hpackBuf2, 0, hpackBuf2.length);

				buf.put(hpackBuf2);
			}
		}

		return buf;
	}

}
