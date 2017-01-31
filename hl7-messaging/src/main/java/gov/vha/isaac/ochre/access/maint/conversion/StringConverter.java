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
package gov.vha.isaac.ochre.access.maint.conversion;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import gov.vha.isaac.ochre.access.maint.util.UserErrors;

/**
 * Convert a String to a Java datatype value.
 * <p>
 * This class assumes that the Java datatype supports one of the following
 * patterns for creation of values from Strings:
 * <ul>
 * <li>Conversion using a static 'valueOf(String)' method.
 * <li>Conversion using a constructor that takes a single String argument.
 * <li>For an Interface, conversion using an 'obtainFromKeyString' method
 * provided by an object stored in an interface field named 'factory'.
 * </ul>
 * <p>
 * The first two patterns are provided by most value types in the JDK. The third
 * is a Dataman pattern.
 * <p>
 * If the target datatype doesn't support any known pattern,
 * <p>
 * Data conversion errors are assumed to result in an exception issued by the
 * converting method. If no UserErrors container is provided, a
 * {@link StringConverter.FormatException FormatException} is thrown. If a
 * UserError container is provided, an error message is added to the container.
 * Messages are defined by type in
 * gov.va.med.term.access.maint.conversion.messages, or in a resource bundle
 * named messages.properties in the same package as the target datatype with an
 * entry keyed by the fully qualified datatype name.
 * <p>
 * Messages use the Locale of the current
 * gov.va.med.term.access.util.ThreadLocale.
 *
 * <p>
 * <b>Limitations</b>
 * <p>
 * <ul>
 * <li>Arrays are not currently supported.
 * <li>Client-defined conversion methods are not currently supported.
 * <li>Native types are not currently supported.
 * </ul>
 *
 * @.maintenance
 *               <dl>
 *
 *               <dt>(Unfinished) Adding Array support</dt>
 *               <dd>This enhancement will be necessary to support multi-valued
 *               attributes where attribute values must be specified externally.
 *               Uncomment, review, revise, and test the fromString(String[] ...
 *               ) method.</dd>
 *
 *               <dt>(Unfinished) Adding custom conversion method support</dt>
 *               <dd>This enhancement would make this class more flexible by
 *               allowing it to support externally-defined datatypes that do not
 *               support conversion from String.
 *               <ol>
 *               <li>Make the ConversionMethod class public. (Do not move it
 *               into its own file.)
 *               <li>Change the ConversionMethod.conversionMethodFor method to
 *               look for a factory first. Use ImplementorRegistry to find the
 *               factory.
 *               </ol>
 *               </dd>
 *
 *               <dt>(Unfinished) Adding native type support</dt>
 *               <dd>Native type conversion requires custom code for each native
 *               type. Fortunately the set of native types is small and closed.
 *
 *               <dt>Supporting new datatypes</dt>
 *               <dd>No changes to this class should be necessary. Simply ensure
 *               that new datatypes support one of the supported conversion
 *               method patterns.</dd>
 *
 *               <dt>Supporting a new conversion method pattern</dt>
 *               <dd>
 *               <ol>
 *               <li>Identify the pattern. Example: a pattern that consists of
 *               creating an instance of the datatype, followed by calling a
 *               method on the datatype with the signature "void
 *               populate(String)".
 *               <li>Name the pattern. The name should be suitable for a class
 *               name component; it will be appended to the prefix
 *               "ConversionMethod_". Example: "PopulateNew".
 *               <li>Determine whether the pattern applies to interfaces or
 *               classes. If a pattern is restricted to a single class, see
 *               "Adding custom conversion method support", above.
 *               <li>Determine pattern precedence. If more than one pattern is
 *               supported, which should be used? Current precedence is:
 *               <dl>
 *               <dt>Classes</dt>
 *               <dd>
 *               <ol>
 *               <li>valueOf(String)
 *               <li>constructor(String)
 *               </ol>
 *               </dd>
 *               <dt>Interfaces</dt>
 *               <dd>
 *               <ol>
 *               <li>factory.obtainfromKeyString(String)
 *               </ol>
 *               </dd>
 *               </dl>
 *               <p>
 *               This and the next step are closely related, and probably need
 *               to be done together.
 *               <li>Determine how this pattern can be detected
 *               programmatically. The PopulateNew pattern is present if the
 *               following are all true:
 *               <ol>
 *               <li>The datatype is a class.
 *               <li>The datatype provides a public default constructor.
 *               <li>The datatype provides a public method named "populate" that
 *               takes a single argument of type String.
 *               </ol>
 *               <li>Create a new subclass of ConversionMethod that implements
 *               the pattern. Place the subclass in this file after the existing
 *               subclasses.
 *               <li>Enhance ConversionMethod.conversionMethodFor to detect the
 *               pattern and construct the new conversion method.
 *               </ol>
 *               </dd>
 *               </dl>
 **/
