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

import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

/**************************************************************
 * A container for user error messages.
 * <p>
 * This class has the following distinctive features:
 * <ul>
 * <li>Supports both sequential and keyed message access
 * <li>Supports field-specific and general (non-field-specific) messages
 * <li>Supports population of messages both directly and through a message
 * catalog, via a {@link UserMessages UserMessages} object.
 * <li>Supports dynamic adaptation of keys and labels provided to the container.
 * This allows domain class edits to be decoupled from user interface field
 * implementation details.
 * <li>Supports the decorator design pattern for extensibility.
 * </ul>
 * <p>
 * User interfaces require several different kinds of access methods for error
 * messages. For example, an HTML interface may use any of the following
 * designs:
 * <ol>
 * <li>Error messages appear on a subsequent page. The user presses the Back
 * button to return to the data entry page and correct the errors. This design
 * requires sequential access to error messages in the order they were
 * generated. Reported error messages must include the field label so the user
 * can associate them with the field in error.
 * <li>The data entry page is redisplayed, with error messages adjacent to each
 * input field. This design requires keyed access to error messages, using a key
 * unique to each input field. Reported error messages should not include the
 * field label.
 * <li>The data entry page is redisplayed, with a list of error messages
 * independent of the data entry fields, and each erroneous field flagged in
 * some way. This design requires both keyed and sequential access. Reported
 * error messages must include the field label so the user can associate them
 * with the field in error.
 * </ol>
 * <p>
 * This container supports each of these designs transparently. The result is
 * that validation logic is greatly simplified, and becomes completely
 * independent of the user interface.
 * <p>
 * <b>Catalog-based Messages</b>
 * <p>
 * This container allows error messages to be provided either as Strings or as
 * references to message catalog entries. The message catalog interface allows a
 * client to add messages to the container by supplying a message catalog key,
 * an optional list of variable values, and a default value for the message
 * catalog entry.
 * <p>
 * UserErrors delegates message catalog access to a {@link UserMessages
 * UserMessages} object that may be provided at construction. The message is
 * retrieved from the catalog when the message is added to the container. If a
 * catalog was not provided at construction or an entry corresponding to the key
 * is not found, the default message is used.
 * <p>
 * This approach allows domain classes to specify error messages while allowing
 * the user interface to control the text of those messages.
 * <p>
 * <b>Use:</b>
 * <ul>
 * <li>Create a UserErrors container before invoking any field conversion or
 * validation methods. Pass the same container to each method.
 * <p>
 * <li>To create a UserErrors container that is not message catalog based, use
 * the default constructor.
 * <li>To create a message-catalog-based UserErrors container, use the
 * constructor that takes a UserMessages object.
 * <li>To add a general error message to the container, use
 * <code>put(String)</code>.
 * <li>To add a catalog-based general error message to the container, use
 * <code>put(key, args, defaultMessage)</code>.
 * <li>To add a field-specific error message to the container, use
 * <code>put(value, name, label, message)</code> or
 * <code>put(value, name, message)</code>. The name will be used as the field
 * error message key. The label will be displayed to the user if the error is
 * not accessed by key. If the label is not provided it is given the same value
 * as the name.
 * <li>To add a catalog-based field-specific error message to the container, use
 * <code>put(value, name, label, key, args, defaultMessage)</code> or
 * <code>put(value, name, message, key, args, defaultMessage)</code>.
 * <li>For convenience in creating the catalog-based arguments Object array, use
 * one of the makeMessageValues methods. If the message doesn't require any
 * substituted values, null may be passed.
 * <li>To obtain a list of error messages suitable for display on a different
 * page or in a different area of the data entry page:
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * UserErrors.allErrorMessages()</td>
 * </tr>
 * </table>
 * <br>
 * <li>To obtain and display a list of general error messages at the top of a
 * page, not including field error messages:
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * Enumeration e = UserErrors.generalErrorMessages(); while (
 * e.hasMoreElements() ) { out.println(e.toString()); }</td>
 * </tr>
 * </table>
 * <br>
 * <li>To obtain an error message suitable for display adjacent to a field:
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * UserErrors.get(fieldName)</td>
 * </tr>
 * </table>
 * <br>
 * <li>To determine if any field values were in error:
 * <table class="CodeExample">
 * <tr>
 * <td>
 *
 * <pre>
 * UserErrors.isEmpty()</td>
 * </tr>
 * </table>
 * <br>
 * <li>To prevent redundancy in messages, make sure that the message text does
 * not contain any of the other arguments to 'put'.
 * </ul>
 * //TODO! add doco for ModifierDecorator
 *************************************************************/
