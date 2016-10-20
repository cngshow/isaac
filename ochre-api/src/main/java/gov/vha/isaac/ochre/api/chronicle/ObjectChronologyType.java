/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.chronicle;

import java.security.InvalidParameterException;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author kec
 */
public enum ObjectChronologyType {
	CONCEPT("Concept"), SEMEME("Sememe"), UNKNOWN_NID("Unknown");
	
	private String niceName_;
	
	private ObjectChronologyType(String niceName)
	{
		niceName_ = niceName;
	}

	/**
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString()
	{
		return niceName_;
	}
	
	public static ObjectChronologyType parse(String nameOrEnumId, boolean exceptionOnParseFail)
	{
		if (nameOrEnumId == null)
		{
			return null;
		}
		String clean = nameOrEnumId.toLowerCase(Locale.ENGLISH).trim();
		if (StringUtils.isBlank(clean))
		{
			return null;
		}
		for (ObjectChronologyType ct : values())
		{
			if (ct.name().toLowerCase(Locale.ENGLISH).equals(clean) || ct.niceName_.toLowerCase(Locale.ENGLISH).equals(clean) 
					|| (ct.ordinal() + "").equals(clean))
			{
				return ct;
			}
		}
		if (exceptionOnParseFail)
		{
			throw new InvalidParameterException("Could not determine ObjectChronologyType from " + nameOrEnumId);
		}
		return UNKNOWN_NID;
	}
}
