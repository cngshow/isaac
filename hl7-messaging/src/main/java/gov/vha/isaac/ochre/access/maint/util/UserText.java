/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.vha.isaac.ochre.access.maint.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**************************************************************
 * Formats user messages using message text in a message catalog and program
 * values.
 * <p>
 * Message catalogs are assumed to reside in the classpath under the name
 * specified by the <tt>catalogName</tt> argument. They
 * <p>
 * This class provides a functional interface via static methods as well as an
 * object-oriented interface via instance methods. The object-oriented interface
 * is slightly more efficient if a single component needs to obtain more than
 * one message.
 * <p>
 * <b>Use:</b>
 * <p>
 * <b>Creating message catalogs:</b>
 * <p>
 * A message catalog is a java.util.ResourceBundle with property values in a
 * format usable by java.text.MessageFormat. Typically, message catalogs are
 * properties files. Since an application message may result in a user calling a
 * help desk, the property keys for application messages should be a
 * one-to-three character alpha identifier plus a 5-to-7 digit number, and
 * should be unique throughout the system.
 * <p>
 * It is recommended that message catalogs implemented as properties files be
 * stored and maintained in the source path, and copied to the class path by the
 * build script.
 * <p>
 * An example message file excerpt:
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * #**************************************************************** # U00000 -
 * U00999 : Security Errors
 * #**************************************************************** U00001:
 * Please enter a valid user id.
 * #**************************************************************** # U01000 -
 * U01999 : Data Entry Errors
 * #**************************************************************** U01000: The
 * value entered for {0} is not valid. ({1}) U01008: The value entered for {0}
 * has more than {2} digits after \ the decimal point. ({1})</td>
 * </tr>
 * </table>
 * <p>
 * <b>Internationalizing application messages:</b>
 * <p>
 * See the documentation for 'java.util.ResourceBundle' for how to name message
 * files for different locales.
 * <p>
 * <b>Obtaining an application message:</b>
 * <p>
 * To obtain an application message, call one of the 'get()' functions in this
 * class.
 * <p>
 * In the simplest form, get() returns the message in the message file. If the
 * message is not found, the default message passed as an argument is returned.
 * The default message is useful for two reasons: First, it provides a failsafe
 * mechanism if somehow the message files are not available. Second, it provides
 * documentation to help maintainers understand which message is being obtained.
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * String message = UserText.get("com.eds.abc.messages.UserErrors", "U00001",
 * "Please enter a valid user id");</td>
 * </tr>
 * </table>
 * <p>
 * In more complex forms, get() also takes program value arguments for
 * replacement into the message text by 'java.text.MessageFormat'. For example:
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * String fieldname = "Amount"; String amountInput = "123.456"; String message =
 * UserText.get("com.eds.abc.messages.UserErrors", "U00008", fieldname,
 * amountInput, 2, "The value entered for {0} has more than {2} digits" + "
 * after the decimal point. ({1})");
 *
 * // message now contains: // The value entered for Amount has more than 2
 * digits // after the decimal point. (123.456)</td>
 * </tr>
 * </table>
 *************************************************************/
public class UserText
{
	private static Logger logger_ = LogManager.getLogger(UserText.class);

	private Locale locale_;
	private ResourceBundle bundle_;
	private String catalogName_;

	public UserText(String catalogName) {
		locale_ = Locale.getDefault();
		bundle_ = bundleFor_(Locale.getDefault(), catalogName);
		catalogName_ = catalogName;
	}

	public UserText(Locale locale, String catalogName) {
		locale_ = (locale == null) ? Locale.getDefault() : locale;
		bundle_ = bundleFor_(locale_, catalogName);
		catalogName_ = catalogName;
	}

	/**
	 * Mostly used for testing.
	 **/
	public UserText(ResourceBundle bundle) {
		locale_ = null;
		bundle_ = bundle;
		catalogName_ = "(program-supplied catalog)";
	}

	private static ResourceBundle bundleFor_(Locale locale, String catalogName) {
		ResourceBundle result = null;

		try {
			result = ResourceBundle.getBundle(catalogName, locale);
		} catch (MissingResourceException x) {
			if (logger_.isInfoEnabled()) {
				logger_.info("UserText catalog not found: " + catalogName, x);
			}
		}

		return result;
	}

	public String get(String key, String defaultMessage) {
		String result = null;
		if (bundle_ != null) {
			try {
				result = bundle_.getString(key);
			} catch (MissingResourceException x) {
				if (logger_.isInfoEnabled()) {
					logger_.info("UserText catalog entry not found: " + catalogName_
							+ (locale_ == Locale.getDefault() ? "" : " [" + locale_.toString() + "]") + ": '" + key
							+ "'", x);
				}
			}
		}

		if (result == null) {
			result = defaultMessage;
		}

		return result;
	}

	public String get(String key, Object arg1, String defaultMessage) {
		final String message = get(key, defaultMessage);
		final Object[] a = { arg1 };
		return MessageFormat.format(message, a);
	}

	public String get(String key, Object arg1, Object arg2, String defaultMessage) {
		final String message = get(key, defaultMessage);
		final Object[] a = { arg1, arg2 };

		return MessageFormat.format(message, a);
	}

	public String get(String key, Object arg1, Object arg2, Object arg3, String defaultMessage) {
		final String message = get(key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3 };
		return MessageFormat.format(message, a);
	}

	public String get(String key, Object arg1, Object arg2, Object arg3, Object arg4, String defaultMessage) {
		final String message = get(key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4 };
		return MessageFormat.format(message, a);
	}

	public String get(String key, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,
			String defaultMessage) {
		final String message = get(key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4, arg5 };
		return MessageFormat.format(message, a);
	}

	public String get(String key, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6,
			String defaultMessage) {
		final String message = get(key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4, arg5, arg6 };
		return MessageFormat.format(message, a);
	}

	public String get(String key, Object[] args, String defaultMessage) {
		final String message = get(key, defaultMessage);
		return MessageFormat.format(message, args);
	}

	// ============================================================
	// Static methods
	// ============================================================

	public static String get(String catalogName, String key, String defaultMessage) {
		return get(Locale.getDefault(), catalogName, key, defaultMessage);
	}

	public static String get(String catalogName, String key, Object arg1, String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		final Object[] a = { arg1 };
		return MessageFormat.format(message, a);
	}

	public static String get(String catalogName, String key, Object arg1, Object arg2, String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2 };

		return MessageFormat.format(message, a);
	}

	public static String get(String catalogName, String key, Object arg1, Object arg2, Object arg3,
			String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3 };
		return MessageFormat.format(message, a);
	}

	public static String get(String catalogName, String key, Object arg1, Object arg2, Object arg3, Object arg4,
			String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4 };
		return MessageFormat.format(message, a);
	}

	public static String get(String catalogName, String key, Object arg1, Object arg2, Object arg3, Object arg4,
			Object arg5, String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4, arg5 };
		return MessageFormat.format(message, a);
	}

	public static String get(String catalogName, String key, Object arg1, Object arg2, Object arg3, Object arg4,
			Object arg5, Object arg6, String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4, arg5, arg6 };
		return MessageFormat.format(message, a);
	}

	public static String get(String catalogName, String key, Object[] args, String defaultMessage) {
		final String message = get(catalogName, key, defaultMessage);
		return MessageFormat.format(message, args);
	}

	// Locale-specific methods

	public static String get(Locale locale, String catalogName, String key, String defaultMessage) {
		String result = null;

		final ResourceBundle bundle = bundleFor_(locale, catalogName);

		if (bundle != null) {
			try {
				result = bundle.getString(key);
			} catch (Exception x) {
				if (logger_.isInfoEnabled()) {
					logger_.info("UserText entry not found: " + catalogName
							+ (locale == Locale.getDefault() ? "" : " [" + locale.toString() + "]") + ": '" + key + "'",
							x);
				}
			}
		}

		if (result == null) {
			result = defaultMessage;
		}

		return result;
	}

	public static String get(Locale locale, String catalogName, String key, Object arg1, String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		final Object[] a = { arg1 };
		return MessageFormat.format(message, a);
	}

	public static String get(Locale locale, String catalogName, String key, Object arg1, Object arg2,
			String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2 };

		return MessageFormat.format(message, a);
	}

	public static String get(Locale locale, String catalogName, String key, Object arg1, Object arg2, Object arg3,
			String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3 };
		return MessageFormat.format(message, a);
	}

	public static String get(Locale locale, String catalogName, String key, Object arg1, Object arg2, Object arg3,
			Object arg4, String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4 };
		return MessageFormat.format(message, a);
	}

	public static String get(Locale locale, String catalogName, String key, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5, String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4, arg5 };
		return MessageFormat.format(message, a);
	}

	public static String get(Locale locale, String catalogName, String key, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5, Object arg6, String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		final Object[] a = { arg1, arg2, arg3, arg4, arg5, arg6 };
		return MessageFormat.format(message, a);
	}

	public static String get(Locale locale, String catalogName, String key, Object[] args, String defaultMessage) {
		final String message = get(locale, catalogName, key, defaultMessage);
		return MessageFormat.format(message, args);
	}
}
