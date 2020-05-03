package com.feeyo.net.codec.mqtt;

/**
 * Base class for all MQTT message types.
 */
public class MqttMessage {

    private final MqttFixedHeader fixedHeader;
    private final Object variableHeader;
    private final Object payload;
    private final Object result;

    // Constants for fixed-header only message types with all flags set to 0 (see
    // http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Table_2.2_-)
    public static final MqttMessage PINGREQ = new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGREQ, false,
            MqttQoS.AT_MOST_ONCE, false, 0));

    public static final MqttMessage PINGRESP = new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false,
            MqttQoS.AT_MOST_ONCE, false, 0));

    public static final MqttMessage DISCONNECT = new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false,
            MqttQoS.AT_MOST_ONCE, false, 0));

    public MqttMessage(MqttFixedHeader fixedHeader) {
        this(fixedHeader, null, null);
    }

    public MqttMessage(MqttFixedHeader fixedHeader, Object variableHeader) {
        this(fixedHeader, variableHeader, null);
    }

    public MqttMessage(MqttFixedHeader fixedHeader, Object variableHeader, Object payload) {
        this(fixedHeader, variableHeader, payload, "SUCCESS");
    }

	public MqttMessage(MqttFixedHeader fixedHeader, Object variableHeader, Object payload, Object result) {
		this.fixedHeader = fixedHeader;
		this.variableHeader = variableHeader;
		this.payload = payload;
		this.result = result;
	}

    public MqttFixedHeader fixedHeader() {
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
