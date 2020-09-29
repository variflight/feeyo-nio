package com.feeyo.net.codec.http;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * TODO consider replacing this with java.net.HttpCookie (once it supports RFC6265)
 */
public class HttpCookie {
	
	private static final String __COOKIE_DELIM = "\",;\\ \t";
    private static final String __01Jan1970_COOKIE = HttpDate.format(0);
	//
	private final String name;
    private final String value;
    private final String comment;
    private final String domain;
    private final long maxAge;
    private final String path;
    private final boolean secure;
    private final int version;
    private final boolean httpOnly;
    private final long expiration;
    
    ///
	public HttpCookie(String name, String value) {
		this(name, value, -1);
	}

	public HttpCookie(String name, String value, String domain, String path) {
		this(name, value, domain, path, -1, false, false);
	}

	public HttpCookie(String name, String value, long maxAge) {
		this(name, value, null, null, maxAge, false, false);
	}

	public HttpCookie(String name, String value, String domain, String path, long maxAge, boolean httpOnly,
			boolean secure) {
		this(name, value, domain, path, maxAge, httpOnly, secure, null, 0);
	}

	public HttpCookie(String name, String value, String domain, String path, long maxAge, boolean httpOnly,
			boolean secure, String comment, int version) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.maxAge = maxAge;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.comment = comment;
        this.version = version;
        this.expiration = maxAge < 0 ? -1 : System.nanoTime() + TimeUnit.SECONDS.toNanos(maxAge);
    }

	public static List<HttpCookie> parse(String setCookie) {
		//
		List<HttpCookie> result = new ArrayList<>();
		//
		List<java.net.HttpCookie> cookies = java.net.HttpCookie.parse(setCookie);
		for(java.net.HttpCookie cookie: cookies) {
			String _name = cookie.getName();
			String _value = cookie.getValue();
			String _domain = cookie.getDomain();
			String _path = cookie.getPath();
			long _maxAge = cookie.getMaxAge();
			boolean _httpOnly = cookie.isHttpOnly();
			boolean _secure = cookie.getSecure();
			String _comment = cookie.getComment();
			int _version = cookie.getVersion();
			result.add( new HttpCookie(_name, _value, _domain, _path, _maxAge, _httpOnly, _secure, _comment, _version) );
		}
		return result;
	}

	public boolean isExpired(long timeNanos) {
		return expiration >= 0 && timeNanos >= expiration;
	}

	public String getComment() {
		return comment;
	}

	public String getDomain() {
		return domain;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public String getPath() {
		return path;
	}

	public boolean getSecure() {
		return secure;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	//
	public int getVersion() {
		return version;
	}

	//
	public boolean isHttpOnly() {
		return httpOnly;
	}

	@Override
	public String toString() {
		if (getVersion() > 0) {
			return getRFC2965SetCookie();
		} else {
			return getRFC6265SetCookie();
		}
	}

    
    /**
     * Does a cookie value need to be quoted?
     *
     * @param s value string
     * @return true if quoted;
     * @throws IllegalArgumentException If there a control characters in the string
     */
	private static boolean isQuoteNeededForCookie(String s) {
		if (s == null || s.length() == 0)
			return true;
		//
		if (QuotedStringTokenizer.isQuoted(s))
			return false;
		//
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (__COOKIE_DELIM.indexOf(c) >= 0)
				return true;
			if (c < 0x20 || c >= 0x7f)
				throw new IllegalArgumentException("Illegal character in cookie value");
		}
		return false;
	}
	
	private static void quoteOnlyOrAppend(StringBuilder buf, String s, boolean quote) {
		if (quote)
			QuotedStringTokenizer.quoteOnly(buf, s);
		else
			buf.append(s);
	}
    
    //
	public String getRFC2965SetCookie() {
        // Check arguments
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Bad cookie name");

        // Format value and params
        StringBuilder buf = new StringBuilder();

        // Name is checked for legality by servlet spec, but can also be passed directly so check again for quoting
        boolean quoteName = isQuoteNeededForCookie(name);
        quoteOnlyOrAppend(buf, name, quoteName);
        
        buf.append('=');

        // Append the value
        boolean quoteValue = isQuoteNeededForCookie(value);
        quoteOnlyOrAppend(buf, value, quoteValue);

        // Look for domain and path fields and check if they need to be quoted
        boolean hasDomain = domain != null && domain.length() > 0;
        boolean quoteDomain = hasDomain && isQuoteNeededForCookie(domain);
        boolean hasPath = path != null && path.length() > 0;
        boolean quotePath = hasPath && isQuoteNeededForCookie(path);

        // Upgrade the version if we have a comment or we need to quote value/path/domain or if they were already quoted
        int _version = version;
        if (_version == 0 && (comment != null || quoteName || quoteValue || quoteDomain || quotePath ||
            QuotedStringTokenizer.isQuoted(name) || QuotedStringTokenizer.isQuoted(value) ||
            QuotedStringTokenizer.isQuoted(path) || QuotedStringTokenizer.isQuoted(domain)))
        	_version = 1;

        // Append version
		if (_version == 1)
			buf.append(";Version=1");
		else if (_version > 1)
			buf.append(";Version=").append(version);

		// Append path
		if (hasPath) {
            buf.append(";Path=");
            quoteOnlyOrAppend(buf, path, quotePath);
        }

        // Append domain
		if (hasDomain) {
			buf.append(";Domain=");
			quoteOnlyOrAppend(buf, domain, quoteDomain);
		}

		// Handle max-age and/or expires
		if (maxAge >= 0) {
            // Always use expires
            // This is required as some browser (M$ this means you!) don't handle max-age even with v1 cookies
            buf.append(";Expires=");
            if (maxAge == 0)
                buf.append(__01Jan1970_COOKIE);
            else
                buf.append(HttpDate.format(System.currentTimeMillis() + 1000L * maxAge));

            // for v1 cookies, also send max-age
			if (_version >= 1) {
                buf.append(";Max-Age=");
                buf.append(maxAge);
            }
        }
		//
        // add the other fields
        if (secure)
            buf.append(";Secure");
        if (httpOnly)
            buf.append(";HttpOnly");
		if (comment != null) {
            buf.append(";Comment=");
            quoteOnlyOrAppend(buf, comment, isQuoteNeededForCookie(comment));
        }
        return buf.toString();
    }
    
	//
    public String getRFC6265SetCookie() {
        // Check arguments
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Bad cookie name");
        //
        // Format value and params
		StringBuilder buf = new StringBuilder();
		buf.append(name).append('=').append(value == null ? "" : value);

		// Append path
		if (path != null && path.length() > 0)
			buf.append("; Path=").append(path);

		// Append domain
		if (domain != null && domain.length() > 0)
			buf.append("; Domain=").append(domain);

		// Handle max-age and/or expires
		if (maxAge >= 0) {
            // Always use expires
            // This is required as some browser (M$ this means you!) don't handle max-age even with v1 cookies
            buf.append("; Expires="); 
            if (maxAge == 0)
                buf.append(__01Jan1970_COOKIE);
            else
            	buf.append( HttpDate.format( System.currentTimeMillis() + 1000L * maxAge ) );

            buf.append("; Max-Age=");
            buf.append(maxAge);
        }

        // add the other fields
        if (secure)
            buf.append("; Secure");
        if (httpOnly)
            buf.append("; HttpOnly");
        return buf.toString();
    }
}