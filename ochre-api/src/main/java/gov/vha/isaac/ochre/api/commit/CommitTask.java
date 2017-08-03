package gov.vha.isaac.ochre.api.commit;

import java.util.Optional;
import java.util.Set;
import gov.vha.isaac.ochre.api.task.TimedTask;

public abstract class CommitTask extends TimedTask<Optional<CommitRecord>>
{
	/**
	 * If there were issues that caused a Commit to not be successful - this task returns an Optiona.empty() - you can check the 
	 * alert collection to find out why.
	 * @return the alerts generated during the commit attempt.
	 */
	public abstract Set<Alert> getAlerts();
}
