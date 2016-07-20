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
package gov.vha.isaac.ochre.impl.utility;

import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;

/**
 * 
 * {@link SimpleDisplayConcept}
 *
 * A very simple concept container, useful for things like ComboBoxes, or lists
 * where we want to display workbench concepts, and still have a link to the underlying
 * concept (via the nid)
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class SimpleDisplayConcept implements Comparable<SimpleDisplayConcept>
{
	protected String description_;
	private int nid_;
	private Supplier<Boolean> customLogic_;
	private boolean uncommitted_ = false;
	
	/**
	 * 
	 * @param description
	 * @param nid
	 * @param customLogic - typically used to allow a changeListener to ignore a change.  
	 * See {@link #shouldIgnoreChange()}
	 */
	public SimpleDisplayConcept(String description, int nid, Supplier<Boolean> customLogic)
	{
		description_ = description;
		nid_ = nid;
		customLogic_ = customLogic;
	}
	
	public SimpleDisplayConcept(ConceptSnapshot c)
	{
		this(c.getChronology(), null);
	}
	
	public SimpleDisplayConcept(ConceptChronology<? extends ConceptVersion<?>> c, Function<ConceptChronology<? extends ConceptVersion<?>>, String> descriptionReader)
	{
		Function<ConceptChronology<? extends ConceptVersion<?>>, String> dr = (descriptionReader == null ? (conceptVersion) -> 
			{return (conceptVersion == null ? "" : Frills.getDescription(conceptVersion.getConceptSequence()).get());} : descriptionReader);
		description_ = dr.apply(c);
		nid_ = c == null ? 0 : c.getNid();
		customLogic_ = null;
	}
	
	/**
	 * @param conceptId nid or sequence
	 * @param descriptionReader - optional
	 */
	public SimpleDisplayConcept(Integer conceptId, Function<ConceptChronology<? extends ConceptVersion<?>>, String> descriptionReader)
	{
		this((conceptId == null ? null : Get.conceptService().getConcept(conceptId)), descriptionReader);
	}
	
	public SimpleDisplayConcept(Integer conceptId)
	{
		this(conceptId, null);
	}
	
	public SimpleDisplayConcept(String description)
	{
		this(description, 0);
	}

	public SimpleDisplayConcept(String description, int nid)
	{
		this(description, nid, null);
	}

	public String getDescription()
	{
		return description_;
	}

	public int getNid()
	{
		return nid_;
	}

	public void setUncommitted(boolean val) {
		uncommitted_ = val;
	}
	
	public boolean isUncommitted() {
		return uncommitted_;
	}
	
	public void setNid(int nid)
	{
		nid_ = nid;
	}
	
	/**
	 * Return back whatever customLogic supplier was passed in
	 */
	public Supplier<Boolean> customLogic()
	{
		return customLogic_;
	}
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description_ == null) ? 0 : description_.hashCode());
		result = prime * result + nid_;
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (obj instanceof SimpleDisplayConcept)
		{
			SimpleDisplayConcept other = (SimpleDisplayConcept) obj;
			return nid_ == other.nid_ && StringUtils.equals(description_, other.description_);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return description_;
	}
	
	@Override
	public SimpleDisplayConcept clone()
	{
		return new SimpleDisplayConcept(this.description_, this.nid_, customLogic_);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SimpleDisplayConcept o)
	{
		return new SimpleDisplayConceptComparator().compare(this, o);
	}
}