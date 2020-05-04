package com.feeyo.net.codec.mqtt;

/**
 * Variable Header of the {@link PublishMessage}
 */
public final class PublishVariableHeader {

    private final String topicName;
    private final int packetId;

    public PublishVariableHeader(String topicName, int packetId) {
        this.topicName = topicName;
        this.packetId = packetId;
    }

    public String topicName() {
        return topicName;
    }

    public int packetId() {
        return packetId;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append("topicName=").append(topicName)
            .append(", packetId=").append(packetId)
            .append(']')
            .toString();
    }
}