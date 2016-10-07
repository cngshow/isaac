package gov.va.isaac.sync.git.gitblit.utils;

import java.util.Locale;

/**
 * Utility class of string functions.
 *
 *
 */
public class StringUtils {
	/**
	 * Returns true if the string is null or empty.
	 *
	 * @param value
	 * @return true if string is null or empty
	 */
	public static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Compare two repository names for proper group sorting.
	 *
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static int compareRepositoryNames(String r1, String r2) {
		// sort root repositories first, alphabetically
		// then sort grouped repositories, alphabetically
		r1 = r1.toLowerCase(Locale.ENGLISH);
		r2 = r2.toLowerCase(Locale.ENGLISH);
		int s1 = r1.indexOf('/');
		int s2 = r2.indexOf('/');
		if (s1 == -1 && s2 == -1) {
			// neither grouped
			return r1.compareTo(r2);
		} else if (s1 > -1 && s2 > -1) {
			// both grouped
			return r1.compareTo(r2);
		} else if (s1 == -1) {
			return -1;
		} else if (s2 == -1) {
			return 1;
		}
		return 0;
	}

	/**
	 * Strips a trailing ".git" from the value.
	 *
	 * @param value
	 * @return a stripped value or the original value if .git is not found
	 */
	public static String stripDotGit(String value) {
		if (value.toLowerCase(Locale.ENGLISH).endsWith(".git")) {
			return value.substring(0, value.length() - 4);
		}
		return value;
	}

	/**
	 * Returns the first path element of a path string.  If no path separator is
	 * found in the path, an empty string is returned.
	 *
	 * @param path
	 * @return the first element in the path
	 */
	public static String getFirstPathElement(String path) {
		if (path.indexOf('/') > -1) {
			return path.substring(0, path.indexOf('/')).trim();
		}
		return "";
	}
	/**
	 * Encodes a url parameter by escaping troublesome characters.
	 *
	 * @param inStr
	 * @return properly escaped url
	 */
	public static String encodeURL(String inStr) {
		StringBuilder retStr = new StringBuilder();
		int i = 0;
		while (i < inStr.length()) {
			if (inStr.charAt(i) == '/') {
				retStr.append("%2F");
			} else if (inStr.charAt(i) == ' ') {
				retStr.append("%20");
			} else if (inStr.charAt(i) == '&') {
				retStr.append("%26");
			} else if (inStr.charAt(i) == '+') {
				retStr.append("%2B");
			} else {
				retStr.append(inStr.charAt(i));
			}
			i++;
		}
		return retStr.toString();
	}

}