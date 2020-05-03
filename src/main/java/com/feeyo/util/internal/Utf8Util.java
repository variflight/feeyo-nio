package com.feeyo.util.internal;

import java.nio.charset.StandardCharsets;

public class Utf8Util {
	
	/**
	 * This method has better performance than String#getBytes(Charset), See the
	 * benchmark class: Utf8Benchmark for details.
	 */
	public static byte[] writeUtf8(final String in) {
		if (in == null) {
			return null;
		}
		if (UnsafeUtil.hasUnsafe()) {
			// Calculate the encoded length.
			final int len = UnsafeUtf8Util.encodedLength(in);
			final byte[] outBytes = new byte[len];
			UnsafeUtf8Util.encodeUtf8(in, outBytes, 0, len);
			return outBytes;
		} else {
			return in.getBytes(StandardCharsets.UTF_8);
		}
	}

	/**
	 * This method has better performance than String#String(byte[], Charset),
	 * See the benchmark class: Utf8Benchmark for details.
	 */
	public static String readUtf8(final byte[] in) {
		return readUtf8(in, 0, in.length);
	}
	
	public static String readUtf8(final byte[] in, int index, int size) {
		if (in == null) 
			return null;
		//
		if (UnsafeUtil.hasUnsafe()) {
			return UnsafeUtf8Util.decodeUtf8(in, index, size);
		} else {
			return new String(in, StandardCharsets.UTF_8);
		}
	}
	
	public static String readUtf8AndRightTrim(final byte[] in) {
		if (in == null) 
			return null;
		
		int idx = whitespaceIndexFromRight(in);
		if ( idx == -1 ) {
			return readUtf8(in);
		} else  {
			return readUtf8(in, 0, idx);
		}
	}

	// trim and parse to String
	public static String readUtf8AndTrim(byte[] in, int start, int end) {
		if (in == null)
			return null;

		// left trim
        while ((start < end) && (in[start] <= ' ')) {
            start++;
        }
        // right trim
        while ((start < end) && (in[end - 1] <= ' ')) {
            end--;
        }

        return readUtf8(in, start, end - start);
	}
	
	private static int whitespaceIndexFromRight(byte[] bytes) {
		//
		int whitespaceIdx = -1;
		for(int i = bytes.length - 1; i >= 0; i--) {
			// isWhitespace
			boolean isWhitespace = ( bytes[i] <= ' ' );
			if ( isWhitespace ) {
				whitespaceIdx = i;
			} else {
				return whitespaceIdx;
			}
		}
		return whitespaceIdx;
	}
	
	public static void main(String[] args) {
		byte[] bb = "xxxxx0  ".getBytes();
		System.out.println( readUtf8AndRightTrim(bb).length() );
	}

}
