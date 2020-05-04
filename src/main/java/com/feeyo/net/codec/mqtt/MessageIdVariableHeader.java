package com.feeyo.net.codec.mqtt;

/**
 * Variable Header containing only Message Id See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#msg-id">MQTTV3.1/msg-id</a>
 */
public final class MessageIdVariableHeader {

	private final int messageId;

	public static MessageIdVariableHeader from(int messageId) {
		if (messageId < 1 || messageId > 0xffff)
			throw new IllegalArgumentException("messageId: " + messageId + " (expected: 1 ~ 65535)");
		//
		return new MessageIdVariableHeader(messageId);
	}

	private MessageIdVariableHeader(int messageId) {
		this.messageId = messageId;
	}

	public int messageId() {
		return messageId;
	}

	@Override
	public String toString() {
		return new StringBuilder().append('[').append("messageId=").append(messageId).append(']').toString();
	}
}