public class UserErrors
{
	// Assumption: the set of errors will be relatively small

	/*
	 * ------------------------------------------------------------ Note to
	 * maintainer:
	 *
	 * When a method that takes a name (or label) as an argument is added, it
	 * also needs to be modified in Modifier.
	 * ------------------------------------------------------------
	 */

	/**
	 * Information about a general error added to a UserErrors container.
	 **/
	public static final class GeneralError
	{
		private String message_;

		// attributes only used by catalog-backed messages
		// these are stored so that UI clients can access them:
		// some clients may only want these values, not the
		// message from the catalog. (for example, a VRU may
		// use the key to identify the appropriate audio file.)
		private String messageCatalog_;
		private String messageKey_;
		private Object[] messageValues_;

		public GeneralError(String message) {
			message_ = message;
			messageCatalog_ = null;
			messageKey_ = null;
			messageValues_ = null;
		}

		public GeneralError(String message, String messageCatalog, String messageKey, Object[] messageValues) {
			message_ = message;
			messageCatalog_ = messageCatalog;
			messageKey_ = messageKey;
			messageValues_ = messageValues;
		}

		/** The error message. */
		public String message() {
			return (messageKey_ == null) ? message_
					: UserText.get(messageCatalog_, messageKey_, messageValues_, message_);
		}

		/** The error message from a possibly different catalog. */
		public String message(Locale locale, CatalogNameModifier catalogNameModifier) {
			return (messageKey_ == null) ? message_
					: UserText.get(locale, catalogNameModifier.modifyName(messageCatalog_), messageKey_, messageValues_,
							message_);
		}

		/**
		 * For catalog-backed message, the name of the message catalog in which
		 * the message resides.
		 *
		 * @return null if this was not a catalog-backed message.
		 **/
		public String messageCatalog() {
			return messageCatalog_;
		}

		/**
		 * For catalog-backed message, the key of the message.
		 *
		 * @return null if this was not a catalog-backed message.
		 **/
		public String messageKey() {
			return messageKey_;
		}

		/**
		 * For catalog-backed messages, the values to be substituted into the
		 * message.
		 *
		 * @return null if this was not a catalog-backed message; an empty array
		 *         if no values were supplied; otherwise the array of values
		 *         supplied to UserErrors.put().
		 **/
		public Object[] messageValues() {
			return messageValues_;
		}

		/**
		 * The message.
		 **/
		@Override
		public String toString() {
			return message();
		}
	}

	/**
	 * Information about a single field error added to a UserErrors container.
	 **/
	public static final class FieldError
	{
		private String name_;
		private String label_;
		private Object value_;
		private String message_;

		// attributes only used by catalog-backed messages
		// these are stored so that UI clients can access them:
		// some clients may only want these values, not the
		// message from the catalog. (for example, a VRU may
		// use the key to identify the appropriate audio file.)
		private String messageCatalog_;
		private String messageKey_;
		private Object[] messageValues_;

		/** The programmatic name for the field. */
		public String name() {
			return name_;
		}

		/** The user's name for the field. */
		public String label() {
			return label_;
		}

		/** The value entered for the field; may be null. */
		public Object value() {
			return value_;
		}

		/** The error message. */
		public String message() {
			return (messageKey_ == null) ? message_
					: UserText.get(messageCatalog_, messageKey_, messageValues_, message_);
		}

		/** The error message from an alternate catalog. */
		public String message(Locale locale, CatalogNameModifier catalogNameModifier) {
			return (messageKey_ == null) ? message_
					: UserText.get(locale, catalogNameModifier.modifyName(messageCatalog_), messageKey_, messageValues_,
							message_);
		}

		/**
		 * For catalog-backed message, the name of the message catalog in which
		 * the message resides.
		 *
		 * @return null if this was not a catalog-backed message.
		 **/
		public String messageCatalog() {
			return messageCatalog_;
		}

		/**
		 * For catalog-backed messages, the key of the message.
		 *
		 * @return null if this was not a catalog-backed message.
		 **/
		public String messageKey() {
			return messageKey_;
		}

