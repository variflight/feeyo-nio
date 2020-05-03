package com.feeyo.metrics;

public class MetricName {

	public static String name(String name, String... names) {
		final StringBuilder builder = new StringBuilder();
		append(builder, name);
		if (names != null) {
			for (String s : names) {
				append(builder, s);
			}
		}
		return builder.toString();
	}

	private static void append(StringBuilder builder, String part) {
		if (part != null && !part.isEmpty()) {
			if (builder.length() > 0) {
				builder.append('.');
			}
			builder.append(part);
		}
	}

}