public class StringConverter
{
	/**
	 * An exception issued when {@link StringConverter StringConverter} cannot
	 * find a way to convert a String to an instance of a datatype.
	 * <p>
	 * {@link StringConverter StringConverter} expects each target datatype to
	 * support one of a set of known conversion patterns. It throws this
	 * exception when none of the known conversion patterns is detected.
	 **/
	public static class ConversionNotSupportedException extends RuntimeException
	{
		public ConversionNotSupportedException(String message) {
			super(message);
		}
	}

	/**
	 * An exception issued when the conversion method used by
	 * {@link StringConverter StringConverter} cannot convert an input value and
	 * no UserErrors container was provided.
	 **/
	public static class FormatException extends Exception
	{
		public FormatException(String message) {
			super(message);
		}
	}

	/**
	 * A method to convert strings into objects of a datatype. Concrete
	 * subclasses implement specific conversion methods.
	 **/
	private abstract static class ConversionMethod
	{
		private static final Class[] parameterTypes_ = { String.class };

		// target datatype and name
		private Class datatype_;
		private String datatypeName_;

		/**
		 * Generate a conversion method for a datatype.
		 *
		 * @throws ConversionNotSupportedException
		 *             if a conversion method cannot be generated because no
		 *             methods conforming to a known conversion pattern can be
		 *             found.
		 **/
		/* file */ static ConversionMethod conversionMethodFor(Class datatype) {
			ConversionMethod result = null;
			// !TODO: Add user-defined conversion methods.
			// Obtain factory using ImplementorRegistry?
			if (datatype.isInterface()) {
				result = initFromInterface(datatype);
			} else {
				result = initFromClass(datatype);
			}

			if (result == null) {
				throw new ConversionNotSupportedException(
						"No obvious way to obtain a " + datatype.getName() + " from a String");
			}

			return result;
		}

		/**
		 * Gets the wrapper class for the given primitive class.
		 *
		 * @param primitiveClass
		 *            The primitive class.
		 * @return The wrapper class.
		 */
		private static Class getWrapperClass(Class primitiveClass) {
			if (Boolean.TYPE.equals(primitiveClass)) {
				return Boolean.class;
			} else if (Byte.TYPE.equals(primitiveClass)) {
				return Byte.class;
			} else if (Character.TYPE.equals(primitiveClass)) {
				return Character.class;
			} else if (Short.TYPE.equals(primitiveClass)) {
				return Short.class;
			} else if (Integer.TYPE.equals(primitiveClass)) {
				return Integer.class;
			} else if (Long.TYPE.equals(primitiveClass)) {
				return Long.class;
			} else if (Float.TYPE.equals(primitiveClass)) {
				return Float.class;
			} else if (Double.TYPE.equals(primitiveClass)) {
				return Double.class;
			} else {
				throw new IllegalArgumentException(
						"Class must represent a primitive type: " + primitiveClass.getName());
			}
		}

		/**
		 * Generate a conversion method that uses a valueOf method or a String
		 * constructor.
		 **/
		private static ConversionMethod initFromClass(Class datatype) {
			try {
				// Use the wrapper class for primitive types.
				if (datatype.isPrimitive()) {
					datatype = getWrapperClass(datatype);
				}
				final Method method = datatype.getMethod("valueOf", parameterTypes_);
				if (Modifier.isStatic(method.getModifiers())) {
					return new ConversionMethod_StaticMethod(datatype, method);
				}
			} catch (Exception x) {
			}

			try {
				final Constructor ctor = datatype.getConstructor(parameterTypes_);
				return new ConversionMethod_Constructor(datatype, ctor);
			} catch (Exception x) {
			}

			return null;
		}

