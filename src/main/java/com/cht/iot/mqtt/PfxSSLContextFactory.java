package com.cht.iot.mqtt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

public class PfxSSLContextFactory {
	static final String PROTOCOL = "TLS";
	static final String KEY_MANAGER_FACTORY_ALGORITHM = KeyManagerFactory.getDefaultAlgorithm();

	/**
	 * openssl pkcs12 -export -out ai-ss.ddns.net.pfx -inkey private.key -in certificate.crt
	 * 
	 * openssl pkcs12 -export -out ai.hinet.net.pfx -inkey ibobbyserver.key -in CertB64.cer -certfile ROOTeCA_64.crt -certfile PublicCA_64.crt -certfile eCA_PublicCA.crt
	 * 
	 */

//	static final String KEYSTORE = "/ai-ss.ddns.net.pfx";
//	static final char[] KEYSTORE_KEY = "abcd1234".toCharArray();
	
//	static final String KEYSTORE = "/aispeakerqa.hisales.hinet.net.pfx";
//	static final char[] KEYSTORE_KEY = "cht12345".toCharArray();
	
	static final String KEYSTORE = "/ai.hinet.net.pfx";
	static final char[] KEYSTORE_KEY = "cht12345".toCharArray();

	public PfxSSLContextFactory() {
	}

	public static final SSLContext createServerSSLContext() throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(PfxSSLContextFactory.class.getResourceAsStream(KEYSTORE), KEYSTORE_KEY);

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
		kmf.init(ks, KEYSTORE_KEY);

		SSLContext ssl = SSLContext.getInstance(PROTOCOL);
		ssl.init(kmf.getKeyManagers(), new BypassTrustManagerFactory().getTrustManagers(), null);

		return ssl;
	}

	public static final SSLContext createClientSSLContext()	throws GeneralSecurityException {
		SSLContext ssl = SSLContext.getInstance(PROTOCOL);
		ssl.init(null, new BypassTrustManagerFactory().getTrustManagers(), null);

		return ssl;
	}

	static class BypassTrustManagerFactory extends TrustManagerFactorySpi {
		final X509TrustManager x509 = new X509TrustManager() {
			
			@Override
			public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		};

		final TrustManager[] managers = new TrustManager[] { x509 };

		public BypassTrustManagerFactory() {
		}

		public TrustManager[] getTrustManagers() {
			return managers;
		}

		@Override
		protected TrustManager[] engineGetTrustManagers() {
			return managers;
		}

		@Override
		protected void engineInit(KeyStore ks) throws KeyStoreException {
		}

		@Override
		protected void engineInit(ManagerFactoryParameters mfp) throws InvalidAlgorithmParameterException {
		}
	}
}
