package com.feeyo.net.nio.ssl;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class SslContextUtil {
	
	public static SSLContext getSslContext1() {
		SSLContext sslContext = null;
		try {
			//
			// Configure SSL
	        char[] password = "password".toCharArray();
	        KeyStore keystore = KeyStore.getInstance("JKS");
	        keystore.load(new FileInputStream("/Users/zhuam/git/feeyo/feeyonio/src/main/java/com/feeyo/net/nio/ssl/test/testkey.jks"), password);
	        //
	        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	        keyManagerFactory.init(keystore, password);
	        //
	        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
	        trustManagerFactory.init(keystore);
	        //
	        sslContext = SSLContext.getInstance("TLS");
	        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sslContext;
	}

	public static SSLContext getSslContext() {
		SSLContext sslContext = null;
		try {
			//
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sslContext;
	}

	//
	private static TrustManager trustManager = new X509TrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			// don't check
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			// don't check
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	};



}