package ade;

import java.io.*;
import java.nio.channels.*;
import java.util.HashMap;

/**
A mechanism for system-wide locking of resources on a single host. 
Operates using a {@link java.nio.channels.FileLock FileLock}
object (for restricting access of separate processes by locking a
{@link java.io.File File} object) and an <tt>int</tt> (for restricting
access of multiple threads within one Java virtual machine). The name of
the lock is specified by the <tt>lockname</tt> parameter.
<p>
Because the lock depends on a particular file, it is assumed that programs
using the lock use a common directory and lock name. The default directory
uses the <tt>java.io.tmpdir</tt> system property, although a different
directory may be specified by supplying an {@link ade.ADEHostInfo ADEHostInfo}
object to the constructor (which then uses the <tt>scratch</tt> field).
<p>
Note that, according to the Java documentation, operation is operating
system dependent.

@author Jim Kramer
@see java.nio.channels.FileLock
*/
public final class ADELock {

    private static boolean debug = false;

    private File file;

    private RandomAccessFile raf;

    private FileChannel chan;

    private FileLock lock;

    private static String propid = "adelock.jvmid";

    private String name;

    private String jvmid;

    private static HashMap<String, ADELock> runtimes = new HashMap<String, ADELock>();

    private static HashMap<String, String> holders = new HashMap<String, String>();

    private Boolean lockGuard = new Boolean(false);

    /** Construct a system lock that relies on the <tt>process.id</tt>
	 * and <tt>java.io.tmpdir</tt> system properties (for within-a-JVM
	 * and interprocess validity, respectively).
	 * @param name The name of the lock */
    public ADELock(String name) throws IOException {
        String lockid = System.getProperty(propid);
        try {
            checkPropid(lockid);
            jvmid = lockid;
        } catch (IOException ioe) {
            String pid = System.getProperty("process.id");
            if (pid != null && !pid.equals("")) {
                jvmid = pid;
            } else {
                jvmid = "pid-" + System.currentTimeMillis();
            }
        }
        setup(jvmid, name);
    }

    /** Construct a system lock that relies on the <tt>java.io.tmpdir</tt>
	 * system properties for interprocess validity, but leaves within-JVM
	 * validity of multiply instantiated locks dependent on external
	 * coordination of the JVM identifier.
	 * @param jvmid A unique identifier for the Java VM
	 * @param name The name of the lock */
    public ADELock(String jvmid, String name) throws IOException {
        checkPropid(jvmid);
        setup(jvmid, name);
    }

    /** Construct a system lock that relies on the <tt>process.id</tt>
	 * and uses the <tt>scratch</tt> field of the specified
	 * <tt>ADEHostInfo</tt> object for interprocess validity.
	 * @param ahi Information about the host
	 * @param name The name of the lock */
    public ADELock(ADEHostInfo ahi, String name) throws IOException {
        String lockid = System.getProperty(propid);
        try {
            checkPropid(lockid);
            jvmid = lockid;
        } catch (IOException ioe) {
            String pid = System.getProperty("process.id");
            if (pid != null && !pid.equals("")) {
                jvmid = pid;
            } else {
                jvmid = "pid-" + System.currentTimeMillis();
            }
        }
        StringBuilder sb = new StringBuilder(ahi.scratch);
        if (!ahi.scratch.endsWith(ahi.filesep)) sb.append(ahi.filesep);
        sb.append(name);
        setup(jvmid, sb.toString());
    }

    /** Construct a system lock that uses the <tt>scratch</tt> field of the
	 * specified <tt>ADEHostInfo</tt> object for interprocess validity and
	 * leaves within-JVM validity of multiply instantiated locks dependent
	 * on external coordination of the JVM identifier.
	 * @param jvmid A unique identifier for the Java VM
	 * @param ahi Information about the host
	 * @param name The name of the lock */
    public ADELock(String jvmid, ADEHostInfo ahi, String name) throws IOException {
        checkPropid(jvmid);
        StringBuilder sb = new StringBuilder(ahi.scratch);
        if (!ahi.scratch.endsWith(ahi.filesep)) sb.append(ahi.filesep);
        sb.append(name);
        setup(jvmid, sb.toString());
    }

    private void checkPropid(String id) throws IOException {
        if (id == null) throw new IOException("Property " + propid + " null; supply it");
        if (id.equals("")) throw new IOException("Property " + propid + " empty; set it");
        String lockid = System.getProperty(propid);
        if (lockid != null && !lockid.equals("")) if (!lockid.equals(id)) throw new IOException("Property " + propid + " already set");
        jvmid = id;
    }

    private void setup(String id, String name) throws IOException {
        file = new File(name);
        file.createNewFile();
        this.name = name;
        try {
            raf = new RandomAccessFile(file, "rw");
            chan = raf.getChannel();
        } catch (FileNotFoundException fnfe) {
        }
        System.setProperty(propid, this.name);
    }

    /** Attempt to acquire the lock. Note that this method does not block
	 * (no blocking method is available), and repeated calls after acquiring
	 * the lock will return <tt>false</tt>.
	 * @return <tt>true</tt> if acquired, <tt>false</tt> otherwise */
    public final boolean lock() {
        boolean gotLock = false;
        synchronized (lockGuard) {
            try {
                if (gotLock = ((lock = chan.tryLock()) != null)) {
                    if (!runtimes.containsKey(name)) {
                        if (debug) System.out.println(jvmid + ": LOCKING " + name + " at " + System.currentTimeMillis());
                        runtimes.put(name, this);
                        holders.put(name, jvmid);
                        gotLock = true;
                    }
                }
            } catch (Exception e) {
            }
        }
        return gotLock;
    }

    /** Release the lock.
	 * @return <tt>true</tt> if successful, <tt>false</tt> otherwise */
    public final boolean unlock() {
        boolean unlocked = false;
        synchronized (lockGuard) {
            if (runtimes.containsKey(name)) {
                try {
                    lock.release();
                    synchronized (runtimes) {
                        runtimes.remove(name);
                    }
                    synchronized (holders) {
                        holders.remove(name);
                    }
                    lock = null;
                    if (debug) System.out.println(jvmid + ": UNLOCKING " + name + " at " + System.currentTimeMillis());
                    unlocked = true;
                } catch (Exception e) {
                }
            } else {
                unlocked = true;
            }
        }
        return unlocked;
    }

    public String getKey() {
        return holders.get(name);
    }

    public String getHolder() {
        return holders.get(name);
    }
}