		/**
		 * For catalog-backed messages, the values to be substituted into the
		 * message.
		 *
		 * @return null if this was not a catalog-backed message; an empty array
		 *         if no values were supplied; otherwise the array of values
		 *         supplied to UserErrors.put().
		 **/
		public Object[] messageValues() {
			return messageValues_;
		}

		/* package */
		FieldError(Object value, // value entered for field
				String name, // programmatic name for field
				String label, // user's name for field
				String message // error message to be displayed
		) {
			value_ = value;
			name_ = name;
			label_ = label;
			message_ = message;
			messageCatalog_ = null;
			messageKey_ = null;
			messageValues_ = null;
		}

		/* package */
		FieldError(Object value, // value entered for field
				String name, // programmatic name for field
				String label, // user's name for field
				String message, // message (derived by UserErrors.Core)
				String messageCatalog, String messageKey, // key in message
				// catalog
				Object[] messageValues // substitution values
		) {
			value_ = value;
			name_ = name;
			label_ = label;
			messageCatalog_ = messageCatalog;
			message_ = message;
			messageKey_ = messageKey;
			messageValues_ = messageValues;
		}

		/**
		 * A user-readable message.
		 * <p>
		 * If no value was supplied, the message is in the form
		 * <p align="center">
		 * <i>label</i>:<i>message</i>
		 * </p>
		 * <p>
		 * If a value was supplied, the message is in the form
		 * <p align="center">
		 * <i>label</i> ('<i>value</i>'):<i>message</i>
		 * </p>
		 **/
		@Override
		public String toString() {
			if (value_ == null) {
				return label_ + ": " + message();
			} else {
				return label_ + " ('" + value_ + "'): " + message();
			}
		}

		public String toString(Locale locale, CatalogNameModifier catalogNameModifier) {
			if (value_ == null) {
				return label_ + ": " + message(locale, catalogNameModifier);
			} else {
				return label_ + " ('" + value_ + "'): " + message(locale, catalogNameModifier);
			}
		}
	}

	// There's probably a better way to do this...
	// I wanted people to be able to construct UserErrors objects and
	// then decorate them, and I didn't want to have to expose an
	// abstract class.
	// Anyway, this class does all the work.
	// One problem with this design is that everyone pays the price for
	// the ability to support decorators.
	// I think the only alternative is to make UserErrors abstract,
	// and re-implement the entire UserErrors interface in each leaf
	// class.
	private static class Core extends UserErrors
	{
		// field-specific error messages
		// entries are of type FieldError
		protected final Vector fieldErrors_;

		// general (non-field-specific) error messages
		// entries are of type GeneralError
		protected final Vector generalErrors_;

		public Core() {
			// very squirrelly
			super(false);

			fieldErrors_ = new Vector();
			generalErrors_ = new Vector();
		}

		private FieldError find_(Object name) {
			FieldError tmp = null;
			int i = fieldErrors_.size();
			while (--i >= 0) {
				tmp = (FieldError) fieldErrors_.elementAt(i);
				if (tmp.name().equals(name)) {
					return tmp;
				}
			}
			return null;
		}

		public Enumeration elements() {
			return allErrorMessages();
		}

		@Override
		public Enumeration allErrorMessages() {
			return new Enumeration() {
				private final Enumeration eg_ = generalErrorMessages();
				private final Enumeration ef_ = fieldErrorMessages();

				@Override
				public boolean hasMoreElements() {
					return ef_.hasMoreElements() || eg_.hasMoreElements();
				}

				@Override
				public Object nextElement() {
					if (eg_.hasMoreElements()) {
						return eg_.nextElement();
					} else {
						return ef_.nextElement();
					}
				}
			};
		}

		@Override
		public Enumeration allErrorMessages(final Locale locale, final CatalogNameModifier catalogNameModifier) {
			return new Enumeration() {
				private final Enumeration eg_ = generalErrorMessages(locale, catalogNameModifier);
				private final Enumeration ef_ = fieldErrorMessages(locale, catalogNameModifier);

				@Override
				public boolean hasMoreElements() {
					return ef_.hasMoreElements() || eg_.hasMoreElements();
				}

				@Override
				public Object nextElement() {
					if (eg_.hasMoreElements()) {
						return eg_.nextElement();
					} else {
						return ef_.nextElement();
					}
				}
			};
		}

		@Override
		public Enumeration fieldErrors() {
			return fieldErrors_.elements();
		}

