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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.commit.manager;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.commit.ChangeSetListener;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.commit.PostCommitService;

/**
 *
 * @author Nuno Marques
 */
@Service(name = "Post Commit Provider")
@RunLevel(value = 1)
public class PostCommitProvider implements PostCommitService {

	private static final Logger LOG = LogManager.getLogger();
	ConcurrentSkipListSet<WeakReference<ChangeSetListener>> changeSetListeners = new ConcurrentSkipListSet<>();

	private PostCommitProvider() {
		//for HK2
	}

	@PostConstruct
	private void startMe() {
		LOG.info("Starting PostCommitProvider post-construct");
	}

	@PreDestroy
	private void stopMe() {
		LOG.info("Stopping PostCommitProvider pre-destroy. ");
	}

	@Override
	public void postCommitNotification(CommitRecord commitRecord) {
		LOG.debug("change set listeners size: {}", changeSetListeners.size());
		changeSetListeners.forEach((listenerReference) -> {
			ChangeSetListener listener = listenerReference.get();
			if (listener == null) {
				changeSetListeners.remove(listenerReference);
			} else {
				listener.handlePostCommit(commitRecord);
			}
		});
	}

	@Override
	public void addChangeSetListener(ChangeSetListener changeSetListener) {
		LOG.debug("add listener");
		changeSetListeners.add(new ChangeSetListenerReference(changeSetListener));
	}

	@Override
	public void removeChangeSetListener(ChangeSetListener changeSetListener) {
		LOG.debug("remove listener");
		changeSetListeners.remove(new ChangeSetListenerReference(changeSetListener));
	}


	private static class ChangeSetListenerReference extends WeakReference<ChangeSetListener> implements Comparable<ChangeSetListenerReference> {

		UUID listenerUuid;

		public ChangeSetListenerReference(ChangeSetListener referent) {
			super(referent);
			this.listenerUuid = referent.getListenerUuid();
		}

		public ChangeSetListenerReference(ChangeSetListener referent, ReferenceQueue<? super ChangeSetListener> q) {
			super(referent, q);
			this.listenerUuid = referent.getListenerUuid();
		}

		@Override
		public int compareTo(ChangeSetListenerReference o) {
			return this.listenerUuid.compareTo(o.listenerUuid);
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 67 * hash + Objects.hashCode(this.listenerUuid);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ChangeSetListenerReference other = (ChangeSetListenerReference) obj;
			return Objects.equals(this.listenerUuid, other.listenerUuid);
		}

	}

}
