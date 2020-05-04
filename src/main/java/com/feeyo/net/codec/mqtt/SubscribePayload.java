package com.feeyo.net.codec.mqtt;

import java.util.Collections;
import java.util.List;

/**
 * Payload of the {@link SubscribeMessage}
 */
public final class SubscribePayload {

	private final List<TopicSubscription> topicSubscriptions;

	public SubscribePayload(List<TopicSubscription> topicSubscriptions) {
		this.topicSubscriptions = Collections.unmodifiableList(topicSubscriptions);
	}

	public List<TopicSubscription> topicSubscriptions() {
		return topicSubscriptions;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder().append('[');
		for (int i = 0; i < topicSubscriptions.size(); i++) {
			builder.append(topicSubscriptions.get(i)).append(", ");
		}
		if (!topicSubscriptions.isEmpty()) {
			builder.setLength(builder.length() - 2);
		}
		return builder.append(']').toString();
	}
}
