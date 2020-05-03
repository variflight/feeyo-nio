package com.feeyo.net.codec.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HpackWriter {

	private static final int SETTINGS_HEADER_TABLE_SIZE = 4096;

	/**
	 * The decoder has ultimate control of the maximum size of the dynamic
	 * table but we can choose to use less. We'll put a cap at 16K. This is
	 * arbitrary but should be enough for most purposes.
	 */
	private static final int SETTINGS_HEADER_TABLE_SIZE_LIMIT = 16384;

	private final ByteArrayOutputStream out;
	private final boolean useCompression;

	/**
	 * In the scenario where the dynamic table size changes multiple times
	 * between transmission of header blocks, we need to keep track of the
	 * smallest value in that interval.
	 */
	private int smallestHeaderTableSizeSetting = Integer.MAX_VALUE;
	private boolean emitDynamicTableSizeUpdate;

	int headerTableSizeSetting;
	int maxDynamicTableByteCount;

	// Visible for testing.
	Header[] dynamicTable = new Header[8];
	// Array is populated back to front, so new entries always have lowest
	// index.
	int nextHeaderIndex = dynamicTable.length - 1;
	int headerCount = 0;
	int dynamicTableByteCount = 0;

	public HpackWriter(ByteArrayOutputStream out) {
		this(SETTINGS_HEADER_TABLE_SIZE, true, out);
	}

	public HpackWriter(int headerTableSizeSetting, boolean useCompression, ByteArrayOutputStream out) {
		this.headerTableSizeSetting = headerTableSizeSetting;
		this.maxDynamicTableByteCount = headerTableSizeSetting;
		this.useCompression = useCompression;
		this.out = out;
	}

	private void clearDynamicTable() {
		Arrays.fill(dynamicTable, null);
		nextHeaderIndex = dynamicTable.length - 1;
		headerCount = 0;
		dynamicTableByteCount = 0;
	}

	/** Returns the count of entries evicted. */
	private int evictToRecoverBytes(int bytesToRecover) {
		int entriesToEvict = 0;
		if (bytesToRecover > 0) {
			// determine how many headers need to be evicted.
			for (int j = dynamicTable.length - 1; j >= nextHeaderIndex && bytesToRecover > 0; j--) {
				bytesToRecover -= dynamicTable[j].hpackSize;
				dynamicTableByteCount -= dynamicTable[j].hpackSize;
				headerCount--;
				entriesToEvict++;
			}
			System.arraycopy(dynamicTable, nextHeaderIndex + 1, dynamicTable, nextHeaderIndex + 1 + entriesToEvict,
					headerCount);
			Arrays.fill(dynamicTable, nextHeaderIndex + 1, nextHeaderIndex + 1 + entriesToEvict, null);
			nextHeaderIndex += entriesToEvict;
		}
		return entriesToEvict;
	}

	private void insertIntoDynamicTable(Header entry) {
		int delta = entry.hpackSize;

		// if the new or replacement header is too big, drop all entries.
		if (delta > maxDynamicTableByteCount) {
			clearDynamicTable();
			return;
		}

		// Evict headers to the required length.
		int bytesToRecover = (dynamicTableByteCount + delta) - maxDynamicTableByteCount;
		evictToRecoverBytes(bytesToRecover);

		if (headerCount + 1 > dynamicTable.length) { // Need to grow the
														// dynamic table.
			Header[] doubled = new Header[dynamicTable.length * 2];
			System.arraycopy(dynamicTable, 0, doubled, dynamicTable.length, dynamicTable.length);
			nextHeaderIndex = dynamicTable.length - 1;
			dynamicTable = doubled;
		}
		int index = nextHeaderIndex--;
		dynamicTable[index] = entry;
		headerCount++;
		dynamicTableByteCount += delta;
	}

	/**
	 * This does not use "never indexed" semantics for sensitive headers.
	 */
	// http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-12#section-6.2.3
	void writeHeaders(List<Header> headerBlock) throws IOException {
		if (emitDynamicTableSizeUpdate) {
			if (smallestHeaderTableSizeSetting < maxDynamicTableByteCount) {
				// Multiple dynamic table size updates!
				writeInt(smallestHeaderTableSizeSetting, Hpack.PREFIX_5_BITS, 0x20);
			}
			emitDynamicTableSizeUpdate = false;
			smallestHeaderTableSizeSetting = Integer.MAX_VALUE;
			writeInt(maxDynamicTableByteCount, Hpack.PREFIX_5_BITS, 0x20);
		}

		for (int i = 0, size = headerBlock.size(); i < size; i++) {
			Header header = headerBlock.get(i);
			byte[] name = Util.toAsciiLowercase(header.name);
			byte[] value = header.value;
			int headerIndex = -1;
			int headerNameIndex = -1;

			Integer staticIndex = NAME_TO_FIRST_INDEX.get(name);
			if (staticIndex != null) {
				headerNameIndex = staticIndex + 1;
				if (headerNameIndex > 1 && headerNameIndex < 8) {
					// Only search a subset of the static header table. Most
					// entries have an empty value, so
					// it's unnecessary to waste cycles looking at them.
					// This check is built on the
					// observation that the header entries we care about are
					// in adjacent pairs, and we
					// always know the first index of the pair.
					if (Util.equal(Hpack.STATIC_HEADER_TABLE[headerNameIndex - 1].value, value)) {
						headerIndex = headerNameIndex;
					} else if (Util.equal(Hpack.STATIC_HEADER_TABLE[headerNameIndex].value, value)) {
						headerIndex = headerNameIndex + 1;
					}
				}
			}

			if (headerIndex == -1) {
				for (int j = nextHeaderIndex + 1, length = dynamicTable.length; j < length; j++) {
					if (Util.equal(dynamicTable[j].name, name)) {
						if (Util.equal(dynamicTable[j].value, value)) {
							headerIndex = j - nextHeaderIndex + Hpack.STATIC_HEADER_TABLE.length;
							break;
						} else if (headerNameIndex == -1) {
							headerNameIndex = j - nextHeaderIndex + Hpack.STATIC_HEADER_TABLE.length;
						}
					}
				}
			}

			if (headerIndex != -1) {
				// Indexed Header Field.
				writeInt(headerIndex, Hpack.PREFIX_7_BITS, 0x80);
			} else if (headerNameIndex == -1) {
				// Literal Header Field with Incremental Indexing - New
				// Name.
				out.write(0x40);
				writeByteString(name);
				writeByteString(value);
				insertIntoDynamicTable(header);
			} else if (Util.startsWith(name, Header.PSEUDO_PREFIX) && !Header.TARGET_AUTHORITY.equals(name)) {
				// Follow Chromes lead - only include the :authority pseudo
				// header, but exclude all other
				// pseudo headers. Literal Header Field without Indexing -
				// Indexed Name.
				writeInt(headerNameIndex, Hpack.PREFIX_4_BITS, 0);
				writeByteString(value);
			} else {
				// Literal Header Field with Incremental Indexing - Indexed
				// Name.
				writeInt(headerNameIndex, Hpack.PREFIX_6_BITS, 0x40);
				writeByteString(value);
				insertIntoDynamicTable(header);
			}
		}
	}

	// http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-12#section-4.1.1
	void writeInt(int value, int prefixMask, int bits) {
		// Write the raw value for a single byte value.
		if (value < prefixMask) {
			out.write(bits | value);
			return;
		}

		// Write the mask to start a multibyte value.
		out.write(bits | prefixMask);
		value -= prefixMask;

		// Write 7 bits at a time 'til we're done.
		while (value >= 0x80) {
			int b = value & 0x7f;
			out.write(b | 0x80);
			value >>>= 7;
		}
		out.write(value);
	}

	void writeByteString(byte[] data) throws IOException {
		if (useCompression && Huffman.get().encodedLength(data) < data.length) {

			ByteArrayOutputStream huffmanBuffer = new ByteArrayOutputStream();
			Huffman.get().encode(data, huffmanBuffer);

			byte[] huffmanBytes = huffmanBuffer.toByteArray();
			writeInt(huffmanBytes.length, Hpack.PREFIX_7_BITS, 0x80);
			out.write(huffmanBytes);

		} else {
			writeInt(data.length, Hpack.PREFIX_7_BITS, 0);
			out.write(data);
		}
	}

	void setHeaderTableSizeSetting(int headerTableSizeSetting) {
		this.headerTableSizeSetting = headerTableSizeSetting;
		int effectiveHeaderTableSize = Math.min(headerTableSizeSetting, SETTINGS_HEADER_TABLE_SIZE_LIMIT);

		if (maxDynamicTableByteCount == effectiveHeaderTableSize)
			return; // No change.

		if (effectiveHeaderTableSize < maxDynamicTableByteCount) {
			smallestHeaderTableSizeSetting = Math.min(smallestHeaderTableSizeSetting, effectiveHeaderTableSize);
		}
		emitDynamicTableSizeUpdate = true;
		maxDynamicTableByteCount = effectiveHeaderTableSize;
		adjustDynamicTableByteCount();
	}

	private void adjustDynamicTableByteCount() {
		if (maxDynamicTableByteCount < dynamicTableByteCount) {
			if (maxDynamicTableByteCount == 0) {
				clearDynamicTable();
			} else {
				evictToRecoverBytes(dynamicTableByteCount - maxDynamicTableByteCount);
			}
		}
	}
	
	static final Map<byte[], Integer> NAME_TO_FIRST_INDEX = nameToFirstIndex();

	private static Map<byte[], Integer> nameToFirstIndex() {
		Map<byte[], Integer> result = new LinkedHashMap<>(Hpack.STATIC_HEADER_TABLE.length);
		for (int i = 0; i < Hpack.STATIC_HEADER_TABLE.length; i++) {
			if (!result.containsKey(Hpack.STATIC_HEADER_TABLE[i].name)) {
				result.put(Hpack.STATIC_HEADER_TABLE[i].name, i);
			}
		}
		return Collections.unmodifiableMap(result);
	}
}