		/**
		 * Generate a conversion method that uses factory.obtainFromKeyString.
		 **/
		private static ConversionMethod initFromInterface(Class datatype) {
			// Look for the Dataman factory pattern
			// <interface>.factory.obtainFromKeyString(String)
			try {
				final Field fFactory = datatype.getField("factory");
				final Class cFactory = fFactory.getType();
				final Method method = cFactory.getMethod("obtainFromKeyString", parameterTypes_);
				final Object factory = fFactory.get(null);
				return new ConversionMethod_Factory(datatype, method, factory);
			} catch (Exception x) {
			}

			return null;
		}

		/**
		 * Create a conversion method for a datatype.
		 **/
		protected ConversionMethod(Class datatype) {
			datatype_ = datatype;
			datatypeName_ = datatype.getName();
		}

		/**
		 * Generate a message for a conversion error.
		 * <p>
		 * This method first attempts to obtain the message from an entry in the
		 * resource bundle gov.va.med.term.access.maint.conversion.messages with
		 * a key corresponding to the fully-qualified class name of the
		 * datatype. If that is not found, the method attempts to obtain the
		 * message from an entry in a resource bundle with the name "messages"
		 * in the same package as the datatype with a key corresponding to the
		 * fully-qualified class name of the datatype. If that is not found, the
		 * default message defined in
		 * gov.va.med.term.access.maint.conversion.messages is used.
		 **/
		protected String message() {
			String result = null;

			final Locale locale = Locale.getDefault(); // ThreadLocale.get();

			try {
				final ResourceBundle bundle = ResourceBundle
						.getBundle("gov.va.med.term.access.maint.conversion.messages", locale);
				result = bundle.getString(datatypeName_);
			} catch (MissingResourceException xx) {
			}

			if (result == null) {
				try {
					final ResourceBundle bundle = ResourceBundle.getBundle(packagePrefix(datatypeName_) + ".messages",
							locale);
					result = bundle.getString(datatypeName_);
				} catch (MissingResourceException x) {
				}
			}

			if (result == null) {
				try {
					final ResourceBundle bundle = ResourceBundle
							.getBundle("gov.va.med.term.access.maint.conversion.messages", locale);
					result = bundle.getString("default");
				} catch (MissingResourceException xx) {
				}
			}

			return result == null ? "Invalid value." : result;
		}

		/**
		 * The package portion of a class name.
		 **/
		private static String packagePrefix(String className) {
			final int p = className.lastIndexOf('.');
			if (p >= 0) {
				className = className.substring(0, p);
			} else {
				className = null;
			}

			return className;
		}

		/**
		 * Convert an input string to an internal object, populating an error
		 * message container on error.
		 **/
		/* file */
		Object fromString(String inputValue, String fieldName, UserErrors errors) {
			try {
				return convert(inputValue);
			} catch (RuntimeException x) {
				throw x;
			} catch (InvocationTargetException x) {
				errors.put(inputValue, fieldName, message());
				return null;
			} catch (Exception x) {
				throw new RuntimeException(x);
			}
		}

		/**
		 * Convert an input string to an internal object, throwing a
		 * {@link StringConverter.FormatException FormatException} on error.
		 **/
		/* file */
		Object fromString(String inputValue) throws FormatException {
			try {
				return convert(inputValue);
			} catch (RuntimeException x) {
				throw x;
			} catch (InvocationTargetException x) {
				if (x.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException) x.getTargetException();
				}

				throw new FormatException(message());
			} catch (Exception x) {
				throw new RuntimeException(x);
			}
		}

