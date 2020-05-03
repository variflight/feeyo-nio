package com.feeyo.net.codec.http2;

import java.util.Arrays;

public final class Settings {

	public static final int DEFAULT_INITIAL_WINDOW_SIZE = 65535;

	public static final int HEADER_TABLE_SIZE = 1;
	public static final int ENABLE_PUSH = 2;
	public static final int MAX_CONCURRENT_STREAMS = 4;
	public static final int MAX_FRAME_SIZE = 5;
	public static final int MAX_HEADER_LIST_SIZE = 6;
	public static final int INITIAL_WINDOW_SIZE = 7; 

	// Total number of settings
	public static final int COUNT = 10;

	// Bitfield of which flags that values
	private int set;

	// Flag values
	private final int[] values = new int[COUNT];

	public Settings set(int id, int value) {
		if (id < 0 || id >= values.length) {
			return this; // Discard unknown settings.
		}

		int bit = 1 << id;
		set |= bit;
		values[id] = value;
		return this;
	}
	
	public void clear() {
		set = 0;
		Arrays.fill(values, 0);
	}

	// Returns true if a value has been assigned for the setting {@code id}
	public boolean isSet(int id) {
		int bit = 1 << id;
		return (set & bit) != 0;
	}

	// Returns the value for the setting {@code id}, or 0 if unset
	public int get(int id) {
		return values[id];
	}

	// Returns the number of settings that have values assigned
	public int size() {
		return Integer.bitCount(set);
	}

	// Returns -1 if unset
	public int getHeaderTableSize() {
		int bit = 1 << HEADER_TABLE_SIZE;
		return (bit & set) != 0 ? values[HEADER_TABLE_SIZE] : -1;
	}

	public boolean getEnablePush(boolean defaultValue) {
		int bit = 1 << ENABLE_PUSH;
		return ((bit & set) != 0 ? values[ENABLE_PUSH] : defaultValue ? 1 : 0) == 1;
	}

	public int getMaxConcurrentStreams(int defaultValue) {
		int bit = 1 << MAX_CONCURRENT_STREAMS;
		return (bit & set) != 0 ? values[MAX_CONCURRENT_STREAMS] : defaultValue;
	}

	public int getMaxFrameSize() {
		int bit = 1 << MAX_FRAME_SIZE;
		return (bit & set) != 0 ? values[MAX_FRAME_SIZE] : -1;
	}

	public int getMaxHeaderListSize(int defaultValue) {
		int bit = 1 << MAX_HEADER_LIST_SIZE;
		return (bit & set) != 0 ? values[MAX_HEADER_LIST_SIZE] : defaultValue;
	}

	public int getInitialWindowSize() {
		int bit = 1 << INITIAL_WINDOW_SIZE;
		return (bit & set) != 0 ? values[INITIAL_WINDOW_SIZE] : DEFAULT_INITIAL_WINDOW_SIZE;
	}

	public void merge(Settings other) {
		for (int i = 0; i < COUNT; i++) {
			if (!other.isSet(i))
				continue;
			set(i, other.get(i));
		}
	}
}