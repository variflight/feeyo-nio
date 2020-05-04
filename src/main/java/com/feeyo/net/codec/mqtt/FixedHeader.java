package com.feeyo.net.codec.mqtt;

/**
 * See <a href="http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html#fixed-header">
 *     MQTTV3.1/fixed-header</a>
 */
public final class FixedHeader {

    private final MessageType messageType;
    private final boolean isDup;
    private final MqttQoS qosLevel;
    private final boolean isRetain;
    private final int remainingLength;

    public FixedHeader(
            MessageType messageType,
            boolean isDup,
            MqttQoS qosLevel,
            boolean isRetain,
            int remainingLength) {
        this.messageType = messageType;
        this.isDup = isDup;
        this.qosLevel = qosLevel;
        this.isRetain = isRetain;
        this.remainingLength = remainingLength;
    }

    public MessageType messageType() {
        return messageType;
    }

    public boolean isDup() {
        return isDup;
    }

    public MqttQoS qosLevel() {
        return qosLevel;
    }

    public boolean isRetain() {
        return isRetain;
    }

    public int remainingLength() {
        return remainingLength;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append("messageType=").append(messageType)
            .append(", isDup=").append(isDup)
            .append(", qosLevel=").append(qosLevel)
            .append(", isRetain=").append(isRetain)
            .append(", remainingLength=").append(remainingLength)
            .append(']')
            .toString();
    }
}
