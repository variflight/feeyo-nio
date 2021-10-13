package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

import com.feeyo.net.codec.Decoder;
import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.nio.util.BufferUtil;

public class WebSocketDecoder implements Decoder<Frame> {
	//
	protected enum State {
		START, 
		PAYLOAD_LEN, 
		PAYLOAD_LEN_BYTES, 
		MASK, 
		MASK_BYTES, 
		PAYLOAD
	}
	//
	// State specific
	protected State state = State.START;
	protected int cursor = 0;
	//
	// Frame
	protected Frame frame;
	//
	// payload specific
	protected ByteBuffer payload;
	protected int payloadLength;
	//
	// 掩码，用于掩码算法
	protected byte[] maskBytes;
	protected int maskInt;
	protected int maskOffset;
	
	
	@Override
	public Frame decode(byte[] buf) throws UnknownProtocolException {
		ByteBuffer buffer = ByteBuffer.wrap(buf);
		while (buffer.hasRemaining()) {
			switch (state) {
				case START: {
					// peek at byte
					byte b = buffer.get();
					boolean fin = ((b & 0x80) != 0);
					byte opcode = (byte) (b & 0x0F);
					if (!OpCode.isKnown(opcode)) {
						throw new UnknownProtocolException("Unknown opcode: " + opcode);
					}
					//
					frame = new Frame(opcode);
					frame.setFin(fin);
					if ((b & 0x70) != 0) {
						if ((b & 0x40) != 0) {
							frame.setRsv1(true);
						}
						if ((b & 0x20) != 0) {
							frame.setRsv2(true);
						}
						if ((b & 0x10) != 0) {
							frame.setRsv3(true);
						}
					}
					//
					state = State.PAYLOAD_LEN;
					break;
				}
				//
				case PAYLOAD_LEN: {
					byte b = buffer.get();
					frame.setMasked((b & 0x80) != 0);
					payloadLength = (byte) (0x7F & b);
					if (payloadLength == 127) {
						// length 8 bytes (extended payload length)
						payloadLength = 0;
						state = State.PAYLOAD_LEN_BYTES;
						cursor = 8;
						break;
					} else if (payloadLength == 126) {
						// length 2 bytes (extended payload length)
						payloadLength = 0;
						state = State.PAYLOAD_LEN_BYTES;
						cursor = 2;
						break;
					}
					//
					if (payloadLength > Integer.MAX_VALUE) 
						throw new UnknownProtocolException("[int-sane!] cannot handle payload lengths larger than " + Integer.MAX_VALUE);
					//
					if (frame.isMasked()) {
						state = State.MASK;
					} else {
						// 空有效负载的特殊情况（缓冲区中不再有字节）
						if (payloadLength == 0) {
							state = State.START;
							return frame;
						}
						resetMask(frame.getMask());
						state = State.PAYLOAD;
					}
					break;
				}
				//
				case PAYLOAD_LEN_BYTES: {
					byte b = buffer.get();
					--cursor;
					payloadLength |= (b & 0xFF) << (8 * cursor);
					if (cursor == 0) {
						//
						if (payloadLength > Integer.MAX_VALUE) 
							throw new UnknownProtocolException("[int-sane!] cannot handle payload lengths larger than " + Integer.MAX_VALUE);
						//
						if (frame.isMasked()) {
							state = State.MASK;
						} else {
							// 空有效负载的特殊情况（缓冲区中不再有字节）
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
						// 空有效负载的特殊情况（缓冲区中不再有字节）
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
				//
				case MASK_BYTES: {
					byte b = buffer.get();
					frame.getMask()[4 - cursor] = b;
					--cursor;
					if (cursor == 0) {
						// 空有效负载的特殊情况（缓冲区中不再有字节）
						if (payloadLength == 0) {
							state = State.START;
							return frame;
						}
						resetMask(frame.getMask());
						state = State.PAYLOAD;
					}
					break;
				}
				//
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
		return null;
	}

	private boolean parsePayload(ByteBuffer buffer) throws UnknownProtocolException {
		if (payloadLength == 0) {
			return true;
		}
		//
		if (buffer.hasRemaining()) {
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
}