		@Override
		public Enumeration fieldErrorMessages() {
			return new Enumeration() {
				private final Enumeration e_ = fieldErrors_.elements();

				@Override
				public boolean hasMoreElements() {
					return e_.hasMoreElements();
				}

				@Override
				public Object nextElement() {
					return e_.nextElement().toString();
				}
			};
		}

		@Override
		public Enumeration fieldErrorMessages(final Locale locale, final CatalogNameModifier catalogNameModifier) {
			return new Enumeration() {
				private final Enumeration e_ = fieldErrors_.elements();

				@Override
				public boolean hasMoreElements() {
					return e_.hasMoreElements();
				}

				@Override
				public Object nextElement() {
					return ((FieldError) (e_.nextElement())).toString(locale, catalogNameModifier);
				}
			};
		}

		@Override
		public boolean hasFieldErrors() {
			return !fieldErrors_.isEmpty();
		}

		@Override
		public FieldError fieldError(String name) {
			return find_(name);
		}

		@Override
		public String get(String name) {
			final FieldError error = find_(name);
			return error == null ? null : error.message();
		}

		@Override
		public Object get(Object name) {
			final FieldError error = find_(name);
			return error == null ? null : error.message();
		}

		@Override
		public boolean isEmpty() {
			return fieldErrors_.isEmpty() && generalErrors_.isEmpty();
		}

		public Enumeration messages() {
			return generalErrors();
		}

		@Override
		public Enumeration generalErrors() {
			return generalErrors_.elements();
		}

		@Override
		public Enumeration generalErrorMessages() {
			return new Enumeration() {
				private final Enumeration e_ = generalErrors_.elements();

				@Override
				public boolean hasMoreElements() {
					return e_.hasMoreElements();
				}

				@Override
				public Object nextElement() {
					return e_.nextElement().toString();
				}
			};
		}

		@Override
		public Enumeration generalErrorMessages(final Locale locale, final CatalogNameModifier catalogNameModifier) {
			return new Enumeration() {
				private final Enumeration e_ = generalErrors_.elements();

				@Override
				public boolean hasMoreElements() {
					return e_.hasMoreElements();
				}

				@Override
				public Object nextElement() {
					return ((GeneralError) (e_.nextElement())).message(locale, catalogNameModifier);
				}
			};
		}

		@Override
		public boolean hasGeneralErrors() {
			return !generalErrors_.isEmpty();
		}

		@Override
		public void put(String message) {
			generalErrors_.addElement(new GeneralError(message));
		}

		@Override
		public void put(Object value, String name, String label, String message) {
			if (name == null) {
				throw new IllegalArgumentException("UserErrors.put: name argument must not be null");
			}

			if (find_(name) == null) {
				fieldErrors_.addElement(new FieldError(value, name, label, message));
			}
		}

		@Override
		public void put(Object value, String name, String message) {
			if (name == null) {
				throw new IllegalArgumentException("UserErrors.put: name argument must not be null");
			}

			if (find_(name) == null) {
				fieldErrors_.addElement(new FieldError(value, name, name, message));
			}
		}

		@Override
		public void put(String messageCatalog, String messageKey, Object[] messageValues, String defaultMessage) {
			generalErrors_.addElement(new GeneralError(defaultMessage, messageCatalog, messageKey, messageValues));
		}

		private static final Object[] noMessageValues_ = {};

		@Override
		public void put(Object value, String name, String label, String messageCatalog, String messageKey,
				Object[] messageValues, String defaultMessage) {
			if (name == null) {
				throw new IllegalArgumentException("UserErrors.put: name argument must not be null");
			}

			if (messageValues == null) {
				messageValues = noMessageValues_;
			}

			if (find_(name) == null) {
				fieldErrors_.addElement(
						new FieldError(value, name, label, defaultMessage, messageCatalog, messageKey, messageValues));
			}
		}

		@Override
		public void put(Object value, String name, String messageCatalog, String messageKey, Object[] messageValues,
				String defaultMessage) {
			put(value, name, name, messageCatalog, messageKey, messageValues, defaultMessage);
		}

		@Override
		public int size() {
			return fieldErrors_.size() + generalErrors_.size();
		}

		@Override
		public void clear() {
			fieldErrors_.removeAllElements();
			generalErrors_.removeAllElements();
		}
	}

	private final UserErrors body_;

	/**
	 * Construct without a message mapper.
	 **/
	public UserErrors() {
		body_ = new Core();
	}

	/* package */
	UserErrors(boolean fromCore) {
		body_ = null;
	}

