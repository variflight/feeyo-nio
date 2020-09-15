package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.nio.util.BufferUtil;

public class WsDecoder implements Decoder<Frame> {
	//
	private enum State {
		START, 
		PAYLOAD_LEN, 
		PAYLOAD_LEN_BYTES, 
		MASK, 
		MASK_BYTES, 
		PAYLOAD
	}
	//
	// State specific
	private State state = State.START;
	private int cursor = 0;
	//
	// Frame
	private Frame frame;
	private boolean priorDataFrame;
	//
	// payload specific
	private ByteBuffer payload;
	private int payloadLength;
	//
	private byte[] maskBytes;
	private int maskInt;
	private int maskOffset;
	//
	private byte flagsInUse = 0x00;

	@Override
	public Frame decode(byte[] buf) throws UnknownProtocolException {
		//
		ByteBuffer buffer = ByteBuffer.wrap(buf);
		while (buffer.hasRemaining()) {
			switch (state) {
			case START: {
				// peek at byte
				byte b = buffer.get();
				boolean fin = ((b & 0x80) != 0);
				byte opcode = (byte) (b & 0x0F);
				if (!FrameType.isKnown(opcode)) {
					throw new UnknownProtocolException("Unknown opcode: " + opcode);
				}
				//
				// base framing flags
				FrameType frameType = FrameType.from(opcode);
				switch ( frameType ) {
				case TEXT:
				case BINARY:
					frame = new Frame(opcode);
					// data validation
					if (priorDataFrame) {
						throw new UnknownProtocolException("Unexpected " + frameType + " frame, was expecting CONTINUATION");
					}
					break;
				case CONTINUATION:
					frame = new Frame(opcode);
					// continuation validation
					if (!priorDataFrame) {
						throw new UnknownProtocolException("CONTINUATION frame without prior !FIN");
					}
					// Be careful to use the original opcode
					break;
				case CLOSE:
				case PING:
				case PONG:
					frame = new Frame(opcode);
					// control frame validation
					if (!fin) {
						throw new UnknownProtocolException("Fragmented Frame [" + frameType + "]");
					}
					break;
				}

				frame.setFin(fin);
				//
				// Are any flags set?
				if ((b & 0x70) != 0) {
					/*
					 * RFC 6455 Section 5.2
					 */
					if ((b & 0x40) != 0) {
						if (isRsv1InUse())
							frame.setRsv1(true);
						else
							throw new UnknownProtocolException("RSV1 not allowed to be set");
					}
					if ((b & 0x20) != 0) {
						if (isRsv2InUse())
							frame.setRsv2(true);
						else
							throw new UnknownProtocolException("RSV2 not allowed to be set");
					}
					if ((b & 0x10) != 0) {
						if (isRsv3InUse())
							frame.setRsv3(true);
						else
							throw new UnknownProtocolException("RSV3 not allowed to be set");
					}
				}

				state = State.PAYLOAD_LEN;
				break;
			}

			case PAYLOAD_LEN: {
				byte b = buffer.get();
				frame.setMasked((b & 0x80) != 0);
				payloadLength = (byte) (0x7F & b);
				if (payloadLength == 127) // 0x7F
				{
					// length 8 bytes (extended payload length)
					payloadLength = 0;
					state = State.PAYLOAD_LEN_BYTES;
					cursor = 8;
					break; // continue onto next state
				} else if (payloadLength == 126) // 0x7E
				{
					// length 2 bytes (extended payload length)
					payloadLength = 0;
					state = State.PAYLOAD_LEN_BYTES;
					cursor = 2;
					break; // continue onto next state
				}
				assertSanePayloadLength(payloadLength);
				if (frame.isMasked()) {
					state = State.MASK;
				} else {
					// special case for empty payloads (no more bytes left in
					// buffer)
					if (payloadLength == 0) {
						state = State.START;
						return frame;
					}
					resetMask(frame.getMask());
					state = State.PAYLOAD;
				}
				break;
			}

			case PAYLOAD_LEN_BYTES: {
				byte b = buffer.get();
				--cursor;
				payloadLength |= (b & 0xFF) << (8 * cursor);
				if (cursor == 0) {
					assertSanePayloadLength(payloadLength);
					if (frame.isMasked()) {
						state = State.MASK;
					} else {
						// special case for empty payloads (no more bytes left
						// in buffer)
						if (payloadLength == 0) {
							state = State.START;
							return frame;
						}
						resetMask(frame.getMask());
						state = State.PAYLOAD;
					}
				}
				break;
			}

			case MASK: {
				byte m[] = new byte[4];
				frame.setMask(m);
				if (buffer.remaining() >= 4) {
					buffer.get(m, 0, 4);
					// special case for empty payloads (no more bytes left in
					// buffer)
					if (payloadLength == 0) {
						state = State.START;
						return frame;
					}
					resetMask(frame.getMask());
					state = State.PAYLOAD;
				} else {
					state = State.MASK_BYTES;
					cursor = 4;
				}
				break;
			}

			case MASK_BYTES: {
				byte b = buffer.get();
				frame.getMask()[4 - cursor] = b;
				--cursor;
				if (cursor == 0) {
					// special case for empty payloads (no more bytes left in
					// buffer)
					if (payloadLength == 0) {
						state = State.START;
						return frame;
					}
					resetMask(frame.getMask());
					state = State.PAYLOAD;
				}
				break;
			}

			case PAYLOAD: {
				frame.assertValid();
				if (parsePayload(buffer)) {
					//
					state = State.START;
					// we have a frame!
					return frame;
				}
				break;
			}
			}
		}
		//
		return null;
	}

