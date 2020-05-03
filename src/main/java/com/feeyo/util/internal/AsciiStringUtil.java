package com.feeyo.util.internal;

public final class AsciiStringUtil {

	public static byte[] unsafeEncode(final CharSequence in) {
		final int len = in.length();
		final byte[] out = new byte[len];
		for (int i = 0; i < len; i++) {
			out[i] = (byte) in.charAt(i);
		}
		return out;
	}

	public static String unsafeDecode(final byte[] in) {
		return unsafeDecode(in, 0, in.length);
	}
	
	public static String unsafeDecode(final byte[] in, int off, int len) {
		final char[] out = new char[len];
		for (int i = off; i < len; i++) {
			out[i] = (char) (in[i] & 0xFF);
		}
		return UnsafeUtil.moveToString(out);
	}

	private AsciiStringUtil() {
	}

}
