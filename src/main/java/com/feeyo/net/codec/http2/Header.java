package com.feeyo.net.codec.http2;

public final class Header {
	//
	// Special header names defined in HTTP/2 spec.
	public static final byte[] PSEUDO_PREFIX = Util.encodeUtf8(":");
	public static final byte[] RESPONSE_STATUS = Util.encodeUtf8(":status");
	public static final byte[] TARGET_METHOD = Util.encodeUtf8(":method");
	public static final byte[] TARGET_PATH = Util.encodeUtf8(":path");
	public static final byte[] TARGET_SCHEME = Util.encodeUtf8(":scheme");
	public static final byte[] TARGET_AUTHORITY = Util.encodeUtf8(":authority");

	// Name in case-insensitive ASCII encoding
	public final byte[] name;
	
	// Value in UTF-8 encoding
	public final byte[] value;
	
	final int hpackSize;

	public Header(String name, String value) {
		this(Util.encodeUtf8(name), Util.encodeUtf8(value));
	}

	public Header(byte[] name, String value) {
		this(name, Util.encodeUtf8(value));
	}

	public Header(byte[] name, byte[] value) {
		this.name = name;
		this.value = value;
		this.hpackSize = 32 + name.length + value.length;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Header) {
			Header that = (Header) other;
			return this.name.equals(that.name) && this.value.equals(that.value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + name.hashCode();
		result = 31 * result + value.hashCode();
		return result;
	}
}