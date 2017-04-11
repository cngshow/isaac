package gov.va.oia.terminology.converters.sharedUtils.umlsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import gov.va.oia.terminology.converters.sharedUtils.ComponentReference;
import gov.va.oia.terminology.converters.sharedUtils.IBDFCreationUtility;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.ValuePropertyPair;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;

public class ValuePropertyPairWithAttributes extends ValuePropertyPair
{
	protected HashMap<UUID, ArrayList<String>> stringAttributes = new HashMap<>();
	protected HashMap<UUID, ArrayList<UUID>> uuidAttributes = new HashMap<>();
	protected HashMap<UUID, ArrayList<String>> identifierAttributes = new HashMap<>();
	protected ArrayList<UUID> refsetMembership = new ArrayList<>();
	
	public ValuePropertyPairWithAttributes(String value, Property property)
	{
		super(value, property);
	}
	
	public void addStringAttribute(UUID type, String value)
	{
		ArrayList<String> values = stringAttributes.get(type);
		if (values == null)
		{
			values = new ArrayList<>();
			stringAttributes.put(type, values);
		}
		values.add(value);
	}
	
	public ArrayList<String> getStringAttribute(UUID type)
	{
		return stringAttributes.get(type);
	}
	
	public void addUUIDAttribute(UUID type, UUID value)
	{
		ArrayList<UUID> values = uuidAttributes.get(type);
		if (values == null)
		{
			values = new ArrayList<>();
			uuidAttributes.put(type, values);
		}
		values.add(value);
	}
	
	public void addRefsetMembership(UUID refsetConcept)
	{
		refsetMembership.add(refsetConcept);
	}
	
	public void addIdentifierAttribute(UUID type, String value)
	{
		ArrayList<String> values = identifierAttributes.get(type);
		if (values == null)
		{
			values = new ArrayList<>();
			identifierAttributes.put(type, values);
		}
		values.add(value);
	}
	public ArrayList<String> getIdentifierAttribute(UUID type)
	{
		return identifierAttributes.get(type);
	}

	public static void processAttributes(IBDFCreationUtility ibdfCreationUtility, List<? extends ValuePropertyPairWithAttributes> descriptionSource, 
		List<SememeChronology<DescriptionSememe<?>>> descriptions)
	{
		for (int i = 0; i < descriptionSource.size(); i++)
		{
			for (Entry<UUID, ArrayList<String>> attributes : descriptionSource.get(i).stringAttributes.entrySet())
			{
				for (String value : attributes.getValue())
				{
					ibdfCreationUtility.addStringAnnotation(ComponentReference.fromChronology(descriptions.get(i)), value, attributes.getKey(), State.ACTIVE);
				}
			}
			
			for (Entry<UUID, ArrayList<UUID>> attributes : descriptionSource.get(i).uuidAttributes.entrySet())
			{
				for (UUID value : attributes.getValue())
				{
					ibdfCreationUtility.addUUIDAnnotation(ComponentReference.fromChronology(descriptions.get(i)), value, attributes.getKey());
				}
			}
			
			for (UUID refsetConcept : descriptionSource.get(i).refsetMembership)
			{
				ibdfCreationUtility.addRefsetMembership(ComponentReference.fromChronology(descriptions.get(i)), refsetConcept, State.ACTIVE, null);
			}

			for (Entry<UUID, ArrayList<String>> identifierAttributes : descriptionSource.get(i).identifierAttributes.entrySet())
			{
				for (String value : identifierAttributes.getValue())
				{
					// TODO confirm parameters appropriate
					ibdfCreationUtility.addStaticStringAnnotation(ComponentReference.fromChronology(descriptions.get(i)), value, identifierAttributes.getKey(), State.ACTIVE);
				}
			}
		}
	}
}
