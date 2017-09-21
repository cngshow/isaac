/*
 * Copyright 2013 International Health Terminology Standards Development Organisation.
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
package gov.vha.isaac.ochre.api.util;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;
import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;

/**
 *
 * @author kec
 */
public class UuidFactory {
	private static final String MEMBER_SEED_STRING = "MEMBER_SEED_STRING";

	/**
	 * 
	 * @param namespace
	 * @param assemblage
	 * @param refComp
	 * @param le
	 * @param consumer an optional parameter that will get a callback with the string used to calculate the UUID - no impact on generation
	 * @return
	 */
	public static UUID getUuidForLogicGraphSememe(UUID namespace, UUID assemblage, UUID refComp, LogicalExpression le, BiConsumer<String, UUID> consumer) {
		byte[][] leBytes = le.getData(DataTarget.EXTERNAL);
		return UuidT5Generator.get(namespace, createUuidTextSeed(assemblage.toString(), refComp.toString(), toString(leBytes)), consumer);
	}
	
	private static String toString(byte[][] b)
	{
		StringBuilder temp = new StringBuilder();
		temp.append("[");
		for (byte[] bNested : b)
		{
			temp.append(Arrays.toString(bNested));
		}
		temp.append("]");
		return temp.toString();
	}

	/**
	 * 
	 * @param namespace
	 * @param assemblage
	 * @param refComp
	 * @param consumer an optional parameter that will get a callback with the string used to calculate the UUID - no impact on generation
	 * @return
	 */
	public static UUID getUuidForMemberSememe(UUID namespace, UUID assemblage, UUID refComp, BiConsumer<String, UUID> consumer) {

		return UuidT5Generator.get(namespace, createUuidTextSeed(assemblage.toString(), refComp.toString(), MEMBER_SEED_STRING), consumer);
	}

	/**
	 * 
	 * @param namespace
	 * @param assemblage
	 * @param refComp
	 * @param data
	 * @param consumer an optional parameter that will get a callback with the string used to calculate the UUID - no impact on generation
	 * @return
	 */
	public static UUID getUuidForDynamicSememe(UUID namespace, UUID assemblage, UUID refComp, DynamicSememeData[] data, BiConsumer<String, UUID> consumer) {
		StringBuilder temp = new StringBuilder();
		temp.append(assemblage.toString()); 
		temp.append(refComp.toString());
		temp.append(data == null ? "0" : data.length + "");
		if (data != null) {
			for (DynamicSememeData d : data)
			{
				if (d == null)
				{
					temp.append("null");
				}
				else
				{
					temp.append(d.getDynamicSememeDataType().getDisplayName());
					if (d.getDynamicSememeDataType() == DynamicSememeDataType.NID)
					{
						temp.append(Get.identifierService().getUuidPrimordialForNid(((DynamicSememeNid)d).getDataNid()));
					}
					else
					{
						temp.append(new String(ChecksumGenerator.calculateChecksum("SHA1", d.getData())));
					}
				}
			}
		}
		
		return UuidT5Generator.get(namespace, temp.toString(), consumer);
	}

	/**
	 * 
	 * @param namespace
	 * @param assemblage
	 * @param refComp
	 * @param component
	 * @param consumer an optional parameter that will get a callback with the string used to calculate the UUID - no impact on generation
	 * @return
	 */
	public static UUID getUuidForComponentNidSememe(UUID namespace, UUID assemblage, UUID refComp, UUID component, BiConsumer<String, UUID> consumer) {
		return UuidT5Generator.get(namespace, createUuidTextSeed(assemblage.toString(), refComp.toString(), component.toString()), consumer);
	}

	/**
	 * 
	 * @param namespace
	 * @param assemblage
	 * @param refComp
	 * @param value
	 * @param consumer an optional parameter that will get a callback with the string used to calculate the UUID - no impact on generation
	 * @return
	 */
	public static UUID getUuidForStringSememe(UUID namespace, UUID assemblage, UUID refComp, String value, BiConsumer<String, UUID> consumer) {
		return UuidT5Generator.get(namespace, createUuidTextSeed(assemblage.toString(), refComp.toString(), value), consumer);
	}

	/**
	 * 
	 * @param namespace
	 * @param assemblage
	 * @param concept
	 * @param caseSignificance
	 * @param descriptionType
	 * @param language
	 * @param descriptionText
	 * @param consumer an optional parameter that will get a callback with the string used to calculate the UUID - no impact on generation
	 * @return
	 */
	public static UUID getUuidForDescriptionSememe(UUID namespace, UUID assemblage, UUID concept, UUID caseSignificance,
			UUID descriptionType, UUID language, String descriptionText, BiConsumer<String, UUID> consumer) {
		return UuidT5Generator.get(namespace, createUuidTextSeed(assemblage.toString(), concept.toString(), caseSignificance.toString(),
				descriptionType.toString(), language.toString(), descriptionText), consumer);
	}

	/**
	 * Create a new Type5 UUID using the provided name as the seed in the
	 * configured namespace.
	 * 
	 * Throws a runtime exception if the namespace has not been configured.
	 */
	private static String createUuidTextSeed(String... values) {
		StringBuilder uuidKey = new StringBuilder();
		for (String s : values) {
			if (s != null) {
				uuidKey.append(s);
				uuidKey.append("|");
			}
		}
		if (uuidKey.length() > 1) {
			uuidKey.setLength(uuidKey.length() - 1);
		} else {
			throw new RuntimeException("No string provided!");
		}
		return uuidKey.toString();
	}
}
