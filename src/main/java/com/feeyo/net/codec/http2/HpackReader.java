package com.feeyo.net.codec.http2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-12#section-3.1
public final class HpackReader {

	private final List<Header> headerList = new ArrayList<>();

	private final byte[] data;
	private int data_offset;

	private final int headerTableSizeSetting;
	private int maxDynamicTableByteCount;

	// Visible for testing.
	Header[] dynamicTable = new Header[8];
	// Array is populated back to front, so new entries always have lowest
	// index.
	int nextHeaderIndex = dynamicTable.length - 1;
	int headerCount = 0;
	int dynamicTableByteCount = 0;

	public HpackReader(int headerTableSizeSetting, byte[] data) {
		this(headerTableSizeSetting, headerTableSizeSetting, data);
	}

	public HpackReader(int headerTableSizeSetting, int maxDynamicTableByteCount, byte[] data) {
		this.headerTableSizeSetting = headerTableSizeSetting;
		this.maxDynamicTableByteCount = maxDynamicTableByteCount;
		this.data = data;
		this.data_offset = 0;
	}

	int maxDynamicTableByteCount() {
		return maxDynamicTableByteCount;
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
			nextHeaderIndex += entriesToEvict;
		}
		return entriesToEvict;
	}

	/**
	 * Read {@code byteCount} bytes of headers from the source stream. This
	 * implementation does not propagate the never indexed flag of a header.
	 */
	void readHeaders() throws IOException {
		while (data_offset < data.length) {
			int b = readByte();
			if (b == 0x80) { // 10000000
				throw new IOException("index == 0");
			} else if ((b & 0x80) == 0x80) { // 1NNNNNNN
				int index = readInt(b, Hpack.PREFIX_7_BITS);
				readIndexedHeader(index - 1);
			} else if (b == 0x40) { // 01000000
				readLiteralHeaderWithIncrementalIndexingNewName();
			} else if ((b & 0x40) == 0x40) { // 01NNNNNN
				int index = readInt(b, Hpack.PREFIX_6_BITS);
				readLiteralHeaderWithIncrementalIndexingIndexedName(index - 1);
			} else if ((b & 0x20) == 0x20) { // 001NNNNN
				maxDynamicTableByteCount = readInt(b, Hpack.PREFIX_5_BITS);
				if (maxDynamicTableByteCount < 0 || maxDynamicTableByteCount > headerTableSizeSetting) {
					throw new IOException("Invalid dynamic table size update " + maxDynamicTableByteCount);
				}
				adjustDynamicTableByteCount();
			} else if (b == 0x10 || b == 0) { // 000?0000 - Ignore never
												// indexed bit.
				readLiteralHeaderWithoutIndexingNewName();
			} else { // 000?NNNN - Ignore never indexed bit.
				int index = readInt(b, Hpack.PREFIX_4_BITS);
				readLiteralHeaderWithoutIndexingIndexedName(index - 1);
			}
		}
	}

	public List<Header> getAndResetHeaderList() {
		List<Header> result = new ArrayList<>(headerList);
		headerList.clear();
		return result;
	}

	private void readIndexedHeader(int index) throws IOException {
		if (isStaticHeader(index)) {
			Header staticEntry = Hpack.STATIC_HEADER_TABLE[index];
			headerList.add(staticEntry);
		} else {
			int dynamicTableIndex = dynamicTableIndex(index - Hpack.STATIC_HEADER_TABLE.length);
			if (dynamicTableIndex < 0 || dynamicTableIndex >= dynamicTable.length) {
				throw new IOException("Header index too large " + (index + 1));
			}
			headerList.add(dynamicTable[dynamicTableIndex]);
		}
	}

	// referencedHeaders is relative to nextHeaderIndex + 1.
	private int dynamicTableIndex(int index) {
		return nextHeaderIndex + 1 + index;
	}

	private void readLiteralHeaderWithoutIndexingIndexedName(int index) throws IOException {
		byte[] name = getName(index);
		byte[] value = readByteString();
		headerList.add(new Header(name, value));
	}

	private void readLiteralHeaderWithoutIndexingNewName() throws IOException {
		byte[] name = Util.checkLowercase(readByteString());
		byte[] value = readByteString();
		headerList.add(new Header(name, value));
	}

	private void readLiteralHeaderWithIncrementalIndexingIndexedName(int nameIndex) throws IOException {
		byte[] name = getName(nameIndex);
		byte[] value = readByteString();
		insertIntoDynamicTable(-1, new Header(name, value));
	}

	private void readLiteralHeaderWithIncrementalIndexingNewName() throws IOException {
		byte[] name = Util.checkLowercase(readByteString());
		byte[] value = readByteString();
		insertIntoDynamicTable(-1, new Header(name, value));
	}

	private byte[] getName(int index) throws IOException {
		if (isStaticHeader(index)) {
			return Hpack.STATIC_HEADER_TABLE[index].name;
		} else {
			int dynamicTableIndex = dynamicTableIndex(index - Hpack.STATIC_HEADER_TABLE.length);
			if (dynamicTableIndex < 0 || dynamicTableIndex >= dynamicTable.length) {
				throw new IOException("Header index too large " + (index + 1));
			}

			return dynamicTable[dynamicTableIndex].name;
		}
	}

	private boolean isStaticHeader(int index) {
		return index >= 0 && index <= Hpack.STATIC_HEADER_TABLE.length - 1;
	}

	/** index == -1 when new. */
	private void insertIntoDynamicTable(int index, Header entry) {
		headerList.add(entry);

		int delta = entry.hpackSize;
		if (index != -1) { // Index -1 == new header.
			delta -= dynamicTable[dynamicTableIndex(index)].hpackSize;
		}

		// if the new or replacement header is too big, drop all entries.
		if (delta > maxDynamicTableByteCount) {
			clearDynamicTable();
			return;
		}

		// Evict headers to the required length.
		int bytesToRecover = (dynamicTableByteCount + delta) - maxDynamicTableByteCount;
		int entriesEvicted = evictToRecoverBytes(bytesToRecover);

		if (index == -1) { // Adding a value to the dynamic table.
			if (headerCount + 1 > dynamicTable.length) { // Need to grow the
															// dynamic
															// table.
				Header[] doubled = new Header[dynamicTable.length * 2];
				System.arraycopy(dynamicTable, 0, doubled, dynamicTable.length, dynamicTable.length);
				nextHeaderIndex = dynamicTable.length - 1;
				dynamicTable = doubled;
			}
			index = nextHeaderIndex--;
			dynamicTable[index] = entry;
			headerCount++;
		} else { // Replace value at same position.
			index += dynamicTableIndex(index) + entriesEvicted;
			dynamicTable[index] = entry;
		}
		dynamicTableByteCount += delta;
	}

	private int readByte() throws IOException {
		byte b = data[data_offset];
		data_offset++;
		return b & 0xff;
	}

	int readInt(int firstByte, int prefixMask) throws IOException {
		int prefix = firstByte & prefixMask;
		if (prefix < prefixMask) {
			return prefix; // This was a single byte value.
		}

		// This is a multibyte value. Read 7 bits at a time.
		int result = prefixMask;
		int shift = 0;
		while (true) {
			int b = readByte();
			if ((b & 0x80) != 0) { // Equivalent to (b >= 128) since b is in
									// [0..255].
				result += (b & 0x7f) << shift;
				shift += 7;
			} else {
				result += b << shift; // Last byte.
				break;
			}
		}
		return result;
	}

	/** Reads a potentially Huffman encoded byte string. */
	byte[] readByteString() throws IOException {
		int firstByte = readByte();
		boolean huffmanDecode = (firstByte & 0x80) == 0x80; // 1NNNNNNN
		int length = readInt(firstByte, Hpack.PREFIX_7_BITS);

		//
		Util.checkOffsetAndCount(data.length, data_offset, length);

		byte[] dest = new byte[length];
		System.arraycopy(data, data_offset, dest, 0, dest.length);
		data_offset += length;

		if (huffmanDecode) {
			return Huffman.get().decode(dest);
		} else {
			return dest;
		}
	}
}
