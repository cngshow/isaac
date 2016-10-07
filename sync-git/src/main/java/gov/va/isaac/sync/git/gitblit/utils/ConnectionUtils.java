package gov.va.isaac.sync.git.gitblit.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.ArrayUtils;


/**
 * Utility class for establishing HTTP/HTTPS connections.
 *
 *
 */
public class ConnectionUtils {

	static final String CHARSET;

	private static final SSLContext SSL_CONTEXT;

	private static final DummyHostnameVerifier HOSTNAME_VERIFIER;

	static {
		SSLContext context = null;
		try {
			context = SSLContext.getInstance("SSL");
			context.init(null, new TrustManager[] { new DummyTrustManager() }, new SecureRandom());
		} catch (Throwable t) {
			t.printStackTrace();
		}
		SSL_CONTEXT = context;
		HOSTNAME_VERIFIER = new DummyHostnameVerifier();
		CHARSET = "UTF-8";

		// Disable Java 7 SNI checks
		// http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
		System.setProperty("jsse.enableSNIExtension", "false");
	}

	public static void setAuthorization(URLConnection conn, String username, char[] password) {
		if (!StringUtils.isEmpty(username) && (password != null && password.length > 0)) {
			
			conn.setRequestProperty(
					"Authorization",
					"Basic "
							+ Base64.encodeBytes(toBytes(
									ArrayUtils.addAll(
											new String(username + ":").toCharArray(), password))));
		}
	}
	
	private static byte[] toBytes(char[] chars) {
	    CharBuffer charBuffer = CharBuffer.wrap(chars);
	    ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
	    byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
	            byteBuffer.position(), byteBuffer.limit());
	    Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
	    Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
	    return bytes;
	}


	public static URLConnection openConnection(String url, String username, char[] password)
			throws IOException {
		URL urlObject = new URL(url);
		URLConnection conn = urlObject.openConnection();
		setAuthorization(conn, username, password);
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		if (conn instanceof HttpsURLConnection) {
			HttpsURLConnection secureConn = (HttpsURLConnection) conn;
			secureConn.setSSLSocketFactory(SSL_CONTEXT.getSocketFactory());
			secureConn.setHostnameVerifier(HOSTNAME_VERIFIER);
		}
		return conn;
	}

	/**
	 * DummyTrustManager trusts all certificates.
	 *
	 */
	private static class DummyTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] certs, String authType)
				throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	/**
	 * Trusts all hostnames from a certificate, including self-signed certs.
	 *
	 */
	private static class DummyHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}
}
