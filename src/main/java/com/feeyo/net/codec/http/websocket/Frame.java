package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.feeyo.net.codec.UnknownProtocolException;
import com.feeyo.net.nio.util.BufferUtil;

/**
 * A Frame as seen in <a href="https://tools.ietf.org/html/rfc6455#section-5.2">RFC 6455. Sec 5.2</a>
 * 
 * <pre>
 *    0                   1                   2                   3
 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *   +-+-+-+-+-------+-+-------------+-------------------------------+
 *   |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
 *   |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
 *   |N|V|V|V|       |S|             |   (if payload len==126/127)   |
 *   | |1|2|3|       |K|             |                               |
 *   +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
 *   |     Extended payload length continued, if payload len == 127  |
 *   + - - - - - - - - - - - - - - - +-------------------------------+
 *   |                               |Masking-key, if MASK set to 1  |
 *   +-------------------------------+-------------------------------+
 *   | Masking-key (continued)       |          Payload Data         |
 *   +-------------------------------- - - - - - - - - - - - - - - - +
 *   :                     Payload Data continued ...                :
 *   + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
 *   |                     Payload Data continued ...                |
 *   +---------------------------------------------------------------+
 * </pre>
 */
public class Frame {
	//
	public static final int MAX_CONTROL_PAYLOAD = 125;
	
    /*
     * Combined FIN + RSV1 + RSV2 + RSV3 + OpCode byte.
     * <p>
     * 
     * <pre>
     *   1000_0000 (0x80) = fin
     *   0100_0000 (0x40) = rsv1
     *   0010_0000 (0x20) = rsv2
     *   0001_0000 (0x10) = rsv3
     *   0000_1111 (0x0F) = opcode
     * </pre>
     */	
    private byte finRsvOp;	
    private boolean masked = false;
    private byte mask[];
    //
    // The payload data
    private ByteBuffer data;
    
	public Frame(byte opcode) {
		reset();
		setOpCode(opcode);
	}
    
	public void assertValid() throws UnknownProtocolException {
		//
		if (isControlFrame()) {
			//
			if (getPayloadLength() > MAX_CONTROL_PAYLOAD) {
				throw new UnknownProtocolException("Desired payload length [" + getPayloadLength() + "] exceeds maximum payload length [" + MAX_CONTROL_PAYLOAD + "]");
			}
			if ((finRsvOp & 0x80) == 0) 
				throw new UnknownProtocolException("Cannot have FIN==false on Control frames");

			if ((finRsvOp & 0x40) != 0) 
				throw new UnknownProtocolException("Cannot have RSV1==true on Control frames");
		
			if ((finRsvOp & 0x20) != 0) 
				throw new UnknownProtocolException("Cannot have RSV2==true on Control frames");
			
			if ((finRsvOp & 0x10) != 0) 
				throw new UnknownProtocolException("Cannot have RSV3==true on Control frames");
		}
	}
	//
	protected void copyHeaders(Frame frame) {
		finRsvOp = 0x00;
		finRsvOp |= frame.isFin() ? 0x80 : 0x00;
		finRsvOp |= frame.isRsv1() ? 0x40 : 0x00;
		finRsvOp |= frame.isRsv2() ? 0x20 : 0x00;
		finRsvOp |= frame.isRsv3() ? 0x10 : 0x00;
		finRsvOp |= frame.getOpCode() & 0x0F;
		masked = frame.isMasked();
		if (masked) {
			mask = frame.getMask();
		} else {
			mask = null;
		}
	}

	public byte[] getMask() {
		return mask;
	}

	public final byte getOpCode() {
		return (byte) (finRsvOp & 0x0F);
	}

	/**
	 * Get the payload ByteBuffer. possible null.
	 */
	public ByteBuffer getPayload() {
		return data;
	}
	
	public String getPayloadAsUTF8() {
		if (data == null) {
			return null;
		}
		return BufferUtil.toUTF8String(getPayload());
	}

	public int getPayloadLength() {
		if (data == null) {
			return 0;
		}
		return data.remaining();
	}

	public boolean hasPayload() {
		return ((data != null) && data.hasRemaining());
	}

	//
	public boolean isControlFrame() {
		return OpCode.isControlFrame(getOpCode());
	}

	public boolean isDataFrame() {
		return OpCode.isDataFrame(getOpCode());
	}

	public boolean isFin() {
		return (byte) (finRsvOp & 0x80) != 0;
	}

	public boolean isMasked() {
		return masked;
	}

	public boolean isRsv1() {
		return (byte) (finRsvOp & 0x40) != 0;
	}

	public boolean isRsv2() {
		return (byte) (finRsvOp & 0x20) != 0;
	}

	public boolean isRsv3() {
		return (byte) (finRsvOp & 0x10) != 0;
	}

	public void reset() {
		finRsvOp = (byte) 0x80; // FIN (!RSV, opcode 0)
		masked = false;
		data = null;
		mask = null;
	}

	public Frame setFin(boolean fin) {
		// set bit 1
		this.finRsvOp = (byte) ((finRsvOp & 0x7F) | (fin ? 0x80 : 0x00));
		return this;
	}

	public Frame setMask(byte[] maskingKey) {
		this.mask = maskingKey;
		this.masked = (mask != null);
		return this;
	}

	public Frame setMasked(boolean mask) {
		this.masked = mask;
		return this;
	}

	protected Frame setOpCode(byte op) {
		this.finRsvOp = (byte) ((finRsvOp & 0xF0) | (op & 0x0F));
		return this;
	}

	/**
	 * Set the data payload.
	 */
	public Frame setPayload(ByteBuffer buf)  {
		data = buf;
		return this;
	}
	
	public Frame setPayload(String str) {
		return setPayload(ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8)));
	}
	
	public Frame setPayload(byte[] bytes) {
		return setPayload(ByteBuffer.wrap(bytes));
	}

	public Frame setRsv1(boolean rsv1) {
		this.finRsvOp = (byte) ((finRsvOp & 0xBF) | (rsv1 ? 0x40 : 0x00));	// set bit 2
		return this;
	}

	public Frame setRsv2(boolean rsv2) {
		this.finRsvOp = (byte) ((finRsvOp & 0xDF) | (rsv2 ? 0x20 : 0x00));	// set bit 3
		return this;
	}

	public Frame setRsv3(boolean rsv3) {
		this.finRsvOp = (byte) ((finRsvOp & 0xEF) | (rsv3 ? 0x10 : 0x00));	// set bit 4
		return this;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((data == null) ? 0 : data.hashCode());
		result = (prime * result) + finRsvOp;
		result = (prime * result) + Arrays.hashCode(mask);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Frame other = (Frame) obj;
		if (data == null) {
			if (other.data != null) {
				return false;
			}
		} else if (!data.equals(other.data)) {
			return false;
		}
		if (finRsvOp != other.finRsvOp) {
			return false;
		}
		if (!Arrays.equals(mask, other.mask)) {
			return false;
		}
		if (masked != other.masked) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(OpCode.name((byte) (finRsvOp & 0x0F)));
		b.append('[');
		b.append("len=").append(getPayloadLength());
		b.append(",fin=").append((finRsvOp & 0x80) != 0);
		b.append(",rsv=");
		b.append(((finRsvOp & 0x40) != 0) ? '1' : '.');
		b.append(((finRsvOp & 0x20) != 0) ? '1' : '.');
		b.append(((finRsvOp & 0x10) != 0) ? '1' : '.');
		b.append(",masked=").append(masked);
		b.append(']');
		return b.toString();
	}
}