	/**
	 * Construct by a decorator.
	 **/
	protected UserErrors(UserErrors adapted) {
		body_ = adapted;
	}

	/**
	 * All field error messages, in the order they were added.
	 *
	 * @return an Enumeration with elements of type String.
	 **/
	public Enumeration fieldErrorMessages() {
		return body_.fieldErrorMessages();
	}

	public Enumeration fieldErrorMessages(Locale locale, CatalogNameModifier catalogNameModifier) {
		return body_.fieldErrorMessages(locale, catalogNameModifier);
	}

	/**
	 * Error messages suitable for separate display. General errors are reported
	 * first, followed by field-specific errors.
	 *
	 * @return A sequence of messages (of type String).
	 */
	public Enumeration allErrorMessages() {
		return body_.allErrorMessages();
	}

	public Enumeration allErrorMessages(Locale locale, CatalogNameModifier catalogNameModifier) {
		return body_.allErrorMessages(locale, catalogNameModifier);
	}

	/**
	 * Field information for errored fields in the order they were reported.
	 *
	 * @return an Enumeration of elements of type {@link FieldError
	 *         UserErrors.FieldError}.
	 */
	public Enumeration fieldErrors() {
		return body_.fieldErrors();
	}

	/** True if this object contains field errors. */
	public boolean hasFieldErrors() {
		return body_.hasFieldErrors();
	}

	/**
	 * The field error for a field.
	 *
	 * @return The error corresponding to field 'name', or null.
	 */
	public FieldError fieldError(String name) {
		return body_.fieldError(name);
	}

	/**
	 * Error message for a field, suitable for display adjacent to the field.
	 *
	 * @return The message corresponding to field 'name', or null.
	 */
	public String get(String name) {
		return body_.get(name);
	}

	/**
	 * Error message for a field, suitable for display adjacent to the field.
	 * This method exists to support the Dictionary interface.
	 *
	 * @return The message corresponding to field 'name', or null. (type String)
	 */
	public Object get(Object name) {
		return body_.get(name);
	}

	/**
	 * True if this object contains no field errors or general errors.
	 */
	public boolean isEmpty() {
		return body_.isEmpty();
	}

	/**
	 * All general error messages added using put(message), in the order they
	 * were added.
	 *
	 * @return an Enumeration of elements of type {@link GeneralError
	 *         UserErrors.GeneralError}.)
	 **/
	public Enumeration generalErrors() {
		return body_.generalErrors();
	}

	/**
	 * All general error messages added using put(message), in the order they
	 * were added.
	 *
	 * @return an Enumeration of messages of type String.
	 **/
	public Enumeration generalErrorMessages() {
		return body_.generalErrorMessages();
	}

	public Enumeration generalErrorMessages(Locale locale, CatalogNameModifier catalogNameModifier) {
		return body_.generalErrorMessages(locale, catalogNameModifier);
	}

	/**
	 * True if this object contains general error messages.
	 **/
	public boolean hasGeneralErrors() {
		return body_.hasGeneralErrors();
	}

	/**
	 * Add a general error message; typically a data entry error related to a
	 * page but not a field. Used, for example, for cross-field edit errors.
	 *
	 * @param message
	 *            error message to be displayed
	 */
	public void put(String message) {
		body_.put(message);
	}

	/**
	 * Add a field data entry error. No effect if this method was already called
	 * for a field with the same name.
	 *
	 * @param value
	 *            The value entered for the field; may be null.
	 * @param name
	 *            The programmatic name for the field.
	 * @param label
	 *            The user's name for the field.
	 * @param message
	 *            The error message to be displayed.
	 */
	public void put(Object value, String name, String label, String message) {
		body_.put(value, name, label, message);
	}

	/**
	 * Add a field data entry error, using the same value for both the field
	 * name and the label. No effect if a put method was already called for a
	 * field with the same name.
	 *
	 * @param value
	 *            The value entered for the field; may be null.
	 * @param name
	 *            The programmatic name for the field.
	 * @param message
	 *            The error message to be displayed.
	 */
	public void put(Object value, String name, String message) {
		body_.put(value, name, message);
	}

	private static final Object[] noMessageValues_ = {};

	/**
	 * Convenience method to obtain an empty message values argument for
	 * catalog-based put methods.
	 **/
	public static Object[] makeMessageValues() {
		return noMessageValues_;
	}

