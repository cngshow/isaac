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

package gov.vha.isaac.ochre.access.maint.messaging.hl7;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A typesafe enumeration based upon JSR 201 for HL7 encodings.
 */
public class Encoding implements Serializable
{
	/**
	 * The ordinal of this enumeration constant (its position
	 * in the enum declaration, where the initial constant is assigned
	 * an ordinal of zero).
	 */
	public final transient int ordinal;

	/**
	 * The name of this enum constant, as declared in the enum declaration.
	 * Most programmers should use the {@link #toString} method rather than
	 * accessing this field.
	 */
	public final String name;

	/**
	 * Sole constructor.  Programmers should never invoke this constructor.
	 * It is for use by code emitted by the compiler in response to
	 * enum class declarations.
	 *
	 * @param n - The name of this enum constant, which is the identifier
	 *               used to declare it.
	 * @param o - The ordinal of this enumeration constant (its position
	 *         in the enum declaration, where the initial constant is assigned
	 *         an ordinal of zero).
	 */
	protected Encoding(String n, int o)
	{
		name = n;
		ordinal = o;
	}

	// Use the HAPI encodings for the names. Unfortunately, HAPI
	// uses VB for ER7.
	public static Encoding er7 = new Encoding("VB", 0);
	public static Encoding xml = new Encoding("XML", 1);

	/**
	 * An immutable list containing the values comprising this enum class
	 * in the order they're declared.  This field may be used to iterate
	 * over.
	 */
	public static final List VALUES =
			Collections.unmodifiableList(Arrays.asList(new Encoding[] {
					er7,
					xml,
			}));

	/**
	 * Returns an immutable list of all the enum constants in this
	 * enum constant's enum class.
	 *
	 * @return an immutable list of all of the enum constants in this
	 *         enum constant's enum class.
	 */
	public List family()
	{
		return VALUES;
	}

	/**
	 * Returns true if the specified object is equal to this
	 * enum constant.
	 *
	 * @param o the object to be compared for equality with this object.
	 * @return  true if the specified object is equal to this
	 *          enum constant.
	 */
	@Override
	public final boolean equals(Object o)
	{
		return (this == o);
	}

	/**
	 * Returns a hash code for this enum constant.
	 *
	 * @return a hash code for this enum constant.
	 */
	@Override
	public final int hashCode()
	{
		return ordinal;
	}

	/**
	 * Returns the name of this enum constant, as contained in the
	 * declaration.
	 *
	 * @return the name of this enum constant
	 */
	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Compares this enum with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 *
	 * Enum constants are only comparable to other enum constants of the
	 * same enum class.  The natural order implemented by this
	 * method is the order in which the constants are declared.
	 */
	public final int compareTo(Encoding o)
	{
		return (ordinal - o.ordinal);
	}

	/**
	 * Throws CloneNotSupportedException.  This guarantees that enums
	 * are never cloned, which is necessary to preserve their "singleton"
	 * status.
	 *
	 * @return (never returns)
	 */
	@Override
	protected final Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException("enums cannot be cloned.");
	}

	/**
	 * This method ensures proper deserialization of enum constants.
	 *
	 * @return the canonical instance of this deserialized enum const.
	 */
	protected final Object readResolve()
	{
		return valueOf(name);
	}

	/**
	 * Static factory to return the enum constant pertaining to the
	 * given string name.  The string must match exactly an identifier
	 * used to declare an enum constant in this type.
	 *
	 * @throws IllegalArgumentException if this enum class has no constant
	 *         with the specified name.
	 */
	public static Encoding valueOf(String name)
	{
		for (int i=0; i<VALUES.size(); i++)
		{
			final Encoding value = (Encoding)VALUES.get(i);
			if (value.name.equals(name))
			{
				return value;
			}
		}
		throw new IllegalArgumentException(
				"No constant with the specified name: "
						+ name
				);
	}
}
