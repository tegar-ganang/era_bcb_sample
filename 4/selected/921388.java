package sf.net.experimaestro.scheduler;

import static java.lang.String.format;
import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.server.XPMServlet;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.PID;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.log.Logger;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;

/**
 * A job is a resource that can be run - that starts and ends (which
 * differentiate it with a server) and generate data
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent()
public class Job extends Resource implements HeapElement<Job>, Runnable {

    private static final Logger LOGGER = Logger.getLogger();

    protected Job() {
    }

    /**
	 * Initialisation of a task
	 * 
	 * @param taskManager
	 */
    public Job(Scheduler taskManager, String identifier) {
        super(taskManager, identifier, LockMode.EXCLUSIVE_WRITER);
        state = isDone() ? ResourceState.DONE : ResourceState.WAITING;
    }

    private boolean isDone() {
        return new File(identifier + ".done").exists();
    }

    /**
	 * The priority of the job (the higher, the more urgent)
	 */
    int priority;

    /**
	 * When was the job submitted (in case the priority is not enough)
	 */
    long timestamp = System.currentTimeMillis();

    /**
	 * When did the job start (0 if not started)
	 */
    private long startTimestamp;

    /**
	 * When did the job stop (0 when it did not stop yet)
	 */
    long endTimestamp;

    @Override
    protected boolean isActive() {
        return super.isActive() || state == ResourceState.WAITING || state == ResourceState.RUNNING;
    }

    /**
	 * The dependencies for this job (dependencies are on any resource)
	 */
    private TreeMap<String, Dependency> dependencies = new TreeMap<String, Dependency>();

    /**
	 * Number of unsatisfied dependencies
	 */
    int nbUnsatisfied;

    /**
	 * The set of dependencies for this object
	 * 
	 * @return
	 */
    public TreeMap<String, Dependency> getDependencies() {
        return dependencies;
    }

    /**
	 * Add a dependency
	 * 
	 * @param data
	 *            The data we depend upon
	 */
    public void addDependency(Resource resource, LockType type) {
        LOGGER.info("Adding dependency %s to %s for %s", type, resource, this);
        final DependencyStatus accept = resource.accept(type);
        if (accept == DependencyStatus.ERROR) throw new RuntimeException(format("Resource %s cannot be satisfied for lock type %s", resource, type));
        resource.register(this);
        final boolean ready = accept.isOK();
        synchronized (this) {
            if (!ready) nbUnsatisfied++;
            dependencies.put(new String(resource.getIdentifier()), new Dependency(type, ready));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        for (String id : dependencies.keySet()) {
            scheduler.getResource(id).unregister(this);
        }
    }

    /**
	 * Task priority - the higher, the better
	 * 
	 * @param priority
	 *            the priority to set
	 */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
	 * @return the priority
	 */
    public int getPriority() {
        return priority;
    }

    /**
	 * This is where the real job gets done
	 * 
	 * @param locks
	 *            The set of locks that were taken
	 * 
	 * @return The error code (0 if everything went fine)
	 * @throws Throwable
	 */
    protected int doRun(ArrayList<Lock> locks) throws Throwable {
        return 1;
    }

    public final void run() {
        File doneFile = new File(identifier + ".done");
        if (doneFile.exists()) {
            LOGGER.info("Task %s is already done", identifier);
            return;
        }
        ArrayList<Lock> locks = new ArrayList<Lock>();
        try {
            while (true) {
                if (doneFile.exists()) {
                    LOGGER.info("Task %s is already done", identifier);
                    lockfile.delete();
                    return;
                }
                try {
                    locks.add(new FileLock(lockfile, true));
                } catch (UnlockableException e) {
                    LOGGER.info("Could not lock job [%s]", identifier);
                    synchronized (this) {
                        try {
                            wait(5000);
                        } catch (InterruptedException ee) {
                        }
                    }
                    continue;
                }
                if (doneFile.exists()) {
                    LOGGER.info("Task %s is already done", identifier);
                    lockfile.delete();
                    return;
                }
                int pid = PID.getPID();
                synchronized (Scheduler.LockSync) {
                    for (Entry<String, Dependency> dependency : dependencies.entrySet()) {
                        String id = dependency.getKey();
                        Resource rsrc = scheduler.getResource(id);
                        final Lock lock = rsrc.lock(pid, dependency.getValue().type);
                        if (lock != null) locks.add(lock);
                    }
                }
                LOGGER.info("Running task %s", identifier);
                try {
                    state = ResourceState.RUNNING;
                    startTimestamp = System.currentTimeMillis();
                    updateDb();
                    int code = doRun(locks);
                    if (code != 0) throw new RuntimeException(String.format("Error while running the task (code %d)", code));
                    doneFile.createNewFile();
                    state = ResourceState.DONE;
                    LOGGER.info("Done");
                } catch (Throwable e) {
                    LOGGER.warn(format("Error while running: %s", this), e);
                    state = ResourceState.ERROR;
                } finally {
                    updateDb();
                    endTimestamp = System.currentTimeMillis();
                    notifyListeners();
                }
                break;
            }
        } catch (UnlockableException e) {
            throw new RuntimeException(e);
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        } finally {
            LOGGER.info("Dispose of locks for %s", this);
            for (Lock lock : locks) lock.dispose();
        }
    }

    /**
	 * Called when a resource status has changed
	 * 
	 * @param resource
	 *            The resource has changed (or null if itself)
	 * @param objects
	 *            Optional parameters
	 */
    public synchronized void notify(Resource resource, Object... objects) {
        if (resource == null) return;
        Dependency status = dependencies.get(resource.getIdentifier());
        int k = resource.accept(status.type).isOK() ? 1 : 0;
        final int diff = (status.isSatisfied ? 1 : 0) - k;
        LOGGER.info("[%s] Got a notification from %s [%d with %s/%d]", this, resource, k, status.type, diff);
        if (resource.getState() == ResourceState.ERROR || resource.getState() == ResourceState.ON_HOLD) {
            if (state != ResourceState.ON_HOLD) {
                state = ResourceState.ON_HOLD;
                scheduler.updateState(this);
            }
        }
        nbUnsatisfied += diff;
        if (k == 1) status.isSatisfied = true;
        if (diff != 0) scheduler.updateState(this);
    }

    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    static final DateFormat longDateFormat = DateFormat.getDateTimeInstance();

    @Override
    public void printHTML(PrintWriter out, PrintConfig config) {
        super.printHTML(out, config);
        out.format("<div><b>Lock</b>: %s</div>", isLocked() ? "Locked" : "Not locked");
        out.format("<div>%d writer(s) and %d reader(s)</div>", getReaders(), getWriters());
        if (getState() == ResourceState.DONE || getState() == ResourceState.ERROR || getState() == ResourceState.RUNNING) {
            long start = getStartTimestamp();
            long end = getState() == ResourceState.RUNNING ? System.currentTimeMillis() : getEndTimestamp();
            out.format("<div>Started: %s</div>", longDateFormat.format(new Date(start)));
            if (getState() != ResourceState.RUNNING) out.format("<div>Ended: %s</div>", longDateFormat.format(new Date(end)));
            out.format("<div>Duration: %s</div>", Time.formatTimeInMilliseconds(end - start));
        }
        TreeMap<String, Dependency> dependencies = getDependencies();
        if (!dependencies.isEmpty()) {
            out.format("<h2>Dependencies</h2><ul>");
            out.format("<div>%d unsatisfied dependencie(s)</div>", nbUnsatisfied);
            for (Entry<String, Dependency> entry : dependencies.entrySet()) {
                String dependency = entry.getKey();
                Dependency status = entry.getValue();
                Resource resource = null;
                try {
                    resource = scheduler.getResource(entry.getKey());
                } catch (DatabaseException e) {
                }
                out.format("<li><a href=\"%s/resource?id=%s\">%s</a>: %s [%b]</li>", config.detailURL, XPMServlet.urlEncode(dependency), dependency, status.getType(), resource == null ? false : resource.accept(status.type).isOK());
            }
            out.println("</ul>");
        }
    }
}