	/**
	 * Convenience method to obtain an empty message values argument for
	 * catalog-based put methods; equivalent to makeMessageValues().
	 **/
	public static Object[] noMessageValues() {
		return noMessageValues_;
	}

	/**
	 * Convenience method to create message values argument for catalog-based
	 * put methods.
	 **/
	public static Object[] makeMessageValues(Object arg1) {
		return new Object[] { arg1 };
	}

	/**
	 * Convenience method to create message values argument for catalog-based
	 * put methods.
	 **/
	public static Object[] makeMessageValues(Object arg1, Object arg2) {
		return new Object[] { arg1, arg2 };
	}

	/**
	 * Convenience method to create message values argument for catalog-based
	 * put methods.
	 **/
	public static Object[] makeMessageValues(Object arg1, Object arg2, Object arg3) {
		return new Object[] { arg1, arg2, arg3 };
	}

	/**
	 * Convenience method to create message values argument for catalog-based
	 * put methods.
	 **/
	public static Object[] makeMessageValues(Object arg1, Object arg2, Object arg3, Object arg4) {
		return new Object[] { arg1, arg2, arg3, arg4 };
	}

	/**
	 * Convenience method to create message values argument for catalog-based
	 * put methods.
	 **/
	public static Object[] makeMessageValues(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		return new Object[] { arg1, arg2, arg3, arg4, arg5 };
	}

	/**
	 * Convenience method to create message values argument for catalog-based
	 * put methods.
	 **/
	public static Object[] makeMessageValues(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,
			Object arg6) {
		return new Object[] { arg1, arg2, arg3, arg4, arg5, arg6 };
	}

	/**
	 * Add a message-catalog-backed general error message; typically a data
	 * entry error related to a page but not a field. Used, for example, for
	 * cross-field edit errors.
	 * <p>
	 * Note: to disambiguate this method from
	 * <code>put(Object value, String name, String message)</code> when
	 * messageValues is null, use
	 * <code>put(value, UserErrors.makeMessageValues(), message)</code>.
	 *
	 * @param messageKey
	 *            the key of the message to be displayed
	 * @param messageValues
	 *            optional values to be displayed on the message
	 * @param defaultMessage
	 *            default message text, used if no messages provider was
	 *            supplied at construction or if a catalog entry for messageKey
	 *            is not found
	 */
	public void put(String messageCatalog, String messageKey, Object[] messageValues, String defaultMessage) {
		body_.put(messageCatalog, messageKey, messageValues, defaultMessage);
	}

	/**
	 * Add a field data entry error. No effect if this method was already called
	 * for a field with the same name.
	 *
	 * @param value
	 *            The value entered for the field; may be null.
	 * @param name
	 *            The programmatic name for the field.
	 * @param label
	 *            The user's name for the field.
	 * @param messageKey
	 *            the key of the message to be displayed
	 * @param messageValues
	 *            optional values to be displayed on the message
	 * @param defaultMessage
	 *            default message text, used if no messages provider was
	 *            supplied at construction or if a catalog entry for messageKey
	 *            is not found
	 */
	public void put(Object value, String name, String label, String messageCatalog, String messageKey,
			Object[] messageValues, String defaultMessage) {
		body_.put(value, name, label, messageCatalog, messageKey, messageValues, defaultMessage);
	}

	/**
	 * Add a field data entry error, using the same value for both the field
	 * name and the label. No effect if a put method was already called for a
	 * field with the same name.
	 *
	 * @param value
	 *            The value entered for the field; may be null.
	 * @param name
	 *            The programmatic name for the field.
	 * @param messageKey
	 *            the key of the message to be displayed
	 * @param messageValues
	 *            optional values to be displayed on the message
	 * @param defaultMessage
	 *            default message text, used if no messages provider was
	 *            supplied at construction or if a catalog entry for messageKey
	 *            is not found
	 */
	public void put(Object value, String name, String messageCatalog, String messageKey, Object[] messageValues,
			String defaultMessage) {
		body_.put(value, name, messageCatalog, messageKey, messageValues, defaultMessage);
	}

	/** Total number of errors, both field and general. */
	public int size() {
		return body_.size();
	}

	/** Remove all entries, allowing the object to be reused. */
	public void clear() {
		body_.clear();
	}

