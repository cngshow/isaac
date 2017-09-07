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
package gov.vha.isaac.ochre.ibdf.provider.diff;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableStringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;
import gov.vha.isaac.ochre.model.relationship.RelationshipAdaptorChronicleKeyImpl;
import gov.vha.isaac.ochre.model.relationship.RelationshipVersionAdaptorImpl;
import gov.vha.isaac.ochre.model.sememe.version.ComponentNidSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.DescriptionSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LongSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.SememeVersionImpl;
import gov.vha.isaac.ochre.model.sememe.version.StringSememeImpl;

/**
 * Utility methods in support of BinaryDataDifferProvider used to see if two
 * components are the same and to create new versions when necessary.
 * 
 * {@link BinaryDataDifferProvider}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class BinaryDataDifferProviderUtility {
	static long newImportDate;
	static boolean componentChangeFound = false;
	boolean diffOnTimestamp;
	boolean diffOnAuthor;
	boolean diffOnModule;
	boolean diffOnPath;
	private SememeBuilderService<?> sememeBuilderService_;

	public BinaryDataDifferProviderUtility() {
		sememeBuilderService_ = Get.sememeBuilderService();
	}

	public OchreExternalizable diff(ObjectChronology<?> oldChron, ObjectChronology<?> newChron, int stampSeq,
			OchreExternalizableObjectType type) {
		List<StampedVersion> oldVersions = null;
		List<StampedVersion> newVersions = (List<StampedVersion>) newChron.getVersionList();

		if (oldChron == null) {
			return createNewChronology(newChron, type, stampSeq);
		}

		boolean newVersionAdded = false;
		oldVersions = (List<StampedVersion>) oldChron.getVersionList();

		for (StampedVersion nv : newVersions) {
			boolean equivalenceFound = false;

			for (StampedVersion ov : oldVersions) {
				if (isEquivalent(ov, nv, type)) {
					equivalenceFound = true;
					break;
				}
			}

			if (!equivalenceFound) {
				// versionsToAdd.add(ov);
				newVersionAdded = addNewActiveVersion(oldChron, nv, type, stampSeq);
			}
		}

		if (!newVersionAdded) {
			return null;
		}

		return oldChron;
	}

	private boolean isEquivalent(StampedVersion ov, StampedVersion nv, OchreExternalizableObjectType type) {
		if ((diffOnTimestamp && ov.getTime() != nv.getTime())
				|| (diffOnAuthor && ov.getAuthorSequence() != nv.getAuthorSequence())
				|| (diffOnModule && ov.getModuleSequence() != nv.getModuleSequence())
				|| (diffOnPath && ov.getPathSequence() != nv.getPathSequence())) {
			return false;
		} else if (type == OchreExternalizableObjectType.CONCEPT) {
			// No other value to analyze equivalence for a concept, so return
			// true
			return true;
		} else {
			// Analyze Sememe
			SememeVersion<?> os = (SememeVersion<?>) ov;
			SememeVersion<?> ns = (SememeVersion<?>) nv;

			if ((os.getAssemblageSequence() != ns.getAssemblageSequence())
					|| os.getReferencedComponentNid() != ns.getReferencedComponentNid()) {
				return false;
			}

			switch (os.getChronology().getSememeType()) {
			case COMPONENT_NID:
				return ((ComponentNidSememe<?>) os).getComponentNid() == ((ComponentNidSememe<?>) ns).getComponentNid();
			case DESCRIPTION:
				return ((DescriptionSememe<?>) os).getText().equals(((DescriptionSememe<?>) ns).getText());
			case LONG:
				return ((LongSememe<?>) os).getLongValue() == ((LongSememe<?>) ns).getLongValue();
			case STRING:
				return ((StringSememe<?>) os).getString().equals(((StringSememe<?>) ns).getString());
			case DYNAMIC:
				return ((DynamicSememe<?>) os).dataToString().equals(((DynamicSememe<?>) ns).dataToString());
			case LOGIC_GRAPH:
				return Arrays.deepEquals(((LogicGraphSememe<?>) os).getGraphData(),
						((LogicGraphSememe<?>) ns).getGraphData());
			case MEMBER:
				return true;
			case RELATIONSHIP_ADAPTOR:
			case UNKNOWN:
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	private SememeVersion<?> populateData(SememeVersion<?> newVer, SememeVersion<?> originalVersion,
			int inactiveStampSeq) {
		switch (newVer.getChronology().getSememeType()) {
		case MEMBER:
			return newVer;
		case COMPONENT_NID:
			((MutableComponentNidSememe<?>) newVer)
					.setComponentNid(((ComponentNidSememe<?>) originalVersion).getComponentNid());
			return newVer;
		case DESCRIPTION:
			((MutableDescriptionSememe<?>) newVer).setText(((DescriptionSememe<?>) originalVersion).getText());
			((MutableDescriptionSememe<?>) newVer).setDescriptionTypeConceptSequence(
					((DescriptionSememe<?>) originalVersion).getDescriptionTypeConceptSequence());
			((MutableDescriptionSememe<?>) newVer).setCaseSignificanceConceptSequence(
					((DescriptionSememe<?>) originalVersion).getCaseSignificanceConceptSequence());
			((MutableDescriptionSememe<?>) newVer)
					.setLanguageConceptSequence(((DescriptionSememe<?>) originalVersion).getLanguageConceptSequence());
			return newVer;
		case DYNAMIC:
			if (((DynamicSememe<?>) originalVersion).getData().length > 0) {
				((MutableDynamicSememe<?>) newVer).setData(((DynamicSememe<?>) originalVersion).getData());
			}
			return newVer;
		case LONG:
			((MutableLongSememe<?>) newVer).setLongValue(((LongSememe<?>) originalVersion).getLongValue());
			return newVer;
		case STRING:
			((MutableStringSememe<?>) newVer).setString(((StringSememe<?>) originalVersion).getString());
			return newVer;
		case RELATIONSHIP_ADAPTOR:
			RelationshipVersionAdaptorImpl origRelVer = (RelationshipVersionAdaptorImpl) originalVersion;
			RelationshipAdaptorChronicleKeyImpl key = new RelationshipAdaptorChronicleKeyImpl(
					origRelVer.getOriginSequence(), origRelVer.getDestinationSequence(), origRelVer.getTypeSequence(),
					origRelVer.getGroup(), origRelVer.getPremiseType(), origRelVer.getNodeSequence());

			return new RelationshipVersionAdaptorImpl(key, inactiveStampSeq);
		case LOGIC_GRAPH:
			((MutableLogicGraphSememe<?>) newVer).setGraphData(((LogicGraphSememe<?>) originalVersion).getGraphData());
			return newVer;
		case UNKNOWN:
			throw new UnsupportedOperationException();
		}

		return null;
	}

	private Class<?> getSememeClass(SememeVersion<?> sememe) {
		{
			switch (sememe.getChronology().getSememeType()) {
			case COMPONENT_NID:
				return ComponentNidSememeImpl.class;
			case DESCRIPTION:
				return DescriptionSememeImpl.class;
			case DYNAMIC:
				return DynamicSememeImpl.class;
			case LONG:
				return LongSememeImpl.class;
			case MEMBER:
				return SememeVersionImpl.class;
			case STRING:
				return StringSememeImpl.class;
			case RELATIONSHIP_ADAPTOR:
				return RelationshipVersionAdaptorImpl.class;
			case LOGIC_GRAPH:
				return LogicGraphSememeImpl.class;
			case UNKNOWN:
			default:
				throw new UnsupportedOperationException();
			}

		}
	}

	private OchreExternalizable createNewChronology(ObjectChronology<?> newChron, OchreExternalizableObjectType type,
			int stampSeq) {
		try {
			if (type == OchreExternalizableObjectType.CONCEPT) {
				return newChron;
			} else if (type == OchreExternalizableObjectType.SEMEME) {
				List<ObjectChronology<? extends StampedVersion>> builtObjects = new ArrayList<>();
				SememeChronology<?> sememe = null;
				for (StampedVersion version : newChron.getVersionList()) {
					SememeBuilder<?> builder = getBuilder((SememeVersion<?>) version);
					sememe = (SememeChronology<?>) builder.build(stampSeq, builtObjects);
				}

				return sememe;
			} else {
				throw new Exception("Unsupported OchreExternalizableObjectType: " + type);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private SememeBuilder<?> getBuilder(SememeVersion<?> version) {
		SememeBuilder<?> builder = null;

		switch (version.getChronology().getSememeType()) {
		case COMPONENT_NID:
			ComponentNidSememe<?> compNidSememe = (ComponentNidSememe<?>) version;
			builder = sememeBuilderService_.getComponentSememeBuilder(compNidSememe.getComponentNid(),
					compNidSememe.getReferencedComponentNid(), compNidSememe.getAssemblageSequence());
			break;
		case DESCRIPTION:
			DescriptionSememe<?> descSememe = (DescriptionSememe<?>) version;
			builder = sememeBuilderService_.getDescriptionSememeBuilder(descSememe.getCaseSignificanceConceptSequence(),
					descSememe.getLanguageConceptSequence(), descSememe.getDescriptionTypeConceptSequence(),
					descSememe.getText(), descSememe.getReferencedComponentNid());
			break;
		case DYNAMIC:
			DynamicSememe<?> dynSememe = (DynamicSememe<?>) version;
			builder = sememeBuilderService_.getDynamicSememeBuilder(dynSememe.getReferencedComponentNid(),
					dynSememe.getAssemblageSequence(), dynSememe.getData());
			break;
		case LONG:
			LongSememe<?> longSememe = (LongSememe<?>) version;
			builder = sememeBuilderService_.getLongSememeBuilder(longSememe.getLongValue(),
					longSememe.getReferencedComponentNid(), longSememe.getAssemblageSequence());
			break;
		case MEMBER:
			builder = sememeBuilderService_.getMembershipSememeBuilder(version.getReferencedComponentNid(),
					version.getAssemblageSequence());
			break;
		case STRING:
			StringSememe<?> stringSememe = (StringSememe<?>) version;
			builder = sememeBuilderService_.getStringSememeBuilder(stringSememe.getString(),
					stringSememe.getReferencedComponentNid(), stringSememe.getAssemblageSequence());
			break;
		case LOGIC_GRAPH:
			LogicGraphSememe<?> logicGraphSememe = (LogicGraphSememe<?>) version;
			builder = sememeBuilderService_.getLogicalExpressionSememeBuilder(logicGraphSememe.getLogicalExpression(),
					logicGraphSememe.getReferencedComponentNid(), logicGraphSememe.getAssemblageSequence());
			break;
		case UNKNOWN:
		case RELATIONSHIP_ADAPTOR: // Dan doesn't believe rel adapaters are ever
								   // created / written to ibdf
		default:
			throw new UnsupportedOperationException();
		}

		builder.setPrimordialUuid(version.getPrimordialUuid());

		return builder;
	}

	public OchreExternalizable addNewInactiveVersion(OchreExternalizable oldChron, OchreExternalizableObjectType type,
			int inactiveStampSeq) {
		LatestVersion<StampedVersion> latestVersion = ((ObjectChronology<StampedVersion>) oldChron)
				.getLatestVersion(StampedVersion.class, StampCoordinates.getDevelopmentLatestActiveOnly()).get();

		if (type == OchreExternalizableObjectType.CONCEPT) {

			((ConceptVersion<?>) latestVersion.value()).getChronology().createMutableVersion(inactiveStampSeq);
		} else if (type == OchreExternalizableObjectType.SEMEME) {
			SememeVersion originalVersion = (SememeVersion) latestVersion.value();

			SememeVersion createdVersion = originalVersion.getChronology()
					.createMutableVersion(getSememeClass((SememeVersion) latestVersion.value()), inactiveStampSeq);

			createdVersion = populateData(createdVersion, originalVersion, inactiveStampSeq);
		}

		return oldChron;
	}

	private boolean addNewActiveVersion(ObjectChronology<?> oldChron, StampedVersion newVersion,
			OchreExternalizableObjectType type, int activeStampSeq) {
		try {
			if (type == OchreExternalizableObjectType.CONCEPT) {
				((ConceptChronology<?>) oldChron)
						.createMutableVersion(((ConceptVersion<?>) newVersion).getStampSequence());
				return true;
			} else if (type == OchreExternalizableObjectType.SEMEME) {
				SememeVersion createdVersion = ((SememeChronology) oldChron).createMutableVersion(newVersion.getClass(),
						((SememeVersion<?>) newVersion).getStampSequence());

				createdVersion = populateData(createdVersion, (SememeVersion<?>) newVersion, activeStampSeq);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public void setNewImportDate(String importDate) {
		// Must be in format of 2005-10-06
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			newImportDate = sdf.parse(importDate).getTime();
		} catch (ParseException e) {
			Date d = new Date();
			newImportDate = d.getTime();
		}
	}

	public long getNewImportDate() {
		return newImportDate;
	}

	public void setDiffOptions(boolean diffOnTimestamp, boolean diffOnAuthor, boolean diffOnModule,
			boolean diffOnPath) {
		this.diffOnTimestamp = diffOnTimestamp;
		this.diffOnAuthor = diffOnAuthor;
		this.diffOnModule = diffOnModule;
		this.diffOnPath = diffOnPath;
	}
}
