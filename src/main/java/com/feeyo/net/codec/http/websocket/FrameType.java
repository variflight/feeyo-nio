package com.feeyo.net.codec.http.websocket;

public enum FrameType {
	//
	CONTINUATION((byte) 0x00), 
	TEXT((byte) 0x01), 
	BINARY((byte) 0x02), 
	CLOSE((byte) 0x08), 
	PING((byte) 0x09), 
	PONG((byte) 0x0A);
	//
	public static FrameType from(byte opcode) {
		for (FrameType type : values()) {
			if (type.opcode == opcode) {
				return type;
			}
		}
		throw new IllegalArgumentException("OpCode " + opcode + " is not a valid FrameType");
	}

	private byte opcode;

	private FrameType(byte opcode) {
		this.opcode = opcode;
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
	
	public static boolean isKnown(byte opcode) {
		return (opcode == CONTINUATION.opcode) 
				|| (opcode == TEXT.opcode) 
				|| (opcode == BINARY.opcode) 
				|| (opcode == CLOSE.opcode)
				|| (opcode == PING.opcode) 
				|| (opcode == PONG.opcode);
	}


	@Override
	public String toString() {
		return this.name();
	}
}