	/**
	 * For debugging.
	 **/
	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer(255);
		if (hasGeneralErrors()) {
			buf.append("General error messages:\n");
			final Enumeration e = generalErrors();
			while (e.hasMoreElements()) {
				buf.append("\t");
				buf.append(e.nextElement().toString());
				buf.append("\n");
			}
		}
		if (hasFieldErrors()) {
			buf.append("Field error messages:\n");
			final Enumeration e = fieldErrors();
			while (e.hasMoreElements()) {
				buf.append("\t");
				buf.append(e.nextElement().toString());
				buf.append("\n");
			}
		}
		if (isEmpty()) {
			buf.append("no errors");
		}

		return buf.toString();
	}

	/**
	 * Field name modifier interface allowing clients to specify field name and
	 * label modification functions for {@link UserErrors UserErrors}
	 * interactions. For an example of how this interface is used, see
	 * {@link UserErrors#adaptWith(UserErrors.FieldNameModifier)
	 * UserErrors.adaptWith()}.
	 **/
	public interface FieldNameModifier
	{
		/**
		 * Change the field name provided by a UserErrors client.
		 **/
		String modifyName(String name);

		/**
		 * Change the field label provided by a UserErrors client.
		 **/
		String modifyLabel(String label);
	}

	/**
	 * Produce an interface to this UserErrors object that modifies names
	 * provided to it using a client-specified function.
	 * <p>
	 * For example, when a domain class may add errors, it will use attribute
	 * names, but UI field names may not be the same. The UI can use this method
	 * to obtain a version of the user errors object that changes attribute
	 * names to field names, pass that version of the object to domain classes
	 * during validation, and discard that version upon return. This allows the
	 * domain class to access the errors container using attribute names, and UI
	 * classes to access the container using field names.
	 * <p>
	 * Multiple threads may use multiple results from this method concurrently.
	 * <p>
	 * <b>Example:</b>
	 * <p>
	 * <table class="CodeExample">
	 * <tr>
	 * <td>
	 *
	 * <pre>
	 * UserErrors errors = new UserErrors(); if ( notNumeric(inputFieldOne) ) {
	 * errors.put(inputFieldOne, "input_field_one", "One", "must be numeric"); }
	 * domainObject.edit(errors.adaptWith( new UserErrors.FieldNameModifier() {
	 * public String modifyName(String name) { return "input_field_" + name; }
	 * public String modifyLabel(String label) { return
	 * Character.toUppercase(label.charAt(0)) + label.substring(1); } } );
	 *
	 * // ----------------------------------------------------- // Assume the
	 * domain class contains the following: //
	 * ----------------------------------------------------- public void
	 * edit(UserErrors errors) { errors.put(two, "two", "this is an error"); }
	 *
	 * // ----------------------------------------------------- // Upon return,
	 * the following assertions will be true: //
	 * -----------------------------------------------------
	 * assert(errors.get("two") == null); assert(errors.get("input_field_two")
	 * != null);</td>
	 * </tr>
	 * </table>
	 **/
	public UserErrors adaptWith(FieldNameModifier value) {
		return new ModifierDecorator(this, value);
	}

	private static class ModifierDecorator extends UserErrors
	{
		FieldNameModifier modifier_;

		public ModifierDecorator(UserErrors adapted, FieldNameModifier modifier) {
			super(adapted);
			modifier_ = modifier;
		}

		/**
		 * The field error for the field name obtained by the function
		 * modifyName(argumentName).
		 *
		 * @return The error corresponding to field 'name', or null.
		 */
		@Override
		public FieldError fieldError(String name) {
			return super.fieldError(modifier_.modifyName(name));
		}

		/**
		 * The error message for the field name obtained by the function
		 * modifyName(argumentName).
		 **/
		@Override
		public String get(String name) {
			return super.get(modifier_.modifyName(name));
		}

		/**
		 * The error message for the field name obtained by the function
		 * modifyName(argumentName).
		 **/
		@Override
		public Object get(Object name) {
			return super.get(modifier_.modifyName(name.toString()));
		}

		/**
		 * Add a field data entry error for the field name obtained by the
		 * function modifyName(argumentName), and the label obtained by the
		 * function modifyLabel(argumentLabel).
		 **/
		@Override
		public void put(Object value, String name, String label, String message) {
			super.put(value, modifier_.modifyName(name), modifier_.modifyLabel(label), message);
		}

		/**
		 * Add a field data entry error for the field name obtained by the
		 * function modifyName(argumentName), and the label name obtained by the
		 * function modifyLabel(argumentName).
		 **/
		@Override
		public void put(Object value, String name, String message) {
			super.put(value, modifier_.modifyName(name), modifier_.modifyLabel(name), message);
		}

		/**
		 * Add a field data entry error for the field name obtained by the
		 * function modifyName(argumentName), and the label obtained by the
		 * function modifyLabel(argumentLabel). No effect if this method was
		 * already called for a field with the same name.
		 *
		 * @param value
		 *            The value entered for the field; may be null.
		 * @param name
		 *            The programmatic name for the field.
		 * @param label
		 *            The user's name for the field.
		 * @param messageKey
		 *            the key of the message to be displayed
		 * @param messageValues
		 *            optional values to be displayed on the message
		 * @param defaultMessage
		 *            default message text, used if no messages provider was
		 *            supplied at construction or if a catalog entry for
		 *            messageKey is not found
		 */
		@Override
		public void put(Object value, String name, String label, String messageKey, Object[] messageValues,
				String defaultMessage) {
			super.put(value, modifier_.modifyName(name), modifier_.modifyLabel(label), messageKey, messageValues,
					defaultMessage);
		}

		/**
		 * Add a field data entry error, for the field name obtained by the
		 * function modifyName(argumentName), and the label obtained by the
		 * function modifyLabel(argumentName). No effect if a put method was
		 * already called for a field with the same name.
		 *
		 * @param value
		 *            The value entered for the field; may be null.
		 * @param name
		 *            The programmatic name for the field.
		 * @param messageKey
		 *            the key of the message to be displayed
		 * @param messageValues
		 *            optional values to be displayed on the message
		 * @param defaultMessage
		 *            default message text, used if no messages provider was
		 *            supplied at construction or if a catalog entry for
		 *            messageKey is not found
		 */
		public void put(Object value, String name, String messageKey, Object[] messageValues, String defaultMessage) {
			super.put(value, modifier_.modifyName(name), modifier_.modifyLabel(name), messageKey, messageValues,
					defaultMessage);
		}
	}

	/**
	 * Catalog name modifier interface used with
	 * {@link UserErrors#adaptWith(UserErrors.CatalogNameModifier)
	 * adaptWith(CatalogNameModifier)} allowing clients to transform message
	 * catalog names when obtaining messages added to the container.
	 **/
	public interface CatalogNameModifier
	{
		String modifyName(String name);
	}

	private static final CatalogNameModifier nullCatalogNameModifier = new CatalogNameModifier() {
		@Override
		public String modifyName(String name) {
			return name;
		}
	};

	/**
	 * Produce an interface to this UserErrors container that obtains error
	 * messages from a message catalog for a non-default locale.
	 *
	 * @see UserErrors.CatalogNameModifier
	 **/
	public UserErrors adaptWith(Locale locale) {
		return new CatalogDecorator(this, locale, nullCatalogNameModifier);
	}

	/**
	 * Produce an interface to this UserErrors container that obtains error
	 * messages from a message catalog whose name may be modified from the
	 * originally specified catalog name.
	 *
	 * @see UserErrors.CatalogNameModifier
	 **/
	public UserErrors adaptWith(CatalogNameModifier catalogNameModifier) {
		return new CatalogDecorator(this, Locale.getDefault(), catalogNameModifier);
	}

	private static class CatalogDecorator extends UserErrors
	{
		CatalogNameModifier catalogNameModifier_;
		Locale locale_;

		public CatalogDecorator(UserErrors adapted, Locale locale, CatalogNameModifier catalogNameModifier) {
			super(adapted);
			locale_ = locale;
			catalogNameModifier_ = catalogNameModifier;
		}

		/**
		 * The error message for a field.
		 **/
		@Override
		public String get(String name) {
			final FieldError error = super.fieldError(name);
			return error == null ? null : error.message(locale_, catalogNameModifier_);
		}

		/**
		 * The error message for the field name obtained by the function
		 * modifyName(argumentName).
		 **/
		@Override
		public Object get(Object name) {
			final FieldError error = super.fieldError(name.toString());
			return error == null ? null : error.message(locale_, catalogNameModifier_);
		}

		@Override
		public Enumeration fieldErrorMessages() {
			return fieldErrorMessages(locale_, catalogNameModifier_);
		}

		@Override
		public Enumeration generalErrorMessages() {
			return generalErrorMessages(locale_, catalogNameModifier_);
		}

		@Override
		public Enumeration allErrorMessages() {
			return allErrorMessages(locale_, catalogNameModifier_);
		}
	}
}
