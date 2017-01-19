package gov.va.isaac.sync.git.gitblit.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Utility class for establishing HTTP/HTTPS connections.
 *
 *
 */
public class ConnectionUtils
{
	static final String CHARSET;

	static
	{
		CHARSET = "UTF-8";
		// Disable Java 7 SNI checks
		// http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
		System.setProperty("jsse.enableSNIExtension", "false");
	}

	public static void setAuthorization(URLConnection conn, String username, char[] password)
	{
		if (!StringUtils.isEmpty(username) && (password != null && password.length > 0))
		{
			conn.setRequestProperty("Authorization",
					"Basic " + Base64.getEncoder().encodeToString(toBytes(ArrayUtils.addAll(new String(username + ":").toCharArray(), password))));
		}
	}

	private static byte[] toBytes(char[] chars)
	{
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}
	
	public static URLConnection openReadConnection(String url, String username, char[] password)
			throws IOException {
		URLConnection conn = openConnection(url, username, password);
		conn.setRequestProperty("Accept-Charset", ConnectionUtils.CHARSET);
		return conn;
	}

	public static URLConnection openConnection(String url, String username, char[] password) throws IOException
	{
		URL urlObject = new URL(url);
		URLConnection conn = urlObject.openConnection();
		setAuthorization(conn, username, password);
		conn.setUseCaches(false);
		conn.setDoOutput(true);
		return conn;
	}
}
