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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.mapping.data;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;

/**
 * {@link MappingDAO}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class MappingDAO
{
	protected static final Logger LOG = LoggerFactory.getLogger(MappingDAO.class);
	
	protected static DynamicSememe<?> readCurrentRefex(UUID refexUUID, StampCoordinate stampCoord) throws RuntimeException
	{
		SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(refexUUID));
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sc).getLatestVersion(DynamicSememe.class, 
				stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		return latest.get().value();
	}
	
	@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
	protected static void setConceptStatus(UUID conceptUUID, State state, StampCoordinate stampCoord, EditCoordinate editCoord) throws RuntimeException
	{
		ConceptChronology<? extends ConceptVersion<?>> cc = Get.conceptService().getConcept(conceptUUID);
		
		Optional<LatestVersion<ConceptVersion>> cv =  ((ConceptChronology) cc).getLatestVersion(ConceptVersion.class, stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		if (cv.isPresent() && !cv.get().contradictions().isPresent() && cv.get().value().getState() == state)
		{
			LOG.warn("Tried set the status to the value it already has.  Doing nothing");
		}
		else
		{
			cc.createMutableVersion(state, editCoord);
			Get.commitService().addUncommitted(cc);
			Get.commitService().commit("Changing map concept state");
		}

	}
	
	@SuppressWarnings("deprecation")
	protected static void setSememeStatus(UUID refexUUID, State state, StampCoordinate stampCoord, EditCoordinate editCoord) throws RuntimeException
	{
		DynamicSememe<?> ds = readCurrentRefex(refexUUID, stampCoord);
		
		if (ds.getState() == state)
		{
			LOG.warn("Tried set the status to the value it already has.  Doing nothing");
		}
		else
		{
			@SuppressWarnings("unchecked")
			MutableDynamicSememe<?> mds = ((SememeChronology<DynamicSememe<?>>)ds.getChronology()).createMutableVersion(MutableDynamicSememe.class, state,
					editCoord);
			mds.setData(ds.getData());
			
			Get.commitService().addUncommitted(ds.getChronology());
			Get.commitService().commit("Changing sememe state");
		}
	}
}
