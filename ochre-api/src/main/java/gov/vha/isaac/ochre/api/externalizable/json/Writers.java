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
package gov.vha.isaac.ochre.api.externalizable.json;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.cedarsoftware.util.io.JsonWriter;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.NodeSemantic;

/**
 * {@link Writers}
 * 
 * An experimental class that doesn't work yet due to upstream bugs
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class Writers
{
	/**
	 * This representation of a concept is only intended to aid in debugging, it is not rich enough to represent all of the data.
	 */
	public static class ConceptChronologyJsonWriter implements JsonWriter.JsonClassWriterEx
	{
		/**
		 * @see com.cedarsoftware.util.io.JsonWriter.JsonClassWriterEx#write(java.lang.Object, boolean, java.io.Writer, java.util.Map)
		 */
		@Override
		public void write(Object obj, boolean showType, Writer output, Map<String, Object> args) throws IOException
		{
			ConceptChronology<?> cc = (ConceptChronology<?>) obj;
			JsonWriter mainWriter = Support.getWriter(args);
			
			output.write("\"nid\":\"");
			output.write(cc.getNid() + "");
			output.write("\",");
			mainWriter.newLine();
			output.write("\"conceptSequence\":\"");
			output.write(cc.getConceptSequence() + "");
			output.write("\",");
			mainWriter.newLine();
			output.write("\"uuidList\":[");
			mainWriter.tabIn();
			StringBuilder temp = new StringBuilder();
			for (UUID uuid : cc.getUuidList())
			{
				temp.append("\"" + uuid);
				temp.append("\", ");
			}
			if (temp.length() > 2)
			{
				temp.setLength(temp.length() - 2);
			}
			output.write(temp.toString());
			mainWriter.tabOut();
			output.write("]");
		}
	}
	
	/**
	 * This representation of a concept is only intended to aid in debugging, it is not rich enough to represent all of the data.
	 */
	public static class SememeChronologyJsonWriter implements JsonWriter.JsonClassWriterEx
	{
		/**
		 * @see com.cedarsoftware.util.io.JsonWriter.JsonClassWriterEx#write(java.lang.Object, boolean, java.io.Writer, java.util.Map)
		 */
		@Override
		public void write(Object obj, boolean showType, Writer output, Map<String, Object> args) throws IOException
		{
			SememeChronology<SememeVersion<?>> sc = (SememeChronology<SememeVersion<?>>) obj;
			JsonWriter mainWriter = Support.getWriter(args);
			
			output.write("\"sememeType\":\"");
			output.write(sc.getSememeType().name());
			output.write("\",");
			mainWriter.newLine();
			output.write("\"nid\":\"");
			output.write(sc.getNid() + "");
			output.write("\",");
			mainWriter.newLine();
			output.write("\"sememeSequence\":\"");
			output.write(sc.getSememeSequence() + "");
			output.write("\",");
			mainWriter.newLine();
			output.write("\"uuidList\":[");
			mainWriter.tabIn();
			StringBuilder temp = new StringBuilder();
			for (UUID uuid : sc.getUuidList())
			{
				temp.append("\"" + uuid);
				temp.append("\", ");
			}
			if (temp.length() > 2)
			{
				temp.setLength(temp.length() - 2);
			}
			output.write(temp.toString());
			mainWriter.tabOut();
			output.write("],");
			mainWriter.newLine();
			output.write("\"assemblageSequence\":\"");
			output.write(sc.getAssemblageSequence() + "");
			output.write("\",");
			mainWriter.newLine();
			output.write("\"referencedComponentNid\":\"");
			output.write(sc.getReferencedComponentNid() + "");
			output.write("\",");
			
			List<SememeVersion<?>> versions =  (List<SememeVersion<?>>) sc.getVersionList();
			mainWriter.newLine();
			output.write("\"versions\":[");
			mainWriter.tabIn();
			
			boolean first = true;
			
			for (SememeVersion<?> sv : versions)
			{
				if (first)
				{
					first = false;
					output.write("{");
				}
				else
				{
					output.write(",");
					mainWriter.newLine();
					output.write("{");
				}
				
				mainWriter.tabIn();
				if (showType)
				{
					output.write("\"@type\":\"");
					output.write(sv.getClass().getName());
					output.write("\",");
					mainWriter.newLine();
				}
				
				if (sv instanceof DescriptionSememe<?>)
				{
					DescriptionSememe<?> ds = (DescriptionSememe<?>)sv;
					output.write("\"caseSignificanceSequence\":\"");
					output.write(ds.getCaseSignificanceConceptSequence() + "");
					output.write("\",");
					mainWriter.newLine();
					output.write("\"languageConceptSequence\":\"");
					output.write(ds.getLanguageConceptSequence() + "");
					output.write("\",");
					mainWriter.newLine();
					output.write("\"descriptionTypeConceptSequence\":\"");
					output.write(ds.getDescriptionTypeConceptSequence() + "");
					output.write("\",");
					mainWriter.newLine();
					output.write("\"text\":\"");
					output.write(ds.getText() + "");
					output.write("\"");
				}
				else if (sv instanceof ComponentNidSememe<?>)
				{
					ComponentNidSememe<?> cns = (ComponentNidSememe<?>)sv;
					output.write("\"componentNid\":\"");
					output.write(cns.getComponentNid() + "");
					output.write("\"");
				}
				else if (sv instanceof DynamicSememe<?>)
				{
					DynamicSememe<?> ds = (DynamicSememe<?>)sv;
					output.write("\"data\":\"");
					output.write(ds.dataToString());
					output.write("\"");
				}
				else if (sv instanceof LogicGraphSememe<?>)
				{
					//A hack for the moment, to just write out the parent of the concept from the logic graph, 
					//as that is often what is wanted for debugging.
					//TODO represent the entire logic graph in JSON?
					LogicGraphSememe<?> lgs = (LogicGraphSememe<?>)sv;
					LogicalExpression le = lgs.getLogicalExpression();
					LogicNode root = le.getRoot();
					for (LogicNode necessaryOrSufficient : root.getChildren())
					{
						for (LogicNode connector : necessaryOrSufficient.getChildren())
						{
							for (LogicNode target : connector.getChildren())
							{
								if (target.getNodeSemantic() == NodeSemantic.CONCEPT)
								{
									//Hack ALERT!
									//This should look like this: Concept[1] ISAAC metadata (ISAAC) <14> 
									String conceptString = target.toString();
									if (conceptString.contains("<") && conceptString.contains(">"))
									{
										output.write("\"parentConceptSequence\":\"");
										output.write(conceptString.substring(conceptString.lastIndexOf('<') + 1, conceptString.lastIndexOf('>')));
										output.write("\"");
									}
									else
									{
										output.write("\"logicGraph\":\"NOT_YET_REPRESENTED\"");
									}
								}
								else
								{
									output.write("\"logicGraph\":\"NOT_YET_REPRESENTED\"");
								}
							}
						}
					}
					
				}
				else if (sv instanceof LongSememe<?>)
				{
					LongSememe<?> ls = (LongSememe<?>)sv;
					output.write("\"long\":\"");
					output.write(ls.getLongValue() + "");
					output.write("\"");
				}
				else if (sv instanceof StringSememe<?>)
				{
					StringSememe<?> ss = (StringSememe<?>)sv;
					output.write("\"string\":\"");
					output.write(ss.getString());
					output.write("\"");
				}
				else
				{
					//Sememe Version - no extra fields
				}
				
				mainWriter.tabOut();
				output.write("}");
			}

			mainWriter.tabOut();
			output.write("]");
		}
	}
}
