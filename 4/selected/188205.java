package sf.net.experimaestro.scheduler;

import static java.lang.String.format;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.utils.PID;
import sf.net.experimaestro.utils.log.Logger;
import bpiwowar.argparser.utils.ReadLineIterator;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * 
 */
@Entity
public abstract class Resource implements Comparable<Resource> {

    private static final Logger LOGGER = Logger.getLogger();

    /**
	 * The task identifier
	 */
    @PrimaryKey
    String identifier;

    /**
	 * Groups this resource belongs to
	 */
    @SecondaryKey(name = "groups", relate = Relationship.MANY_TO_MANY)
    Set<String> groups = new TreeSet<String>();

    /**
	 * True when the resource has been generated
	 */
    protected ResourceState state;

    /**
	 * The access mode
	 */
    private LockMode lockmode;

    /**
	 * Task manager
	 */
    transient Scheduler scheduler;

    /**
	 * Our set of listeners (resources that are listening to changes in the
	 * state of this resource)
	 */
    @SecondaryKey(name = "listeners", relate = Relationship.ONE_TO_MANY, relatedEntity = Resource.class)
    Set<String> listeners = new TreeSet<String>();

    /**
	 * If the resource is currently locked
	 */
    boolean locked;

    protected Resource() {
    }

    /**
	 * Constructs a resource
	 * 
	 * @param taskManager
	 * @param identifier
	 * @param mode
	 */
    public Resource(Scheduler taskManager, String identifier, LockMode mode) {
        this.scheduler = taskManager;
        this.identifier = identifier;
        this.lockmode = mode;
        lockfile = new File(identifier + ".lock");
    }

    /**
	 * Register a task that waits for our output
	 */
    public synchronized void register(Job job) {
        listeners.add(new String(job.getIdentifier()));
    }

    /**
	 * Unregister a task
	 */
    public void unregister(Job task) {
        listeners.remove(task);
    }

    /**
	 * Called when we have generated the resources (either by running it or
	 * producing it)
	 * @throws DatabaseException 
	 */
    void notifyListeners(Object... objects) throws DatabaseException {
        for (String id : listeners) {
            Job resource = (Job) scheduler.getResource(id);
            resource.notify(this, objects);
        }
    }

