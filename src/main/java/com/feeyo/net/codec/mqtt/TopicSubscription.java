package com.feeyo.net.codec.mqtt;

public final class TopicSubscription {

	private final String topicFilter;
	private final MqttQoS qualityOfService;

	public TopicSubscription(String topicFilter, MqttQoS qualityOfService) {
		this.topicFilter = topicFilter;
		this.qualityOfService = qualityOfService;
	}

	public String topicName() {
		return topicFilter;
	}

	public MqttQoS qualityOfService() {
		return qualityOfService;
	}

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append("topicFilter=").append(topicFilter)
            .append(", qualityOfService=").append(qualityOfService)
            .append(']')
            .toString();
    }
}
