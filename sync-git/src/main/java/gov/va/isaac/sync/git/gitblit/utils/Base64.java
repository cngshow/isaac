//
//  NOTE: The following source code is heavily derived from the
//  iHarder.net public domain Base64 library.  See the original at
//  http://iharder.sourceforge.net/current/java/base64/
//

package gov.va.isaac.sync.git.gitblit.utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Encodes and decodes to and from Base64 notation.
 * <p>
 * I am placing this code in the Public Domain. Do with it as you will. This
 * software comes with no guarantees or warranties but with plenty of
 * well-wishing instead! Please visit <a
 * href="http://iharder.net/base64">http://iharder.net/base64</a> periodically
 * to check for updates or to contribute improvements.
 * </p>
 *
 * @author Robert Harder
 * @author rob@iharder.net
 * @version 2.1, stripped to minimum feature set used by JGit.
 */
public class Base64 {
	/** The equals sign (=) as a byte. */
	private final static byte EQUALS_SIGN = (byte) '=';

	/** Indicates equals sign in encoding. */
	private final static byte EQUALS_SIGN_DEC = -1;

	/** Indicates white space in encoding. */
	private final static byte WHITE_SPACE_DEC = -2;

	/** Indicates an invalid byte during decoding. */
	private final static byte INVALID_DEC = -3;

	/** Preferred encoding. */
	private final static String UTF_8 = "UTF-8";

	/** The 64 valid Base64 values. */
	private final static byte[] ENC;

	/**
	 * Translates a Base64 value to either its 6-bit reconstruction value or a
	 * negative number indicating some other meaning. The table is only 7 bits
	 * wide, as the 8th bit is discarded during decoding.
	 */
	private final static byte[] DEC;

	static {
		try {
			ENC = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" //
					+ "abcdefghijklmnopqrstuvwxyz" //
					+ "0123456789" //
					+ "+/" //
			).getBytes(UTF_8);
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException(uee.getMessage(), uee);
		}

		DEC = new byte[128];
		Arrays.fill(DEC, INVALID_DEC);

		for (int i = 0; i < 64; i++)
			DEC[ENC[i]] = (byte) i;
		DEC[EQUALS_SIGN] = EQUALS_SIGN_DEC;

		DEC['\t'] = WHITE_SPACE_DEC;
		DEC['\n'] = WHITE_SPACE_DEC;
		DEC['\r'] = WHITE_SPACE_DEC;
		DEC[' '] = WHITE_SPACE_DEC;
	}

	/** Defeats instantiation. */
	private Base64() {
		// Suppress empty block warning.
	}

	/**
	 * Encodes up to three bytes of the array <var>source</var> and writes the
	 * resulting four Base64 bytes to <var>destination</var>. The source and
	 * destination arrays can be manipulated anywhere along their length by
	 * specifying <var>srcOffset</var> and <var>destOffset</var>. This method
	 * does not check to make sure your arrays are large enough to accommodate
	 * <var>srcOffset</var> + 3 for the <var>source</var> array or
	 * <var>destOffset</var> + 4 for the <var>destination</var> array. The
	 * actual number of significant bytes in your array is given by
	 * <var>numSigBytes</var>.
	 *
	 * @param source
	 *            the array to convert
	 * @param srcOffset
	 *            the index where conversion begins
	 * @param numSigBytes
	 *            the number of significant bytes in your array
	 * @param destination
	 *            the array to hold the conversion
	 * @param destOffset
	 *            the index where output will be put
	 */
	private static void encode3to4(byte[] source, int srcOffset, int numSigBytes,
			byte[] destination, int destOffset) {
		// We have to shift left 24 in order to flush out the 1's that appear
		// when Java treats a value as negative that is cast from a byte.

		int inBuff = 0;
		switch (numSigBytes) {
		case 3:
			inBuff |= (source[srcOffset + 2] << 24) >>> 24;
			//$FALL-THROUGH$

		case 2:
			inBuff |= (source[srcOffset + 1] << 24) >>> 16;
			//$FALL-THROUGH$

		case 1:
			inBuff |= (source[srcOffset] << 24) >>> 8;
		}

		switch (numSigBytes) {
		case 3:
			destination[destOffset] = ENC[(inBuff >>> 18)];
			destination[destOffset + 1] = ENC[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = ENC[(inBuff >>> 6) & 0x3f];
			destination[destOffset + 3] = ENC[(inBuff) & 0x3f];
			break;

		case 2:
			destination[destOffset] = ENC[(inBuff >>> 18)];
			destination[destOffset + 1] = ENC[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = ENC[(inBuff >>> 6) & 0x3f];
			destination[destOffset + 3] = EQUALS_SIGN;
			break;

		case 1:
			destination[destOffset] = ENC[(inBuff >>> 18)];
			destination[destOffset + 1] = ENC[(inBuff >>> 12) & 0x3f];
			destination[destOffset + 2] = EQUALS_SIGN;
			destination[destOffset + 3] = EQUALS_SIGN;
			break;
		}
	}

	/**
	 * Encodes a byte array into Base64 notation.
	 *
	 * @param source
	 *            The data to convert
	 * @return encoded base64 representation of source.
	 */
	public static String encodeBytes(byte[] source) {
		return encodeBytes(source, 0, source.length);
	}

	/**
	 * Encodes a byte array into Base64 notation.
	 *
	 * @param source
	 *            The data to convert
	 * @param off
	 *            Offset in array where conversion should begin
	 * @param len
	 *            Length of data to convert
	 * @return encoded base64 representation of source.
	 */
	public static String encodeBytes(byte[] source, int off, int len) {
		final int len43 = len * 4 / 3;

		byte[] outBuff = new byte[len43 + ((len % 3) > 0 ? 4 : 0)];
		int d = 0;
		int e = 0;
		int len2 = len - 2;

		for (; d < len2; d += 3, e += 4)
			encode3to4(source, d + off, 3, outBuff, e);

		if (d < len) {
			encode3to4(source, d + off, len - d, outBuff, e);
			e += 4;
		}

		try {
			return new String(outBuff, 0, e, UTF_8);
		} catch (UnsupportedEncodingException uue) {
			return new String(outBuff, 0, e);
		}
	}
}
