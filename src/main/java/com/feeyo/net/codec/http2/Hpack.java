package com.feeyo.net.codec.http2;

/**
 * Read and write HPACK v10.
 *
 * http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-12
 *
 * This implementation uses an array for the dynamic table and a list for
 * indexed entries. Dynamic entries are added to the array, starting in the last
 * position moving forward. When the array fills, it is doubled.
 */
final class Hpack {

	public static final int PREFIX_4_BITS = 0x0f;
	public static final int PREFIX_5_BITS = 0x1f;
	public static final int PREFIX_6_BITS = 0x3f;
	public static final int PREFIX_7_BITS = 0x7f;

	public static final Header[] STATIC_HEADER_TABLE = new Header[] { 
			
			new Header(Header.TARGET_AUTHORITY, ""),
			new Header(Header.TARGET_METHOD, "GET"), 
			new Header(Header.TARGET_METHOD, "POST"),
			new Header(Header.TARGET_PATH, "/"), 
			new Header(Header.TARGET_PATH, "/index.html"),
			new Header(Header.TARGET_SCHEME, "http"), 
			new Header(Header.TARGET_SCHEME, "https"),
			new Header(Header.RESPONSE_STATUS, "200"), 
			new Header(Header.RESPONSE_STATUS, "204"),
			new Header(Header.RESPONSE_STATUS, "206"), 
			new Header(Header.RESPONSE_STATUS, "304"),
			new Header(Header.RESPONSE_STATUS, "400"), 
			new Header(Header.RESPONSE_STATUS, "404"),
			new Header(Header.RESPONSE_STATUS, "500"), 
			new Header("accept-charset", ""),
			new Header("accept-encoding", "gzip, deflate"), 
			new Header("accept-language", ""),
			new Header("accept-ranges", ""), 
			new Header("accept", ""), 
			new Header("access-control-allow-origin", ""),
			new Header("age", ""), 
			new Header("allow", ""), 
			new Header("authorization", ""),
			new Header("cache-control", ""), 
			new Header("content-disposition", ""), 
			new Header("content-encoding", ""),
			new Header("content-language", ""), 
			new Header("content-length", ""), 
			new Header("content-location", ""),
			new Header("content-range", ""), 
			new Header("content-type", ""), 
			new Header("cookie", ""),
			new Header("date", ""), 
			new Header("etag", ""), 
			new Header("expect", ""), 
			new Header("expires", ""),
			new Header("from", ""), 
			new Header("host", ""), 
			new Header("if-match", ""),
			new Header("if-modified-since", ""), 
			new Header("if-none-match", ""), 
			new Header("if-range", ""),
			new Header("if-unmodified-since", ""), 
			new Header("last-modified", ""), 
			new Header("link", ""),
			new Header("location", ""), 
			new Header("max-forwards", ""), 
			new Header("proxy-authenticate", ""),
			new Header("proxy-authorization", ""), 
			new Header("range", ""), 
			new Header("referer", ""),
			new Header("refresh", ""), 
			new Header("retry-after", ""), 
			new Header("server", ""),
			new Header("set-cookie", ""), 
			new Header("strict-transport-security", ""),
			new Header("transfer-encoding", ""), 
			new Header("user-agent", ""), 
			new Header("vary", ""),
			new Header("via", ""), 
			new Header("www-authenticate", "") 
	};

	private Hpack() {}
	
}