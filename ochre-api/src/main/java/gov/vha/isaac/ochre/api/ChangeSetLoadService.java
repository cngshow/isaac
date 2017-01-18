package gov.vha.isaac.ochre.api;

import java.io.IOException;
import org.jvnet.hk2.annotations.Contract;

@Contract
public interface ChangeSetLoadService {
	
	/**
	 * Call to trigger a re-read of changeset files - which may be necessary after a remote sync, for example.
	 * returns the number of files loaded
	 */
	public int readChangesetFiles() throws IOException;

}