package gov.vha.isaac.ochre.stamp.provider;

import gov.vha.isaac.ochre.api.*;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.collections.ConcurrentObjectIntMap;
import gov.vha.isaac.ochre.api.collections.ConcurrentSequenceSerializedObjectMap;
import gov.vha.isaac.ochre.api.commit.Stamp;
import gov.vha.isaac.ochre.api.commit.StampService;
import gov.vha.isaac.ochre.api.commit.UncommittedStamp;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.task.TimedTask;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * Created by kec on 1/2/16.
 */
@Service(name = "Stamp Provider")
@RunLevel(value = 1)
public class StampProvider implements StampService {

    private static final Logger LOG = LogManager.getLogger();
    private static final String STAMP_MANAGER_DATA_FILENAME = "stamp-manager.data";
    public static final String DEFAULT_STAMP_MANAGER_FOLDER = "stamp-manager";
    private final ReentrantLock stampLock = new ReentrantLock();

    private final AtomicInteger nextStampSequence = new AtomicInteger(FIRST_STAMP_SEQUENCE);
    /**
     * TODO: persist across restarts.
     */
    private static final Map<UncommittedStamp, Integer> UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP
            = new ConcurrentHashMap<>();


    /**
     * Persistent map of stamp sequences to a Stamp object.
     */
    private final ConcurrentObjectIntMap<Stamp> stampMap = new ConcurrentObjectIntMap<>();
    private AtomicBoolean loadRequired = new AtomicBoolean();
    private final Path dbFolderPath;
    private final Path stampManagerFolder;


    /**
     * Persistent as a result of reading and writing the stampMap.
     */
    private final ConcurrentSequenceSerializedObjectMap<Stamp> inverseStampMap;

    public StampProvider() throws IOException {
        dbFolderPath = LookupService.getService(ConfigurationService.class).getChronicleFolderPath().resolve("stamp-provider");
        loadRequired.set(Files.exists(dbFolderPath));
        Files.createDirectories(dbFolderPath);
        inverseStampMap = new ConcurrentSequenceSerializedObjectMap<>(new StampSerializer(),
                dbFolderPath, null, null);
        stampManagerFolder = dbFolderPath.resolve(DEFAULT_STAMP_MANAGER_FOLDER);
        Files.createDirectories(stampManagerFolder);
    }

