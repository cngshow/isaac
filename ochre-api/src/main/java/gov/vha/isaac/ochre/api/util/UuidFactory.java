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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;

/**
 *
 * @author kec
 */
public class UuidFactory {
	private static final String MEMBER_SEED_STRING = "MEMBER_SEED_STRING";
	private static final String EMPTY_CONTENT_DYNAMIC_SEED_STRING = "EMPTY_CONTENT_DYNAMIC_SEED_STRING";

	/**
	 * Gets the uuid of a component from the specified alternate identifier of
	 * the same component.
	 *
	 * @param authority
	 *            the uuid representing the authoring associated with the
	 *            alternate id
	 * @param altId
	 *            a string representation of the alternate id
	 * @return the uuid of the specified component
	 */
	public static UUID getUuidFromAlternateId(UUID authority, String altId) {
		if (authority.equals(TermAux.SNOMED_IDENTIFIER.getUuids()[0])
				|| authority.equals(TermAux.SNOMED_IDENTIFIER.getUuids()[1])) {
			return UuidT3Generator.fromSNOMED(altId);
		}
		return UuidT5Generator.get(authority, altId);
	}

	public static UUID getUuidForLogicGraphSememe(UUID authority, UUID assemblage, UUID refComp, Object[] parameters) {
		String logicGraphStr = ((LogicalExpression) parameters[0]).toString().replaceAll("<[0-9]+>", "");
		
		if (logicGraphStr.contains("No desc for:")) {
			logicGraphStr = logicGraphStr.substring(0, logicGraphStr.indexOf("No desc for:"));
		}
		
		return UuidT5Generator.get(authority,
				createUuidTextSeed(assemblage.toString(), refComp.toString(), logicGraphStr));
	}

	public static UUID getUuidForMemberSememe(UUID authority, UUID assemblage, UUID refComp) {
		return UuidT5Generator.get(authority,
				createUuidTextSeed(assemblage.toString(), refComp.toString(), MEMBER_SEED_STRING));
	}

	public static UUID getUuidForDynamicSememe(UUID authority, UUID assemblage, UUID refComp, Object[] dynamicData) {
		if (dynamicData != null && dynamicData.length > 0
				&& ((AtomicReference<DynamicSememeData[]>) dynamicData[0]).get() != null) {
			DynamicSememeData[] dataArray = ((AtomicReference<DynamicSememeData[]>) dynamicData[0]).get();

			StringBuilder dataArrayStr = new StringBuilder();

			for (int i = 0; i < dataArray.length; i++) {
				if (dataArray[i] != null) {
					dataArrayStr.append(dataArray[i].dataToString() + "|");
				} else {
					dataArrayStr.append("NULL|");
				}
			}

			return UuidT5Generator.get(authority,
					createUuidTextSeed(assemblage.toString(), refComp.toString(), dataArrayStr.toString()));

		} else {
			return UuidT5Generator.get(authority,
					createUuidTextSeed(assemblage.toString(), refComp.toString(), EMPTY_CONTENT_DYNAMIC_SEED_STRING));
		}
	}

	public static UUID getUuidForComponentNidSememe(UUID authority, UUID assemblage, UUID refComp, UUID component) {
		return UuidT5Generator.get(authority,
				createUuidTextSeed(assemblage.toString(), refComp.toString(), component.toString()));
	}

	public static UUID getUuidForDescriptionSememe(UUID authority, int assemblageSeq, UUID concept, int caseSigNid,
			UUID descType, UUID language, String descriptionText) {
		int assemblageNid = Get.identifierService().getConceptNid(assemblageSeq);

		return UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
				createUuidTextSeedForDescription(Get.identifierService().getUuidPrimordialForNid(assemblageNid).get(),
						concept, Get.identifierService().getUuidPrimordialForNid(caseSigNid).get(), descType, language,
						descriptionText));
	}

	public static UUID getUuidForStringSememe(UUID authority, UUID assemblage, UUID refComp, Object[] parameters) {
		return UuidT5Generator.get(authority,
				createUuidTextSeed(assemblage.toString(), refComp.toString(), (String) parameters[0]));
	}

	public static UUID getUuidForDescriptionSememe(UUID authority, UUID assemblage, UUID concept, Object[] parameters) {
		int caseSigNid = Get.identifierService().getConceptNid((Integer) parameters[0]);
		int descTypeNid = Get.identifierService().getConceptNid((Integer) parameters[1]);
		int languageNid = Get.identifierService().getConceptNid((Integer) parameters[2]);

		return UuidT5Generator.get(authority,
				createUuidTextSeedForDescription(assemblage, concept,
						Get.identifierService().getUuidPrimordialForNid(caseSigNid).get(),
						Get.identifierService().getUuidPrimordialForNid(descTypeNid).get(),
						Get.identifierService().getUuidPrimordialForNid(languageNid).get(), (String) parameters[3]));
	}

	private static String createUuidTextSeedForDescription(UUID assemblage, UUID concept, UUID caseSignificance,
			UUID descriptionType, UUID language, String descriptionText) {
		return createUuidTextSeed(assemblage.toString(), concept.toString(), caseSignificance.toString(),
				descriptionType.toString(), language.toString(), descriptionText);
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
