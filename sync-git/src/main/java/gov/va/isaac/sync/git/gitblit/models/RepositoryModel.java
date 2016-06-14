/*
 * Copyright 2011 gitblit.com.
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
package gov.va.isaac.sync.git.gitblit.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.va.isaac.sync.git.gitblit.utils.StringUtils;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.AccessRestrictionType;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.AuthorizationControl;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.FederationStrategy;



/**
 * RepositoryModel is a serializable model class that represents a Gitblit
 * repository including its configuration settings and access restriction.
 *
 * @author James Moger
 *
 */
public class RepositoryModel implements Serializable, Comparable<RepositoryModel> {

	private static final long serialVersionUID = 1L;

	// field names are reflectively mapped in EditRepository page
	public String name;
	public String description;
	public List<String> owners;
	public Date lastChange;
	public String lastChangeAuthor;
	public boolean hasCommits;
	public boolean showRemoteBranches;
	public boolean useIncrementalPushTags;
	public String incrementalPushTagPrefix;
	public AccessRestrictionType accessRestriction;
	public AuthorizationControl authorizationControl;
	public boolean allowAuthenticated;
	public boolean isFrozen;
	public FederationStrategy federationStrategy;
	public List<String> federationSets;
	public boolean isFederated;
	public boolean skipSizeCalculation;
	public boolean skipSummaryMetrics;
	public String frequency;
	public boolean isBare;
	public boolean isMirror;
	public String origin;
	public String HEAD;
	public List<String> availableRefs;
	public List<String> indexedBranches;
	public String size;
	public List<String> preReceiveScripts;
	public List<String> postReceiveScripts;
	public List<String> mailingLists;
	public Map<String, String> customFields;
	public String projectPath;
	private String displayName;
	public boolean allowForks;
	public Set<String> forks;
	public String originRepository;
	public boolean verifyCommitter;
	public String gcThreshold;
	public int gcPeriod;
	public int maxActivityCommits;
	public List<String> metricAuthorExclusions;
	public boolean acceptNewPatchsets;
	public boolean acceptNewTickets;
	public boolean requireApproval;
	public String mergeTo;

	public transient boolean isCollectingGarbage;
	public Date lastGC;
	public String sparkleshareId;

	public RepositoryModel() {
		this("", "", "", new Date(0));
	}

	public RepositoryModel(String name, String description, String owner, Date lastchange) {
		this.name = name;
		this.description = description;
		this.lastChange = lastchange;
		this.accessRestriction = AccessRestrictionType.NONE;
		this.authorizationControl = AuthorizationControl.NAMED;
		this.federationSets = new ArrayList<String>();
		this.federationStrategy = FederationStrategy.FEDERATE_THIS;
		this.projectPath = StringUtils.getFirstPathElement(name);
		this.owners = new ArrayList<String>();
		this.isBare = true;
		this.acceptNewTickets = true;
		this.acceptNewPatchsets = true;

		addOwner(owner);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RepositoryModel) {
			return name.equals(((RepositoryModel) o).name);
		}
		return false;
	}

	@Override
	public String toString() {
		if (displayName == null) {
			displayName = StringUtils.stripDotGit(name);
		}
		return displayName;
	}

	@Override
	public int compareTo(RepositoryModel o) {
		return StringUtils.compareRepositoryNames(name, o.name);
	}


	public void addOwner(String username) {
		if (!StringUtils.isEmpty(username)) {
			String name = username.toLowerCase();
			// a set would be more efficient, but this complicates JSON
			// deserialization so we enforce uniqueness with an arraylist
			if (!owners.contains(name)) {
				owners.add(name);
			}
		}
	}
}
