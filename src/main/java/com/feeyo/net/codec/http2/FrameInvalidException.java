package com.feeyo.net.codec.http2;

/**
 * 
 * @author zhuam
 */
public class FrameInvalidException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public FrameInvalidException(String message) {
		super(message);
	}
	
	public FrameInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

}
