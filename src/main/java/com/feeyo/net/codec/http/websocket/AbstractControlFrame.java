package com.feeyo.net.codec.http.websocket;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.feeyo.net.codec.UnknownProtocolException;

public abstract class AbstractControlFrame extends AbstractFrame {
	/*
	 * Maximum size of Control frame, per RFC 6455 
	 */
	public static final int MAX_CONTROL_PAYLOAD = 125;

	public AbstractControlFrame(byte opcode) {
		super(opcode);
	}

	public void assertValid() throws UnknownProtocolException {
		if (isControlFrame()) {
			if (getPayloadLength() > AbstractControlFrame.MAX_CONTROL_PAYLOAD) {
				throw new UnknownProtocolException("Desired payload length [" + getPayloadLength() 
								+ "] exceeds maximum control payload length [" + MAX_CONTROL_PAYLOAD + "]");
			}
			if ((finRsvOp & 0x80) == 0) {
				throw new UnknownProtocolException("Cannot have FIN==false on Control frames");
			}
			if ((finRsvOp & 0x40) != 0) {
				throw new UnknownProtocolException("Cannot have RSV1==true on Control frames");
			}
			if ((finRsvOp & 0x20) != 0) {
				throw new UnknownProtocolException("Cannot have RSV2==true on Control frames");
			}
			if ((finRsvOp & 0x10) != 0) {
				throw new UnknownProtocolException("Cannot have RSV3==true on Control frames");
			}
		}
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
		AbstractControlFrame other = (AbstractControlFrame) obj;
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

	public boolean isControlFrame() {
		return true;
	}

	@Override
	public boolean isDataFrame() {
		return false;
	}

	@Override
	public AbstractFrame setPayload(ByteBuffer buf) throws UnknownProtocolException {
		if (buf != null && buf.remaining() > MAX_CONTROL_PAYLOAD) {
			throw new UnknownProtocolException("Control Payloads can not exceed " + MAX_CONTROL_PAYLOAD + " bytes in length.");
		}
		return super.setPayload(buf);
	}
}
