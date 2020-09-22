package com.feeyo.net.codec.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class UriUtil {
	
	///
	public static String parsePath(String uriString) {
		return parsePath(true, uriString);
	}
    
    ///
	public static String parsePath(boolean findSchemeSeparator, String uriString) {
		//
		// findSchemeSeparator
		int ssi = -1;
		//
		if ( findSchemeSeparator ) {
			//
			ssi = uriString.indexOf(':');
			//
			// If the URI is absolute.
			if (ssi > -1) {
				// Is there anything after the ':'?
				boolean schemeOnly = ssi + 1 == uriString.length();
				if (schemeOnly) {
					// Opaque URI.
					return null;
				}
				// A '/' after the ':' means this is hierarchical.
				if (uriString.charAt(ssi + 1) != '/') {
					// Opaque URI.
					return null;
				}
			} else {
				// All relative URIs are hierarchical.
			}
		}

		int length = uriString.length();

		// Find start of path.
		int pathStart;
		if (length > ssi + 2 && uriString.charAt(ssi + 1) == '/' && uriString.charAt(ssi + 2) == '/') {
			// Skip over authority to path.
			pathStart = ssi + 3;
			LOOP: while (pathStart < length) {
				switch (uriString.charAt(pathStart)) {
				case '?': 		// Start of query
				case '#': 		// Start of fragment
					return "";  // Empty path.
				case '/': 		// Start of path!
					break LOOP;
				}
				pathStart++;
			}
		} else {
			// Path starts immediately after scheme separator.
			pathStart = ssi + 1;
		}

		// Find end of path.
		int pathEnd = pathStart;
		LOOP: while (pathEnd < length) {
			switch (uriString.charAt(pathEnd)) {
			case '?': // Start of query
			case '#': // Start of fragment
				break LOOP;
			}
			pathEnd++;
		}
		return uriString.substring(pathStart, pathEnd);
	}
	
	//
	public static Map<String, String> parseParameters(String uriString) {
		return parseParameters(true, uriString);
	}
    
	public static Map<String, String> parseParameters(boolean findSchemeSeparator, String uriString) {

		Map<String, String> parameters = new HashMap<String, String>();
		//
		// calculate it once
		int qsi = findSchemeSeparator ? uriString.indexOf('?', uriString.indexOf(':')) : uriString.indexOf('?');
		if ( qsi >= 0 ) {
			uriString = uriString.substring(qsi + 1, uriString.length());
		}
		
		String[] querys = uriString.split("&");
		for (String query : querys) {
			String[] pair = query.split("=");
			if (pair.length == 2) {
				try {
					parameters.put(URLDecoder.decode(pair[0], "UTF8"), URLDecoder.decode(pair[1], "UTF8"));
				} catch (UnsupportedEncodingException e) {
					parameters.put(pair[0], pair[1]);
				}
			}
		}
		return parameters;
	}   
	
	public static void main(String[] args) {
		String uri = "/raft/cli?cmd=getNodes";
		 //uri = "http://127.0.0.1:9090/raft/cli?cmd=getNodes";
		Map<String, String> parameters = parseParameters(uri);
		System.out.println( parameters.get("cmd") );
		
		String path = parsePath(uri);
		System.out.println( path );
	}

}
