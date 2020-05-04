package com.feeyo.net.codec.mqtt;

import java.nio.charset.StandardCharsets;

/**
 * Mqtt version 
 */
public enum MqttVersion {

	MQTT_3_1("MQIsdp", (byte) 3), 
	MQTT_3_1_1("MQTT", (byte) 4);

	private final String name;
	private final byte level;

	MqttVersion(String protocolName, byte protocolLevel) {
		name = protocolName;
		level = protocolLevel;
	}

	public String protocolName() {
		return name;
	}

	public byte[] protocolNameBytes() {
		return name.getBytes(StandardCharsets.UTF_8);
	}

	public byte protocolLevel() {
		return level;
	}

	public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel)
			throws MqttUnacceptableProtocolVersionException {
		//
		for (MqttVersion mv : values()) {
			if (mv.name.equals(protocolName)) {
				if (mv.level == protocolLevel) {
					return mv;
				} else {
					throw new MqttUnacceptableProtocolVersionException(protocolName + " and " + protocolLevel + " are not match");
				}
			}
		}
		throw new MqttUnacceptableProtocolVersionException(protocolName + "is unknown protocol name");
	}
}