	private boolean parsePayload(ByteBuffer buffer) throws UnknownProtocolException {
		if (payloadLength == 0) {
			return true;
		}
		//
		if (buffer.hasRemaining()) {
			// Create a small window of the incoming buffer to work with.
			// this should only show the payload itself, and not any more
			// bytes that could belong to the start of the next frame.
			int bytesSoFar = payload == null ? 0 : payload.position();
			int bytesExpected = payloadLength - bytesSoFar;
			int bytesAvailable = buffer.remaining();
			int windowBytes = Math.min(bytesAvailable, bytesExpected);
			int limit = buffer.limit();
			buffer.limit(buffer.position() + windowBytes);
			ByteBuffer window = buffer.slice();
			buffer.limit(limit);
			buffer.position(buffer.position() + window.remaining());
			//
			//
			// Mask process
			processMask( window );
			//
			if (window.remaining() == payloadLength) {
				// We have the whole content, no need to copy.
				frame.setPayload(window);
				return true;
			} else {
				if (payload == null) {
					payload = ByteBuffer.allocate(payloadLength);
					BufferUtil.clearToFill(payload);
				}
				// Copy the payload.
				payload.put(window);
				if (payload.position() == payloadLength) {
					BufferUtil.flipToFlush(payload, 0);
					frame.setPayload(payload);
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * 处理 Mask
	 */
	private void processMask(ByteBuffer payload) {
		if (maskBytes == null) {
			return;
		}
		int maskInt = this.maskInt;
		int start = payload.position();
		int end = payload.limit();
		int offset = this.maskOffset;
		int remaining;
		while ((remaining = end - start) > 0) {
			if (remaining >= 4 && (offset & 3) == 0) {
				payload.putInt(start, payload.getInt(start) ^ maskInt);
				start += 4;
				offset += 4;
			} else {
				payload.put(start, (byte) (payload.get(start) ^ maskBytes[offset & 3]));
				++start;
				++offset;
			}
		}
		maskOffset = offset;
	}
	
	//
	private void resetMask(byte[] mask) {
		this.maskBytes = mask;
		int maskInt = 0;
		if (mask != null) {
			for (byte maskByte : mask)
				maskInt = (maskInt << 8) + (maskByte & 0xFF);
		}
		this.maskInt = maskInt;
		this.maskOffset = 0;
	}

	private void assertSanePayloadLength(long len) throws UnknownProtocolException {
		if (len > Integer.MAX_VALUE) {
			throw new UnknownProtocolException("[int-sane!] cannot handle payload lengths larger than " + Integer.MAX_VALUE);
		}
	}

	public boolean isRsv1InUse() {
		return (flagsInUse & 0x40) != 0;
	}

	public boolean isRsv2InUse() {
		return (flagsInUse & 0x20) != 0;
	}

	public boolean isRsv3InUse() {
		return (flagsInUse & 0x10) != 0;
	}
}