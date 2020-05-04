package com.feeyo.net.codec.mqtt;


import java.util.Collections;
import java.util.List;

/**
 * Payload of the {@link MqttUnsubscribeMessage}
 */
public final class MqttUnsubscribePayload {

    private final List<String> topics;

    public MqttUnsubscribePayload(List<String> topics) {
        this.topics = Collections.unmodifiableList(topics);
    }

    public List<String> topics() {
        return topics;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append('[');
        for (int i = 0; i < topics.size(); i++) {
            builder.append("topicName = ").append(topics.get(i)).append(", ");
        }
        if (!topics.isEmpty()) {
            builder.setLength(builder.length() - 2);
        }
        return builder.append("]").toString();
    }
}
