package com.feeyo.net.codec.mqtt;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubAckPayload {

	private final List<Integer> grantedQoSLevels;

	public SubAckPayload(int... grantedQoSLevels) {
		if (grantedQoSLevels == null) {
			throw new NullPointerException("grantedQoSLevels");
		}

		List<Integer> list = new ArrayList<Integer>(grantedQoSLevels.length);
		for (int v : grantedQoSLevels) {
			list.add(v);
		}
		this.grantedQoSLevels = Collections.unmodifiableList(list);
	}

	public SubAckPayload(Iterable<Integer> grantedQoSLevels) {
		if (grantedQoSLevels == null) {
			throw new NullPointerException("grantedQoSLevels");
		}
		List<Integer> list = new ArrayList<Integer>();
		for (Integer v : grantedQoSLevels) {
			if (v == null) {
				break;
			}
			list.add(v);
		}
		this.grantedQoSLevels = Collections.unmodifiableList(list);
	}

	public List<Integer> grantedQoSLevels() {
		return grantedQoSLevels;
	}

    @Override
    public String toString() {
        return new StringBuilder()
            .append('[')
            .append("grantedQoSLevels=").append(grantedQoSLevels)
            .append(']')
            .toString();
    }
}