    @PostConstruct
    private void startMe() {
        try {
            LOG.info("Starting StampProvider post-construct");
            if (loadRequired.get()) {
                LOG.info("Reading existing commit manager data. ");
                LOG.info("Reading " + STAMP_MANAGER_DATA_FILENAME);
                try (DataInputStream in = new DataInputStream(new FileInputStream(new File(stampManagerFolder.toFile(), STAMP_MANAGER_DATA_FILENAME)))) {
                    nextStampSequence.set(in.readInt());
                    int stampMapSize = in.readInt();
                    for (int i = 0; i < stampMapSize; i++) {
                        int stampSequence = in.readInt();
                        Stamp stamp = new Stamp(in);
                        stampMap.put(stamp, stampSequence);
                        inverseStampMap.put(stampSequence, stamp);
                    }

                    int uncommittedSize = in.readInt();
                    for (int i = 0; i < uncommittedSize; i++) {
                        UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.put(new UncommittedStamp(in), in.readInt());
                    }
                }
            }
        } catch (Exception e) {
            LookupService.getService(SystemStatusService.class).notifyServiceConfigurationFailure("Stamp Provider", e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    private void stopMe() {
        LOG.info("Stopping StampProvider pre-destroy. ");

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(stampManagerFolder.toFile(), STAMP_MANAGER_DATA_FILENAME)))) {
            out.writeInt(nextStampSequence.get());
            out.writeInt(stampMap.size());
            stampMap.forEachPair((Stamp stamp, int stampSequence) -> {
                try {
                    out.writeInt(stampSequence);
                    stamp.write(out);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            int size = UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.size();
            out.writeInt(size);

            for (Map.Entry<UncommittedStamp, Integer> entry : UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.entrySet()) {
                entry.getKey().write(out);
                out.writeInt(entry.getValue());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public int getAuthorSequenceForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return TermAux.USER.getConceptSequence();
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return Get.identifierService().getConceptSequence(
                    s.get().getAuthorSequence());
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    public int getAuthorNidForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return TermAux.USER.getNid();
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return s.get().getAuthorSequence();
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    @Override
    public int getModuleSequenceForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return TermAux.UNSPECIFIED_MODULE.getConceptSequence();
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return Get.identifierService().getConceptSequence(
                    s.get().getModuleSequence());
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    private int getModuleNidForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return TermAux.UNSPECIFIED_MODULE.getNid();
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return s.get().getModuleSequence();
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    ConcurrentHashMap<Integer, Integer> stampSequencePathSequenceMap = new ConcurrentHashMap();

    @Override
    public int getPathSequenceForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return TermAux.DEVELOPMENT_PATH.getConceptSequence();
        }
        if (stampSequencePathSequenceMap.containsKey(stampSequence)) {
            return stampSequencePathSequenceMap.get(stampSequence);
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            stampSequencePathSequenceMap.put(stampSequence, Get.identifierService().getConceptSequence(
                    s.get().getPathSequence()));
            return stampSequencePathSequenceMap.get(stampSequence);
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    private int getPathNidForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return TermAux.PATH.getNid();
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return s.get().getPathSequence();
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    @Override
    public State getStatusForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return State.CANCELED;
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return s.get().getStatus();
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence);
    }

    @Override
    public long getTimeForStamp(int stampSequence) {
        if (stampSequence < 0) {
            return Long.MIN_VALUE;
        }
        Optional<Stamp> s = inverseStampMap.get(stampSequence);
        if (s.isPresent()) {
            return s.get().getTime();
        }
        throw new NoSuchElementException("No stampSequence found: " + stampSequence
                + " map size: " + stampMap.size()
                + " inverse map size: " + inverseStampMap.getSize());
    }

    @Override
    public int getStampSequence(State status, long time, int authorSequence, int moduleSequence, int pathSequence) {
        Stamp stampKey = new Stamp(status, time,
                authorSequence,
                moduleSequence,
                pathSequence);

        if (time == Long.MAX_VALUE) {
            UncommittedStamp usp = new UncommittedStamp(status, authorSequence,
                    moduleSequence, pathSequence);
            if (UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.containsKey(usp)) {
                return UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.get(usp);
            } else {
                stampLock.lock();

                try {
                    if (UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.containsKey(usp)) {
                        return UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.get(usp);
                    }

                    int stampSequence = nextStampSequence.getAndIncrement();
                    UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.put(usp, stampSequence);
                    inverseStampMap.put(stampSequence, stampKey);

                    return stampSequence;
                } finally {
                    stampLock.unlock();
                }
            }
        }

        OptionalInt stampValue = stampMap.get(stampKey);
        if (!stampValue.isPresent()) {
            // maybe have a few available in an atomic queue, and put back
            // if not used? Maybe in a thread-local?
            // Have different sequences, and have the increments be equal to the
            // number of sequences?
            stampLock.lock();
            try {
                stampValue = stampMap.get(stampKey);
                if (!stampValue.isPresent()) {
                    stampValue = OptionalInt.of(nextStampSequence.getAndIncrement());
                    inverseStampMap.put(stampValue.getAsInt(), stampKey);
                    stampMap.put(stampKey, stampValue.getAsInt());
                }
            } finally {
                stampLock.unlock();
            }
        }
        return stampValue.getAsInt();
    }


    @Override
    public IntStream getStampSequences() {
        return IntStream.rangeClosed(FIRST_STAMP_SEQUENCE, nextStampSequence.get()).
                filter((stampSequence) -> inverseStampMap.containsKey(stampSequence));
    }

    @Override
    public String describeStampSequence(int stampSequence) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stamp<Begin>");
        sb.append(stampSequence);
        sb.append("::");
        State status = getStatusForStamp(stampSequence);
        sb.append(status);
        if (status == State.ACTIVE) {
            sb.append("  ");
        }
        sb.append(" ");

        long time = getTimeForStamp(stampSequence);
        if (time == Long.MAX_VALUE) {
            sb.append("UNCOMMITTED:");
        } else if (time == Long.MIN_VALUE) {
            sb.append("CANCELED:  ");
        } else {
            sb.append(Instant.ofEpochMilli(time));
        }
        sb.append(" a:");
        sb.append(Get.conceptDescriptionText(getAuthorSequenceForStamp(stampSequence)));
        sb.append(" <");
        sb.append(getAuthorSequenceForStamp(stampSequence));
        sb.append(">");
        sb.append(" m:");
        sb.append(Get.conceptDescriptionText(getModuleSequenceForStamp(stampSequence)));
        sb.append(" <");
        sb.append(getModuleSequenceForStamp(stampSequence));
        sb.append(">");
        sb.append(" p: ");
        sb.append(Get.conceptDescriptionText(getPathSequenceForStamp(stampSequence)));
        sb.append(" <");
        sb.append(getPathSequenceForStamp(stampSequence));
        sb.append("> <Stamp End>");
        return sb.toString();
    }

    @Override
    public void addStamp(Stamp stamp, int stampSequence) {
        stampMap.put(stamp, stampSequence);
        inverseStampMap.put(stampSequence, stamp);
    }

    @Override
    public boolean stampSequencesEqualExceptAuthorAndTime(int stampSequence1, int stampSequence2) {
        if (getModuleNidForStamp(stampSequence1) != getModuleNidForStamp(stampSequence2)) {
            return false;
        }
        if (getPathNidForStamp(stampSequence1) != getPathNidForStamp(stampSequence2)) {
            return false;
        }
        return getStatusForStamp(stampSequence1) == getStatusForStamp(stampSequence2);
    }

    @Override
    public boolean isNotCanceled(int stamp) {
        if (stamp < 0) {
            return false;
        }
        return getTimeForStamp(stamp) != Long.MIN_VALUE;
    }

    @Override
    public boolean isUncommitted(int stampSequence) {
        return getTimeForStamp(stampSequence) == Long.MAX_VALUE;
    }

    @Override
    public int getRetiredStampSequence(int stampSequence) {
        return getStampSequence(State.INACTIVE,
                getTimeForStamp(stampSequence),
                getAuthorSequenceForStamp(stampSequence),
                getModuleSequenceForStamp(stampSequence),
                getPathSequenceForStamp(stampSequence));
    }

    @Override
    public int getActivatedStampSequence(int stampSequence) {
        return getStampSequence(State.ACTIVE,
                getTimeForStamp(stampSequence),
                getAuthorSequenceForStamp(stampSequence),
                getModuleSequenceForStamp(stampSequence),
                getPathSequenceForStamp(stampSequence));
    }

    @Override
    public synchronized Task<Void> cancel(int authorSequence) {
        UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.forEach((uncommittedStamp, stampSequence) -> {
            // for each uncommitted stamp matching the author, remove the uncommitted stamp
            // and replace with a canceled stamp.
            if (uncommittedStamp.authorSequence == authorSequence) {
                Stamp stamp = new Stamp(uncommittedStamp.status,
                        Long.MIN_VALUE,
                        uncommittedStamp.authorSequence,
                        uncommittedStamp.moduleSequence,
                        uncommittedStamp.pathSequence);
                addStamp(stamp, stampSequence);
                UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.remove(uncommittedStamp);
            }
        });
        // TODO make asynchronous with a actual task.
        Task<Void> task = new TimedTask() {

            @Override
            protected Object call() throws Exception {
                Get.activeTasks().remove(this);
                return null;
            }
        };

        Get.activeTasks().add(task);
        Get.workExecutors().getExecutor().execute(task);
        return task;
    }

    @Override
    synchronized public Map<UncommittedStamp, Integer>  getPendingStampsForCommit() {
        Map<UncommittedStamp, Integer> pendingStampsForCommit = new HashMap<>(UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP);
        UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.clear();
        return pendingStampsForCommit;
    }

    @Override
     synchronized public void setPendingStampsForCommit(Map<UncommittedStamp, Integer> pendingStamps) {
        UNCOMMITTED_STAMP_TO_STAMP_SEQUENCE_MAP.putAll(pendingStamps);
    }
}
