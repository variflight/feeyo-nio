package com.feeyo.net.codec.mqtt;

/**
 * Base class for all MQTT message types.
 */
public class Message {

    private final FixedHeader fixedHeader;
    private final Object variableHeader;
    private final Object payload;
    private final Object result;

    // Constants for fixed-header only message types with all flags set to 0 (see
    // http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Table_2.2_-)
    public static final Message PINGREQ = new Message(new FixedHeader(MessageType.PINGREQ, false,
            MqttQoS.AT_MOST_ONCE, false, 0));

    public static final Message PINGRESP = new Message(new FixedHeader(MessageType.PINGRESP, false,
            MqttQoS.AT_MOST_ONCE, false, 0));

    public static final Message DISCONNECT = new Message(new FixedHeader(MessageType.DISCONNECT, false,
            MqttQoS.AT_MOST_ONCE, false, 0));

    public Message(FixedHeader fixedHeader) {
        this(fixedHeader, null, null);
    }

    public Message(FixedHeader fixedHeader, Object variableHeader) {
        this(fixedHeader, variableHeader, null);
    }

    public Message(FixedHeader fixedHeader, Object variableHeader, Object payload) {
        this(fixedHeader, variableHeader, payload, "SUCCESS");
    }

	public Message(FixedHeader fixedHeader, Object variableHeader, Object payload, Object result) {
		this.fixedHeader = fixedHeader;
		this.variableHeader = variableHeader;
		this.payload = payload;
		this.result = result;
	}

    public FixedHeader fixedHeader() {
        return fixedHeader;
    }

    public Object variableHeader() {
        return variableHeader;
    }

    public Object payload() {
        return payload;
    }
    
    public Object result() {
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append("fixedHeader=").append(fixedHeader() != null ? fixedHeader().toString() : "")
            .append(", variableHeader=").append(variableHeader() != null ? variableHeader.toString() : "")
            .append(", payload=").append(payload() != null ? payload.toString() : "")
            .append(']')
            .toString();
    }
}
