package com.feeyo.net.codec.http2;

import java.io.IOException;
import java.nio.charset.Charset;

public class Util {
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	
	
	public static String toUtf8String(byte[] data) {
		return new String(data, UTF_8);
	}
	
	public static byte[] encodeUtf8(String s) {
		if (s == null)
			throw new IllegalArgumentException("s == null");
		return s.getBytes(UTF_8);
	}
	
	
	public static String hex(byte[] data) {
		char[] result = new char[data.length * 2];
		int c = 0;
		for (byte b : data) {
			result[c++] = HEX_DIGITS[(b >> 4) & 0xf];
			result[c++] = HEX_DIGITS[b & 0xf];
		}
		return new String(result);
	}
	
	public static byte[] decodeHex(String hex) {
		if (hex == null)
			throw new IllegalArgumentException("hex == null");
		
		if (hex.length() % 2 != 0)
			throw new IllegalArgumentException("Unexpected hex string: " + hex);

		byte[] result = new byte[hex.length() / 2];
		for (int i = 0; i < result.length; i++) {
			int d1 = decodeHexDigit(hex.charAt(i * 2)) << 4;
			int d2 = decodeHexDigit(hex.charAt(i * 2 + 1));
			result[i] = (byte) (d1 + d2);
		}
		return result;
	}
	
	public static int decodeHexDigit(char c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		throw new IllegalArgumentException("Unexpected hex digit: " + c);
	}
	
	
	public static byte[] toAsciiLowercase(byte[] data) {
		// Search for an uppercase character. If we don't find one, return this.
		for (int i = 0; i < data.length; i++) {
			byte c = data[i];
			if (c < 'A' || c > 'Z')
				continue;

			// If we reach this point, this string is not not lowercase. Create
			// and return a new byte string.
			byte[] lowercase = data.clone();
			lowercase[i++] = (byte) (c - ('A' - 'a'));
			for (; i < lowercase.length; i++) {
				c = lowercase[i];
				if (c < 'A' || c > 'Z')
					continue;
				lowercase[i] = (byte) (c - ('A' - 'a'));
			}
			return lowercase;
		}
		return data;
	}
	
	public static byte[] toAsciiUppercase(byte[] data) {
		// Search for an lowercase character. If we don't find one, return this.
		for (int i = 0; i < data.length; i++) {
			byte c = data[i];
			if (c < 'a' || c > 'z')
				continue;

			// If we reach this point, this string is not not uppercase. Create
			// and return a new byte string.
			byte[] uppercase = data.clone();
			uppercase[i++] = (byte) (c - ('a' - 'A'));
			for (; i < uppercase.length; i++) {
				c = uppercase[i];
				if (c < 'a' || c > 'z')
					continue;
				uppercase[i] = (byte) (c - ('a' - 'A'));
			}
			return uppercase;
		}
		return data;
	}
	
	/**
	 * An HTTP/2 response cannot contain uppercase header characters and must be treated as malformed.
	 */
	public static byte[] checkLowercase(byte[] name) throws IOException {
		for (int i = 0, length = name.length; i < length; i++) {
			byte c = name[i];
			if (c >= 'A' && c <= 'Z') {
				throw new IOException("PROTOCOL_ERROR response malformed: mixed case name: " + Util.toUtf8String(name));
			}
		}
		return name;
	}
	
	public static boolean startsWith(byte[] src, byte[] prefix) {
		return rangeEquals(src, 0, prefix, 0, prefix.length);
	}
	
	public final boolean endsWith(byte[] src, byte[] suffix) {
		return rangeEquals(src, src.length - suffix.length, suffix, 0, suffix.length);
	}
	
	public final int indexOf(byte[] src, byte[] dst) {
		return indexOf(src, dst, 0);
	}

	public int indexOf(byte[] src, byte[] dst, int srcIndex) {
		srcIndex = Math.max(srcIndex, 0);
		for (int i = srcIndex, limit = src.length - dst.length; i <= limit; i++) {
			if (Util.arrayRangeEquals(src, i, dst, 0, dst.length)) {
				return i;
			}
		}
		return -1;
	}
	
	public final int lastIndexOf(byte[] src, byte[] dst) {
		return lastIndexOf(src, dst, src.length);
	}

	public int lastIndexOf(byte[] src, byte[] other, int srcIndex) {
		srcIndex = Math.min(srcIndex, src.length - other.length);
		for (int i = srcIndex; i >= 0; i--) {
			if (Util.arrayRangeEquals(src, i, other, 0, other.length)) {
				return i;
			}
		}
		return -1;
	}

	
	public static boolean rangeEquals(byte[] src, int srcOffset, byte[] dst, int dstOffset, int byteCount) {
		return srcOffset >= 0 && srcOffset <= src.length - byteCount && dstOffset >= 0
				&& dstOffset <= dst.length - byteCount
				&& arrayRangeEquals(src, srcOffset, dst, dstOffset, byteCount);
	}
	
	public static boolean arrayRangeEquals(byte[] a, int aOffset, byte[] b, int bOffset, int byteCount) {
		for (int i = 0; i < byteCount; i++) {
			if (a[i + aOffset] != b[i + bOffset])
				return false;
		}
		return true;
	}
	
	public static void checkOffsetAndCount(long size, long offset, long byteCount) {
		if ((offset | byteCount) < 0 || offset > size || size - offset < byteCount) {
			throw new ArrayIndexOutOfBoundsException(
					String.format("size=%s offset=%s byteCount=%s", size, offset, byteCount));
		}
	}
	
	public static boolean equal(byte[] src, byte[] dst) {
		return src.length == dst.length
				&& rangeEquals(src, 0, dst, 0, dst.length);
	}

	public static String format(String format, Object... args) {
		return String.format(format, args);
	}
	
	//
	public static IllegalArgumentException illegalArgument(String message, Object... args) {
		throw new IllegalArgumentException( format(message, args) );
	}

	public static IOException ioException(String message, Object... args) throws IOException {
		throw new IOException( format(message, args) );
	}

}
