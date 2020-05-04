package com.feeyo.net.codec.mqtt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//
public enum ConnectReturnCode {
	
    CONNECTION_ACCEPTED((byte) 0x00),
    CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01),
    CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte) 0x02),
    CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte) 0x03),
    CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
    CONNECTION_REFUSED_NOT_AUTHORIZED((byte) 0x05);

    private static final Map<Byte, ConnectReturnCode> VALUE_TO_CODE_MAP;

	static {
		final Map<Byte, ConnectReturnCode> valueMap = new HashMap<Byte, ConnectReturnCode>();
		for (ConnectReturnCode code : values())
			valueMap.put(code.byteValue, code);
		//
		VALUE_TO_CODE_MAP = Collections.unmodifiableMap(valueMap);
	}

	private final byte byteValue;

	ConnectReturnCode(byte byteValue) {
		this.byteValue = byteValue;
	}

	public byte byteValue() {
		return byteValue;
	}

	public static ConnectReturnCode valueOf(byte b) {
		if (VALUE_TO_CODE_MAP.containsKey(b))
			return VALUE_TO_CODE_MAP.get(b);
		//
		throw new IllegalArgumentException("unknown connect return code: " + (b & 0xFF));
	}
}
