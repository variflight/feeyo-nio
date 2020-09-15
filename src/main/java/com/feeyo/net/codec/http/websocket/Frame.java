package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;

/**
 * An immutable WebSocket frame.
 */
public interface Frame {
	//
	public static enum Type {
		//
		CONTINUATION((byte) 0x00), 
		TEXT((byte) 0x01), 
		BINARY((byte) 0x02), 
		CLOSE((byte) 0x08), 
		PING((byte) 0x09), 
		PONG((byte) 0x0A);
		//
		public static Type from(byte op) {
			for (Type type : values()) {
				if (type.opcode == op) {
					return type;
				}
			}
			throw new IllegalArgumentException("OpCode " + op + " is not a valid Frame.Type");
		}

		private byte opcode;

		private Type(byte code) {
			this.opcode = code;
		}

		public byte getOpCode() {
			return opcode;
		}

		public boolean isControl() {
			return (opcode >= CLOSE.getOpCode());
		}

		public boolean isData() {
			return (opcode == TEXT.getOpCode()) | (opcode == BINARY.getOpCode());
		}

		public boolean isContinuation() {
			return opcode == CONTINUATION.getOpCode();
		}

		@Override
		public String toString() {
			return this.name();
		}
	}

	public byte[] getMask();

	public byte getOpCode();

	public ByteBuffer getPayload();

	/**
	 * The original payload length ({@link ByteBuffer#remaining()})
	 * 
	 * @return the original payload length ({@link ByteBuffer#remaining()})
	 */
	public int getPayloadLength();

	public Type getType();

	public boolean hasPayload();

	public boolean isFin();

	/**
	 * Same as {@link #isFin()}
	 * 
	 * @return true if final frame.
	 */
	// FIXME: remove
	public boolean isLast();

	public boolean isMasked();

	public boolean isRsv1();

	public boolean isRsv2();

	public boolean isRsv3();
}
