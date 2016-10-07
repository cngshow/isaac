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
package gov.vha.isaac.ochre.workflow.model.contents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import gov.vha.isaac.ochre.api.commit.Stamp;
import gov.vha.isaac.ochre.api.externalizable.ByteArrayDataBuffer;
import gov.vha.isaac.ochre.stamp.provider.StampSerializer;
import gov.vha.isaac.ochre.workflow.provider.BPMNInfo;

/**
 * The metadata defining a given process (or workflow instance). This doesn't
 * include its Detail which is available via {@link ProcessHistory}
 * 
 * {@link AbstractStorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class ProcessDetail extends AbstractStorableWorkflowContents
{
	/**
	 * The exhaustive list of possible process statuses.
	 */
	public enum ProcessStatus
	{
		/** Process is being defined and has yet to be launched. */
		DEFINED,
		/** Process has been launched */
		LAUNCHED,
		/** A previously launched or defined process that has been canceled */
		CANCELED,
		/** A previously launched process that is completed */
		CONCLUDED
	}

	/**
	 * The exhaustive list of possible ways an instantiated process may be ended
	 *
	 * 
	 */
	public enum EndWorkflowType
	{
		/** Process is stopped without reaching a completed state */
		CANCELED,
		/** Process has been finished by reaching a completed state */
		CONCLUDED
	};

	/** The workflow definition key for which the Process Detail is relevant. */
	private UUID definitionId;

	/**
	 * A map of all component nids modified within the workflow process to the time of initial modification.
	 * Therefore, if a component has been modified multiple times within a
	 * single process, only the first of those times are required on the map.
	 */
	private Map<Integer, Stamp> componentToIntitialEditMap = new HashMap<>();

	/** The user who originally defined (created) the workflow process. */
	private UUID creatorId;

	/** The time the workflow process was created. */
	private long timeCreated;

	/** The time the workflow process was launched. */
	private long timeLaunched = -1L;

	/**
	 * The time the workflow process was finished (either via canceled Or
	 * Concluded).
	 */
	private long timeCanceledOrConcluded = -1L;

	/** The workflow process's status. */
	private ProcessStatus status;

	/** The workflow process's name. */
	private String name;

	/** The workflow process's description. */
	private String description;

	/** The workflow process's current "owner". */
	private UUID ownerId = BPMNInfo.UNOWNED_PROCESS;

	/**
     * Definition uuid most significant bits
     */
	private long definitionIdMsb;
    
	/**
     * Definition uuid least significant bits
     */
    private long definitionIdLsb;

    /**
     * Creator uuid most significant bits
     */
	private long creatorIdMsb;
    
	/**
     * Creator uuid least significant bits
     */
    private long creatorIdLsb;

	/**
     * Owner uuid most significant bits
     */
	private long ownerIdMsb;
    
	/**
     * Owner uuid least significant bits
     */
    private long ownerIdLsb;
	
    private StampSerializer stampSerializer = new StampSerializer();

	/**
	 * Constructor for a new process based on specified entry fields.
	 * 
	 * @param definitionId
	 * @param creatorId
	 * @param timeCreated
	 * @param status
	 * @param name
	 * @param description
	 */
	public ProcessDetail(UUID definitionId, UUID creatorId, long timeCreated, ProcessStatus status, String name, String description)
	{
		this.definitionId = definitionId;
		this.creatorId = creatorId;
		this.timeCreated = timeCreated;
		this.status = status;
		this.name = name;
		this.description = description;
		this.ownerId = creatorId;

		this.definitionIdMsb = definitionId.getMostSignificantBits();
        this.definitionIdLsb = definitionId.getLeastSignificantBits();
        this.creatorIdMsb = creatorId.getMostSignificantBits();
        this.creatorIdLsb = creatorId.getLeastSignificantBits();
        this.ownerIdMsb = ownerId.getMostSignificantBits();
        this.ownerIdLsb = ownerId.getLeastSignificantBits();
	}

	/**
	 * Constructor for a new process based on serialized content.
	 *
	 * @param data
	 * The data to deserialize into its components
	 */
	public ProcessDetail(byte[] data)
	{
		readData(new ByteArrayDataBuffer(data));
	}

	/**
	 * Gets the definition Id associated with the process.
	 *
	 * @return the key of the definition from which the process is created
	 */
	public UUID getDefinitionId()
	{
		return definitionId;
	}

	/**
	 * Gets the process's component nids.
	 *
	 * @return map of component nids to ordered stamp sequences
	 */
	public Map<Integer, Stamp> getComponentToInitialEditMap()
	{
		return componentToIntitialEditMap;
	}

	/**
	 * Gets the process creator.
	 *
	 * @return the process creator's id
	 */
	public UUID getCreatorId()
	{
		return creatorId;
	}

	/**
	 * Gets the time the process was created as long primitive type.
	 *
	 * @return the time the process was created
	 */
	public long getTimeCreated()
	{
		return timeCreated;
	}

	/**
	 * Gets the time the process ended either via cancelation or conclusion as
	 * long primitive type.
	 *
	 * @return the time the process was canceled or concluded
	 */
	public long getTimeCanceledOrConcluded()
	{
		return timeCanceledOrConcluded;
	}

	/**
	 * Sets the time the process ended either via cancelation or conclusion as
	 * this isn't available during the object's construction.
	 *
	 * @param time
	 * The time the process was canceled/concluded as long primitive
	 * type
	 */
	public void setTimeCanceledOrConcluded(long time)
	{
		timeCanceledOrConcluded = time;
	}

	/**
	 * Get the time the process was launched as long primitive type
	 * 
	 * @return the time launched
	 */
	public long getTimeLaunched()
	{
		return timeLaunched;
	}

	/**
	 * Sets the time the process as launched as this isn't available during the
	 * object's construction.
	 *
	 * @param time
	 * The time the process was launched as long primitive type
	 */
	public void setTimeLaunched(long time)
	{
		timeLaunched = time;
	}

	/**
	 * Gets the process's current status.
	 *
	 * @return the process Status
	 */
	public ProcessStatus getStatus()
	{
		return status;
	}

	/**
	 * Sets the process's current status as this is updated over the course of
	 * the process.
	 *
	 * @param status
	 * The process's current status
	 */
	public void setStatus(ProcessStatus status)
	{
		this.status = status;
	}

	/**
	 * The name of the process
	 * 
	 * @return process name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * The description of the process
	 * 
	 * @return process description
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * Retrieves the current owner of the process
	 * 
	 * @return 0-based UUID: Process is not owned.  
	 * Otherwise, return the process's current owner id
	 */
	public UUID getOwnerId()
	{
		return ownerId;
	}

	/**
	 * Sets the current owner of the process
	 * 
	 * @param nid
	 * The userId obtaining a lock on the instance. Note '0' means no owner.
	 */
	public void setOwnerId(UUID nid)
	{
		ownerId = nid;
		ownerIdMsb = ownerId.getMostSignificantBits();
		ownerIdLsb = ownerId.getLeastSignificantBits();
	}

	public boolean isActive()
	{
		return status == ProcessStatus.LAUNCHED || status == ProcessStatus.DEFINED;
	}

	@Override
	protected void putAdditionalWorkflowFields(ByteArrayDataBuffer out) {
		out.putLong(definitionIdMsb);
		out.putLong(definitionIdLsb);

		out.putInt(componentToIntitialEditMap.size());
		for (Integer componentNid : componentToIntitialEditMap.keySet()) {
			out.putNid(componentNid);
			
	        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	        	stampSerializer.serialize(new DataOutputStream(baos), componentToIntitialEditMap.get(componentNid));
	        	out.putByteArrayField(baos.toByteArray());
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
		}
		
		out.putLong(creatorIdMsb);
		out.putLong(creatorIdLsb);
		out.putLong(timeCreated);
		out.putLong(timeLaunched);
		out.putLong(timeCanceledOrConcluded);
		out.putByteArrayField(status.name().getBytes());
		out.putByteArrayField(name.getBytes());
		out.putByteArrayField(description.getBytes());
		out.putLong(ownerIdMsb);
		out.putLong(ownerIdLsb);
	}

	@Override
	protected void getAdditionalWorkflowFields(ByteArrayDataBuffer in) {
		definitionIdMsb = in.getLong();
		definitionIdLsb = in.getLong();
		definitionId = new UUID(definitionIdMsb, definitionIdLsb);
		
		int collectionCount = in.getInt();
		for (int i = 0; i < collectionCount; i++) {
			int compNid = in.getNid();
			
	        try (ByteArrayInputStream baos = new ByteArrayInputStream(in.getByteArrayField())) {
	        	componentToIntitialEditMap.put(compNid, stampSerializer.deserialize(new DataInputStream(baos)));
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
		}

		creatorIdMsb = in.getLong();
		creatorIdLsb = in.getLong();
		creatorId = new UUID(creatorIdMsb, creatorIdLsb);
		timeCreated = in.getLong();
		timeLaunched = in.getLong();
		timeCanceledOrConcluded = in.getLong();
		status = ProcessStatus.valueOf(new String(in.getByteArrayField()));
		name = new String(in.getByteArrayField());
		description = new String(in.getByteArrayField());
		ownerIdMsb = in.getLong();
		ownerIdLsb = in.getLong();
		ownerId = new UUID(ownerIdMsb, ownerIdLsb);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();

		for (Integer compNid : componentToIntitialEditMap.keySet())
		{
			buf.append("\n\t\t\tFor Component Nid: " + compNid + " first edited in workflow at Stamp: " + componentToIntitialEditMap.get(compNid));
		}

		Date date = new Date(timeCreated);
		String timeCreatedString = workflowDateFormatter.format(date);

		date = new Date(timeLaunched);
		String timeLaunchedString = workflowDateFormatter.format(date);

		date = new Date(timeCanceledOrConcluded);
		String timeCanceledOrConcludedString = workflowDateFormatter.format(date);

		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tComponents to Sequences Map: " + buf.toString() + "\n\t\tCreator Id: "
				+ creatorId.toString() + "\n\t\tTime Created: " + timeCreatedString + "\n\t\tTime Launched: " + timeLaunchedString + "\n\t\tTime Canceled or Concluded: "
				+ timeCanceledOrConcludedString + "\n\t\tStatus: " + status + "\n\t\tName: " + name + "\n\t\tDescription: " + description + "\n\t\tOwner Id: "
				+ ownerId.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		ProcessDetail other = (ProcessDetail) obj;

		return this.definitionId.equals(other.definitionId) && this.componentToIntitialEditMap.equals(other.componentToIntitialEditMap) && this.creatorId.equals(other.creatorId)
				&& this.timeCreated == other.timeCreated && this.timeLaunched == other.timeLaunched && this.timeCanceledOrConcluded == other.timeCanceledOrConcluded
				&& this.status == other.status && this.name.equals(other.name) && this.description.equals(other.description) && this.ownerId.equals(other.ownerId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return definitionId.hashCode() + componentToIntitialEditMap.hashCode() + creatorId.hashCode() + new Long(timeCreated).hashCode() + new Long(timeLaunched).hashCode()
				+ new Long(timeCanceledOrConcluded).hashCode() + status.hashCode() + name.hashCode() + description.hashCode() + ownerId.hashCode();
	}
}