    /**
	 * Compares to another resource (based on the identifier)
	 */
    public int compareTo(Resource o) {
        return identifier.compareTo(o.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return identifier.toString();
    }

    /**
	 * Update the status of this resource
	 */
    boolean updateStatus() {
        boolean updated = update();
        if (new File(identifier + ".done").exists() && this.getState() != ResourceState.DONE) {
            updated = true;
            this.state = ResourceState.DONE;
        }
        boolean locked = new File(identifier + ".lock").exists();
        updated |= locked != this.locked;
        this.locked = locked;
        return updated;
    }

    public static enum DependencyStatus {

        /**
		 * The resource can be used as is
		 */
        OK, /**
		 * The resouce can be used when properly locked
		 */
        OK_LOCK, /**
		 * The resource is not ready yet
		 */
        WAIT, /**
		 * The resource will not be ready given the current state
		 */
        ERROR;

        public boolean isOK() {
            return this == OK_LOCK || this == OK;
        }
    }

    /**
	 * Can the dependency be accepted?
	 * 
	 * @param locktype
	 * @return {@link DependencyStatus#OK} if the dependency is satisfied,
	 *         {@link DependencyStatus#WAIT} if it can be satisfied and
	 *         {@link DependencyStatus#ERROR} if it cannot be satisfied
	 */
    DependencyStatus accept(LockType locktype) {
        LOGGER.debug("Checking lock %s for resource %s (generated %b)", locktype, this, getState());
        if (state == ResourceState.ERROR) return DependencyStatus.ERROR;
        if (state == ResourceState.WAITING) return DependencyStatus.WAIT;
        switch(locktype) {
            case GENERATED:
                return getState() == ResourceState.DONE ? DependencyStatus.OK : DependencyStatus.WAIT;
            case EXCLUSIVE_ACCESS:
                return writers == 0 && readers == 0 ? DependencyStatus.OK_LOCK : DependencyStatus.WAIT;
            case READ_ACCESS:
                switch(lockmode) {
                    case EXCLUSIVE_WRITER:
                    case SINGLE_WRITER:
                        return writers == 0 ? DependencyStatus.OK : DependencyStatus.WAIT;
                    case MULTIPLE_WRITER:
                        return DependencyStatus.OK;
                    case READ_ONLY:
                        return getState() == ResourceState.DONE ? DependencyStatus.OK : DependencyStatus.WAIT;
                }
                break;
            case WRITE_ACCESS:
                switch(lockmode) {
                    case EXCLUSIVE_WRITER:
                        return writers == 0 && readers == 0 ? DependencyStatus.OK_LOCK : DependencyStatus.WAIT;
                    case MULTIPLE_WRITER:
                        return DependencyStatus.OK;
                    case READ_ONLY:
                        return DependencyStatus.ERROR;
                    case SINGLE_WRITER:
                        return writers == 0 ? DependencyStatus.OK_LOCK : DependencyStatus.WAIT;
                }
        }
        return DependencyStatus.ERROR;
    }

    /**
	 * Tries to lock the resource
	 * 
	 * @param dependency
	 * @throws UnlockableException
	 */
    public Lock lock(int pid, LockType dependency) throws UnlockableException {
        switch(accept(dependency)) {
            case WAIT:
                throw new UnlockableException("Cannot grant dependency %s for resource %s", dependency, this);
            case ERROR:
                throw new RuntimeException(format("Resource %s cannot accept dependency %s", this, dependency));
            case OK_LOCK:
                return new FileLock(lockfile);
            case OK:
                return new StatusLock(PID.getPID(), dependency == LockType.WRITE_ACCESS);
        }
        return null;
    }

    /**
	 * @return the generated
	 */
    public boolean isGenerated() {
        return getState() == ResourceState.DONE;
    }

    /** Useful files */
    transient File lockfile, statusFile;

    /** Number of readers and writers */
    int writers = 0, readers = 0;

    /** Last check of the status file */
    long lastUpdate = 0;

    /** Current list */
    transient TreeMap<Integer, Boolean> processMap = new TreeMap<Integer, Boolean>();

    public int getReaders() {
        return readers;
    }

    public int getWriters() {
        return writers;
    }

    File getStatusFile() {
        if (statusFile == null) statusFile = new File(identifier + ".status");
        return statusFile;
    }

    File getLockFile() {
        if (lockfile == null) lockfile = new File(identifier + ".lock");
        return lockfile;
    }

    /**
	 * Update the status of the resource
	 * 
	 * @param processMap
	 *            Fill up a list
	 * 
	 * @return A boolean specifying whether something was updated
	 * 
	 * @throws FileNotFoundException
	 *             If some error occurs while reading status
	 */
    public boolean update() {
        boolean updated = writers > 0 || readers > 0;
        if (!getStatusFile().exists()) {
            writers = readers = 0;
            return updated;
        } else {
            long lastModified = statusFile.lastModified();
            if (lastUpdate >= lastModified) return false;
            lastUpdate = lastModified;
            try {
                for (String line : new ReadLineIterator(statusFile)) {
                    String[] fields = line.split("\\s+");
                    if (fields.length != 2) LOGGER.error("Skipping line %s (wrong number of fields)", line); else {
                        if (fields[1].equals("r")) readers += 1; else if (fields[1].equals("w")) writers += 1; else LOGGER.error("Skipping line %s (unkown mode %s)", fields[1]);
                        if (processMap != null) processMap.put(Integer.parseInt(fields[0]), fields[1].equals("w"));
                    }
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
	 * @param pidFrom
	 *            The old PID (or 0 if none)
	 * @param pidTo
	 *            The new PID (or 0 if none)
	 * @param mode
	 *            Status
	 * @throws UnlockableException
	 */
    void updateStatusFile(int pidFrom, int pidTo, boolean writeAccess) throws UnlockableException {
        FileLock fileLock = new FileLock(getLockFile());
        try {
            update();
            if (pidFrom <= 0) {
                PrintStream out = new PrintStream(new FileOutputStream(statusFile, true));
                out.format("%d %s%n", pidTo, writeAccess ? "w" : "r");
                out.close();
            } else {
                File file = new File(statusFile.getParent(), statusFile.getName() + ".tmp");
                PrintStream out = new PrintStream(new FileOutputStream(file));
                processMap.remove(pidFrom);
                if (pidTo > 0) processMap.put(pidTo, writeAccess);
                for (Entry<Integer, Boolean> x : processMap.entrySet()) out.format("%d %s%n", x.getKey(), x.getValue() ? "w" : "r");
                out.close();
                file.renameTo(statusFile);
            }
        } catch (FileNotFoundException e) {
            throw new UnlockableException("Status file '%s' could not be created", statusFile);
        } finally {
            if (fileLock != null) fileLock.dispose();
        }
    }

    /**
	 * Defines a status lock
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
    public class StatusLock implements Lock {

        private int pid;

        private final boolean writeAccess;

        public StatusLock(int pid, final boolean writeAccess) throws UnlockableException {
            this.pid = pid;
            this.writeAccess = writeAccess;
            updateStatusFile(-1, pid, writeAccess);
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    try {
                        updateStatusFile(StatusLock.this.pid, -1, writeAccess);
                    } catch (UnlockableException e) {
                        LOGGER.warn("Unable to change status file %s (tried to remove pid %d)", statusFile, StatusLock.this.pid);
                    }
                }
            });
        }

        /**
		 * Change the PID in the status file
		 * 
		 * @throws UnlockableException
		 */
        public void changePID(int pid) throws UnlockableException {
            updateStatusFile(this.pid, pid, writeAccess);
            this.pid = pid;
        }

        public boolean dispose() {
            try {
                updateStatusFile(pid, -1, writeAccess);
            } catch (UnlockableException e) {
                return false;
            }
            return true;
        }

        public void changeOwnership(int pid) {
            try {
                updateStatusFile(this.pid, pid, writeAccess);
            } catch (UnlockableException e) {
                return;
            }
            this.pid = pid;
        }
    }

    /**
	 * Get the task identifier
	 * 
	 * @return The task unique identifier
	 */
    public String getIdentifier() {
        return identifier;
    }

    /**
	 * Is the resource locked?
	 */
    public boolean isLocked() {
        return locked;
    }

    /**
	 * Get the state of the resource
	 * 
	 * @return
	 */
    public ResourceState getState() {
        return state;
    }

    /**
	 * Checks whether a resource should be kept in main memory (e.g.,
	 * running/waiting jobs & monitored resources)
	 * 
	 * When overriding this method, use
	 * <code>super.isActive() || condition</code>
	 * 
	 * @return
	 */
    protected boolean isActive() {
        return !listeners.isEmpty();
    }

    /**
	 * Returns the list of listeners
	 */
    public Set<String> getListeners() {
        return listeners;
    }

    /**
	 * Update the database after a change in state
	 */
    void updateDb() {
        try {
            scheduler.store(this);
        } catch (DatabaseException e) {
            LOGGER.error("Could not update the information in the database for %s", this);
        }
    }

    public static class PrintConfig {

        public String detailURL;
    }

    /**
	 * Writes an HTML description of the resource
	 * 
	 * @param out
	 * @param detailURL
	 */
    public void printHTML(PrintWriter out, PrintConfig config) {
        out.format("<div><b>Resource id</b>: %s</h2>", identifier);
        out.format("<div><b>Status</b>: %s</div>", state);
    }
}
