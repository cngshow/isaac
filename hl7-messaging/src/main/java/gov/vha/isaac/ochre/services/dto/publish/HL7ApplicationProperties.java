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

package gov.vha.isaac.ochre.services.dto.publish;

public class HL7ApplicationProperties implements ApplicationProperties
{
	// Application Server Message String
	private String applicationServerName;
	private String applicationVersion;

	// Listener Port
	private int listenerPort;

	// Sending Facility Site ID
	private String sendingFacilityNamespaceId;

	// Target Vitria Interface Engine
	private String interfaceEngineURL;
	private boolean useInterfaceEngine;
	private String environment;

	// Encoding type
	private String hl7EncodingType;

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getApplicationServerName()
	 */
	@Override
	public String getApplicationServerName() {
		return this.applicationServerName;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setApplicationServerName(java.lang.String)
	 */
	@Override
	public void setApplicationServerName(String applicationServerName) {
		this.applicationServerName = applicationServerName;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getApplicationVersion()
	 */
	@Override
	public String getApplicationVersion() {
		return this.applicationVersion;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setApplicationVersion(java.lang.String)
	 */
	@Override
	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getListenerPort()
	 */
	@Override
	public int getListenerPort() {
		return this.listenerPort;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setListenerPort(int)
	 */
	@Override
	public void setListenerPort(int listenerPort) {
		this.listenerPort = listenerPort;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getSendingFacilityNamespaceId()
	 */
	@Override
	public String getSendingFacilityNamespaceId() {
		return this.sendingFacilityNamespaceId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setSendingFacilityNamespaceId(java.lang.String)
	 */
	@Override
	public void setSendingFacilityNamespaceId(String sendingFacilityNamespaceId) {
		this.sendingFacilityNamespaceId = sendingFacilityNamespaceId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getInterfaceEngineURL()
	 */
	@Override
	public String getInterfaceEngineURL() {
		return this.interfaceEngineURL;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setInterfaceEngineURL(java.net.URL)
	 */
	@Override
	public void setInterfaceEngineURL(String interfaceEngineURL) {
		this.interfaceEngineURL = interfaceEngineURL;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getUseInterfaceEngine()
	 */
	@Override
	public boolean getUseInterfaceEngine() {
		return this.useInterfaceEngine;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setUseInterfaceEngine(boolean)
	 */
	@Override
	public void setUseInterfaceEngine(boolean useInterfaceEngine) {
		this.useInterfaceEngine = useInterfaceEngine;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#getHl7EncodingType()
	 */
	@Override
	public String getHl7EncodingType() {
		return this.hl7EncodingType;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties#setHl7EncodingType(java.lang.String)
	 */
	@Override
	public void setHl7EncodingType(String hl7EncodingType) {
		this.hl7EncodingType = hl7EncodingType;
	}

}
