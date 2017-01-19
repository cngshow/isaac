package gov.va.isaac.sync.git.gitblit.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.AccessRestrictionType;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.AuthorizationControl;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.FederationStrategy;
import gov.va.isaac.sync.git.gitblit.utils.StringUtils;

/**
 * RepositoryModel is a serializable model class that represents a Gitblit
 * repository including its configuration settings and access restriction.
 *
 *
 */
public class RepositoryModel implements Serializable, Comparable<RepositoryModel> {

	private static final long serialVersionUID = 1L;

	public String name;
	public String description;
	public List<String> owners;
	public Date lastChange;
	public String accessRestriction;
	public String authorizationControl;
	public String federationStrategy;
	public List<String> federationSets;
	public boolean isBare;
	public String projectPath;
	private String displayName;
	public boolean acceptNewPatchsets;
	public boolean acceptNewTickets;


	public RepositoryModel(String name, String description, String owner, Date lastchange) {
		this.name = name;
		this.description = description;
		this.lastChange = lastchange;
		this.accessRestriction = AccessRestrictionType.VIEW.toString();
		this.authorizationControl = AuthorizationControl.NAMED.toString();
		this.federationSets = new ArrayList<String>();
		this.federationStrategy = FederationStrategy.FEDERATE_THIS.toString();
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
