package com.feeyo.net.nio.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufferUtil {
	
	/**
	 * Clear the buffer to be empty in flush mode
	 */
	public static void clear(ByteBuffer buffer) {
		if (buffer != null) {
			buffer.position(0);
			buffer.limit(0);
		}
	}

	/**
	 * Clear the buffer to be empty in fill mode
	 */
	public static void clearToFill(ByteBuffer buffer) {
		if (buffer != null) {
			buffer.position(0);
			buffer.limit(buffer.capacity());
		}
	}
	
	/**
	 * Flip the buffer to fill mode
	 */
	public static int flipToFill(ByteBuffer buffer) {
		int position = buffer.position();
		int limit = buffer.limit();
		if (position == limit) {
			buffer.position(0);
			buffer.limit(buffer.capacity());
			return 0;
		}
		int capacity = buffer.capacity();
		if (limit == capacity) {
			buffer.compact();
			return 0;
		}
		buffer.position(limit);
		buffer.limit(capacity);
		return position;
	}
	
	public static void flipToFlush(ByteBuffer buffer, int position) {
        buffer.limit(buffer.position());
        buffer.position(position);
    }

	public static boolean isEmpty(ByteBuffer buf) {
		return buf == null || buf.remaining() == 0;
	}

	public static boolean isFull(ByteBuffer buf) {
		return buf != null && buf.limit() == buf.capacity();
	}

	public static int length(ByteBuffer buffer) {
		return buffer == null ? 0 : buffer.remaining();
	}

	
	/**
	 *  Convert the buffer to an ISO-8859-1 String
	 */
	public static String toString(ByteBuffer buffer) {
		return toString(buffer, StandardCharsets.ISO_8859_1);
	}

	/**
	 * Convert the buffer to an UTF-8 String
	 */
	public static String toUTF8String(ByteBuffer buffer) {
		return toString(buffer, StandardCharsets.UTF_8);
	}

	/**
	 * Convert the buffer to an the charset String
	 */
	public static String toString(ByteBuffer buffer, Charset charset) {
		if (buffer == null)
			return null;
		byte[] array = buffer.hasArray() ? buffer.array() : null;
		if (array == null) {
			byte[] to = new byte[buffer.remaining()];
			buffer.slice().get(to);
			return new String(to, 0, to.length, charset);
		}
		return new String(array, buffer.arrayOffset() + buffer.position(), buffer.remaining(), charset);
	}

	/**
	 * Convert a partial buffer to an ISO-8859-1 String
	 */
	public static String toString(ByteBuffer buffer, int position, int length, Charset charset) {
		if (buffer == null)
			return null;
		byte[] array = buffer.hasArray() ? buffer.array() : null;
		if (array == null) {
			ByteBuffer ro = buffer.asReadOnlyBuffer();
			ro.position(position);
			ro.limit(position + length);
			byte[] to = new byte[length];
			ro.get(to);
			return new String(to, 0, to.length, charset);
		}
		return new String(array, buffer.arrayOffset() + position, length, charset);
	}

}
