package com.feeyo.net.codec.http.websocket;

/**
 * A Data Frame
 */
public class DataFrame extends AbstractFrame {
	//
	protected DataFrame(byte opcode) {
		super(opcode);
	}

	/**
	 * Construct new DataFrame based on headers of provided frame.
	 * <p>
	 * Useful for when working in extensions and a new frame needs to be
	 * created.
	 */
	public DataFrame(Frame basedOn) {
		this(basedOn, false);
	}

	/**
	 * Construct new DataFrame based on headers of provided frame, overriding
	 * for continuations if needed.
	 * <p>
	 * Useful for when working in extensions and a new frame needs to be
	 * created.
	 */
	public DataFrame(Frame basedOn, boolean continuation) {
		super(basedOn.getOpCode());
		copyHeaders(basedOn);
		if (continuation) {
			setOpCode(OpCode.CONTINUATION);
		}
	}

	@Override
	public void assertValid() {
		/* no extra validation for data frames (yet) here */
	}

	@Override
	public boolean isControlFrame() {
		return false;
	}

	@Override
	public boolean isDataFrame() {
		return true;
	}

	/**
	 * Set the data frame to continuation mode
	 */
	public void setIsContinuation() {
		setOpCode(OpCode.CONTINUATION);
	}
}