		/**
		 * The actual conversion function.
		 **/
		protected abstract Object convert(String value) throws Exception;
	}

	/**
	 * A conversion method that uses a String constructor.
	 **/
	private static class ConversionMethod_Constructor extends ConversionMethod
	{
		private Constructor constructor_;

		public ConversionMethod_Constructor(Class datatype, Constructor constructor) {
			super(datatype);
			constructor_ = constructor;
		}

		@Override
		protected Object convert(String inputValue) throws Exception {
			final Object[] arguments = { inputValue };
			return constructor_.newInstance(arguments);
		}
	}

	/**
	 * A conversion method that uses a static valueOf method.
	 **/
	private static class ConversionMethod_StaticMethod extends ConversionMethod
	{
		private Method converter_;

		public ConversionMethod_StaticMethod(Class datatype, Method converter) {
			super(datatype);
			converter_ = converter;
		}

		@Override
		protected Object convert(String inputValue) throws Exception {
			final Object[] arguments = { inputValue };
			return converter_.invoke(null, arguments);
		}
	}

	/**
	 * A conversion method that uses the Dataman factory pattern.
	 **/
	private static class ConversionMethod_Factory extends ConversionMethod
	{
		private Method converter_;
		private Object factory_;

		public ConversionMethod_Factory(Class datatype, Method converter, Object factory) {
			super(datatype);
			converter_ = converter;
			factory_ = factory;
		}

		@Override
		protected Object convert(String inputValue) throws Exception {
			final Object[] arguments = { inputValue };
			return converter_.invoke(factory_, arguments);
		}
	}

	private static Hashtable cache_ = new Hashtable();

	private static synchronized ConversionMethod conversionMethodFor(Class datatype) {
		ConversionMethod result = (ConversionMethod) cache_.get(datatype);
		if (result == null) {
			result = ConversionMethod.conversionMethodFor(datatype);
			cache_.put(datatype, result);
		}

		return result;
	}

	/**
	 * Convert a string to a datatype value.
	 * <p>
	 * This method has conversion behavior identical to the core
	 * {@link #fromString(java.lang.String, java.lang.Class) fromString} method.
	 * However, if the value cannot be converted, an error message is added to
	 * the errors container. The key of the error message is the field name. The
	 * text of the error message is obtained from a properties file.
	 * <p>
	 * Error messages are assumed to reside in properties files. The key to the
	 * properties file is the fully qualified class name of the datatype (e.g.
	 * 'java.lang.Integer').
	 * <p>
	 * The message issued is determined by the following process:
	 * <ul>
	 * <li>Look in messages file
	 * "/gov/va/med/term/access/maint/conversion/messages.properties". By
	 * default, this properties file contains messages for Java types such as
	 * java.lang.Integer. If the default messages file contains a matching
	 * entry, the value of the entry is used as the message.
	 * <li>Look in messages file "<i>datatype-package</i>.messages.properties".
	 * For example, for datatype 'a.b.C', the messages file would be assumed to
	 * be '/a/b/messages.properties'. If a type-specific messages file is found
	 * and it contains a matching entry, the value of the entry is used as the
	 * message.
	 * <li>Use the generic message specified in
	 * "/gov/va/med/term/access/maint/conversion/messages.properties" with key
	 * "default", which by default is "Invalid value".
	 * </ul>
	 * <p>
	 * Messages may be localized, as documented in java.util.ResourceBundle.
	 *
	 * @param from
	 *            String value to be converted.
	 * @param datatype
	 *            Class of result type.
	 * @param fieldName
	 *            Name of input field providing 'from' value; this will be the
	 *            key of any error message created in 'errors'.
	 * @param errors
	 *            Error messages container.
	 *
	 * @return Converted value, or null if 'from' is null or empty or if
	 *         conversion failed.
	 *
	 * @throws StringConverter.ConversionNotSupportedException
	 *             if the argument datatype does not support any known patterns
	 *             for conversion from a String.
	 **/
	public static Object fromString(String from, Class datatype, String fieldName, UserErrors errors) {
		if (from == null || from.length() == 0) {
			return null;
		}

		final ConversionMethod conversionMethod = conversionMethodFor(datatype);
		return conversionMethod.fromString(from, fieldName, errors);
	}

	/**
	 * Convert a string to a datatype value.
	 * <p>
	 * Every datatype is assumed to have either a public static conversion
	 * method with the signature 'valueOf(String value): <i>Type</i>', or a
	 * public constructor that takes a String. This is true for every basic Java
	 * type except Date. Applications should use the Date types specified in the
	 * VA basic datatypes package (gov.va.basic).
	 * <p>
	 * The datatype conversion method is assumed to throw an exception if the
	 * string value cannot be converted. This method converts that exception to
	 * a {@link StringConverter.FormatException FormatException}, which is a
	 * RuntimeException.
	 * <p>
	 * If the string to be converted is null or empty, a null value is returned.
	 * In most cases, clients should trim the input string before calling this
	 * method.
	 *
	 * @param from
	 *            String value to be converted.
	 * @param datatype
	 *            Class of result type.
	 *
	 * @return Converted value, or null if 'from' is null or empty or if
	 *         conversion failed.
	 *
	 * @throws StringConverter.ConversionNotSupportedException
	 *             if the argument datatype does not support any known patterns
	 *             for conversion from a String.
	 *
	 * @throws FormatException
	 *             if the string cannot be converted to an object of the
	 *             requested datatype.
	 **/
	public static Object fromString(String from, Class datatype) throws FormatException {
		if (from == null || from.length() == 0) {
			return null;
		}

		final ConversionMethod conversionMethod = conversionMethodFor(datatype);
		return conversionMethod.fromString(from);
	}

	/**
	 * Whether a string can be converted into an object of a given type.
	 *
	 * @param datatype
	 *            Class of desired result type.
	 *
	 * @return true if StringConverter can convert a string into an object of
	 *         type 'datatype'.
	 **/
	public static boolean canConvert(Class datatype) {
		ConversionMethod conversionMethod = null;
		try {
			conversionMethod = conversionMethodFor(datatype);
		} catch (Exception x) {
		}

		return conversionMethod != null;
	}

	/**
	
	 **/

	// ! Old code, unconverted and untested
	// ! /**
	// ! Convert a string array to a datatype value.
	// ! <p>
	// ! If the target type is an array type, each element of the array
	// ! is converted using StringConverter.
	// ! <p>
	// ! Otherwise, a predefined transformation is applied based on the
	// ! target datatype and the size of the input array.
	// ! The following transformations are currently supported:
	// ! <table>
	// ! <tr>
	// ! <th>Target datatype</th>
	// ! <th>Input elements</th>
	// ! <th>Transformation</th>
	// ! </tr>
	// ! <tr>
	// ! <td>gov.va.med.term.access.maint.types.Date</td>
	// ! <td>2</td>
	// ! <td>A date/time value:
	// ! <ol>
	// ! <li>Date
	// ! <li>Time of day (24-hour clock)
	// ! </ol>
	// ! </td>
	// ! </tr>
	// ! <tr>
	// ! <td>gov.va.med.term.access.maint.types.Date</td>
	// ! <td>3</td>
	// ! <td>A date/time value:
	// ! <ol>
	// ! <li>Date
	// ! <li>Time of day (12-hour clock)
	// ! <li>"am" or "pm"
	// ! </ol>
	// ! </td>
	// ! </tr>
	// ! </table>
	// !
	// ! @param from
	// ! Array of string values to be converted.
	// ! @param datatype
	// ! Class of result type.
	// ! @param fieldName
	// ! Name of input field providing 'from' value; this will be
	// ! the key of any error message created in 'errors'.
	// ! @param errors
	// ! Error messages container.
	// !
	// ! @return
	// ! Converted value, or null if 'from' is null or empty or if
	// ! conversion failed.
	// ! **/
	// ! public static
	// ! Object
	// ! fromString(
	// ! String[] from,
	// ! Class datatype,
	// ! String fieldName,
	// ! UserErrors errors
	// ! )
	// ! {
	// ! if ( from == null || from.length() == 0 )
	// ! {
	// ! return null;
	// ! }
	// !
	// ! Object result;
	// !
	// ! if ( datatype.isArray() )
	// ! {
	// ! result = Array.newInstance(
	// ! datatype.getComponentType(), from.length);
	// !
	// ! int lim = from.length;
	// ! int i = -1;
	// ! while ( ++i < lim )
	// ! {
	// ! result[i] = fromString(
	// ! from[i], datatype.getComponentType(),
	// ! fieldName, errors);
	// ! }
	// !
	// ! return result;
	// ! }
	// !
	// ! ConversionMethod conversionMethod = converterFor(datatype);
	// ! return converter.fromString(from, fieldName, errors);
	// ! }
}
