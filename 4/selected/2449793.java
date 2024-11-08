package fr.x9c.cadmium.kernel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import fr.x9c.cadmium.primitives.cadmium.CustomObject;
import fr.x9c.cadmium.util.CustomClassLoader;
import fr.x9c.cadmium.util.Signal;

/**
 * This class encapsulates the state of a code runner.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.3
 * @since 1.0
 */
public final class Context {

    /** Filename placeholder when none is specified. */
    private static final String NO_FILE = "";

    /** OCaml value for 'default' signal handler. */
    private static final Value SIGNAL_DEFAULT_HANDLER = Value.ZERO;

    /** OCaml value for 'ignore' signal handler. */
    private static final Value SIGNAL_IGNORE_HANDLER = Value.ONE;

    /** Library name for internal Cadmium elements. */
    private static final String DUMMY_LIB_NAME = "[Cadmium-Internal]";

    /** When to recompile a compiled dispatcher. */
    private static final int RECOMPILE_THRESHOLD = 8;

    /** Sigmask command: replace current mask by passed one. */
    private static final int SIG_SETMASK = 0;

    /** Sigmask command: new mask is union of current and passed ones. */
    private static final int SIG_BLOCK = 1;

    /**
     * Sigmask command: new mask is intersection of current one and complement
     * of passed one.
     */
    private static final int SIG_UNBLOCK = 2;

    /** Number of atoms. */
    private static final int NB_ATOMS = 256;

    /** Parameters (shared but immutable). */
    private final AbstractParameters params;

    /** Whether the associated code runner is native. */
    private final boolean isNative;

    /** Current working directory. */
    private File pwd;

    /** File hook. */
    private FileHook fileHook;

    /** Filename of interpreted bytecode. */
    private final String file;

    /** Main code runner of the program. */
    private CodeRunner mainCodeRunner;

    /** Main thread of the program. */
    private CadmiumThread mainThread;

    /*** Program thread group. */
    private final ThreadGroup threadGroup;

    /** Additional threads (non-cadmium threads calling callbacks). */
    private final Set<Thread> additThreads;

    /**
     * Atoms - they are not shared across code runner to prevent an illegal
     * bytecode to corrupt all-runners instances.
     */
    private final Value[] atoms;

    /** Code to execute (can be modified by dynamic linking). */
    private int[] code;

    /** Original size of code (<i>i.e.</i> after first append). */
    private int originalCodeSize;

    /** Saved code (used by debugger). */
    private int[] savedCode;

    /** Offset of callback tail in code. */
    private int callbackTail;

    /** Digest of (original) code. */
    private byte[] codeDigest;

    /** Map of custom types, from identifiers to implementations. */
    private final Map<String, Custom.Operations> customs;

    /** Slots - used by librairies to register some state information. */
    private final Map<Object, Object> slots;

    /** Timestamp of instance creation - to be used as program start time. */
    private final long start;

    /** Channels map, from file descriptor to actual channel instance. */
    private final Map<Integer, Channel> channels;

    /** Callbacks map, from identifier to value. */
    private final Map<String, Value> callbacks;

    /** Lock used for access to {@link #runningFinalizer}. */
    private final Object finalizerLock;

    /** Whether finalizer code is currently running. */
    private boolean runningFinalizer;

    /**
     * Lock used for access to {@link #runtimeBusy}. <br/>
     * Non-<tt>null</tt> iff threads have been initialized.
     */
    private Object runtimeLock;

    /**
     * Whether runtime is currently running:
     * <ul>
     *   <li>if <tt>null</tt>: not busy;</li>
     *   <li>if not <tt>null</tt>: busy (field contains lock owner).</li>
     * </ul>
     * The value is always up-to-date, whether threads have been initialized
     * or not.
     */
    private Thread runtimeBusy;

    /** Global data (can be modified by 'caml_realloc_global'). */
    private Value globalData;

    /** Debug info (used for exception backtrace). */
    private Value debugInfo;

    /** Whether backtrace is active. */
    private boolean backtraceActive;

    /** Method/primitive dispatcher. */
    private Dispatcher dispatcher;

    /** Signals map, from system identifiers to handlers. */
    private final Map<Integer, Value> signals;

    /** Sets of blocked signals. */
    private final Set<Integer> blockedSignals;

    /** Sets of ignored signals. */
    private final Set<Integer> ignoredSignals;

    /** Asynchronous exception raised by a signal handler. */
    private Exception asyncException;

    /**
     * Opened libraires, as a map from names to list of actual class objects.
     */
    private final Map<String, List<Class>> libraries;

    /** GC parameter - stored and returned but not used to control GC. */
    private int gcMinorHeapSize;

    /** GC parameter - stored and returned but not used to control GC. */
    private int gcMajorHeapIncrement;

    /** GC parameter - stored and returned but not used to control GC. */
    private int gcSpaceOverhead;

    /** GC parameter - stored and returned but not used to control GC. */
    private int gcVerbose;

    /** GC parameter - stored and returned but not used to control GC. */
    private int gcMaxOverhead;

    /** GC parameter - stored and returned but not used to control GC. */
    private int gcStackLimit;

    /** GC counter. */
    private int gcMinorCounter;

    /** GC counter. */
    private int gcMajorCounter;

    /** GC counter. */
    private int gcCompactionCounter;

    /** GC allocation policy. */
    private int gcAllocationPolicy;

    /** Whether parser trace is enabled. */
    private boolean parserTrace;

    /** Debugger event count (number of events remaining to execute). */
    private long debuggerEventCount;

    /** Whether debugger is currently in use. */
    private boolean debuggerInUse;

    /** Socket used to communicate with the debugger. */
    private Socket debuggerSocket;

    /** Trap barrier used by the debugger. */
    private int debuggerTrapBarrier;

    /**
     * Constructs a context, with the following values:
     * <ul>
     *   <li>code is empty;</li>
     *   <li>code digest is <tt>null</tt>;</li>
     *   <li>main thread and code runner are set to <tt>null</tt>;</li>
     *   <li>customs types are <i>int32</i>, <i>int64</i>,
     *       <i>nativeint</i>, and <i>Cadmium.java_object</i>;</li>
     *   <li>start is initialized to current time;</li>
     *   <li>channels are created for standard in, out, and error;</li>
     *   <li>debug informations are set to <tt>false</tt>;</li>
     *   <li>finalizers and runtime lock are free;</li>
     *   <li>global data and dispatcher are set to <tt>null</tt>;</li>
     *   <li>callback tail offset is set to <tt>-1</tt>;</li>
     *   <li>no slot, neither callback is registered;</li>
     *   <li>asynchronous exception is set to <tt>null</tt>;</li>
     *   <li>gc parameters are set to dummy values;</li>
     *   <li>gc counters are set to zero;</li>
     *   <li>gc allocation policy is set to zero;</li>
     *   <li>parser trace is disabled;</li>
     *   <li>debugger event count is set to zero;</li>
     *   <li>debugger is not in use;</li>
     *   <li>debugger socket is <tt>null</tt>;</li>
     *   <li>debugger trap barrier is set to zero.</li>
     * </ul>
     * @param p parameters - should not be <tt>null</tt>
     * @param isNative whether the underlying code runner is native
     * @param dir working directory - should not be <tt>null</tt>
     */
    Context(final AbstractParameters p, final boolean isNative, final File dir) {
        assert p != null : "null p";
        assert dir != null : "null dir";
        this.params = p;
        this.isNative = isNative;
        this.pwd = dir;
        Class embBase = null;
        try {
            embBase = Class.forName(p.getEmbeddedBase());
        } catch (final Throwable t) {
        }
        this.fileHook = p.isEmbedded() ? new BasicFileHook(embBase) : null;
        this.file = p.getFile() != null ? p.getFile() : Context.NO_FILE;
        this.mainCodeRunner = null;
        this.mainThread = null;
        this.threadGroup = new ThreadGroup(CadmiumThread.getNextThreadGroupName(isNative));
        this.additThreads = new HashSet<Thread>();
        this.atoms = new Value[Context.NB_ATOMS];
        for (int i = 0; i < Context.NB_ATOMS; i++) {
            this.atoms[i] = Value.createFromBlock(Block.createAtom(i));
        }
        this.code = new int[0];
        this.savedCode = null;
        this.codeDigest = null;
        this.customs = new HashMap<String, Custom.Operations>();
        registerCustom(Custom.INT_32_OPS);
        registerCustom(Custom.INT_NAT_OPS);
        registerCustom(Custom.INT_64_OPS);
        registerCustom(CustomObject.OPS);
        this.slots = new HashMap<Object, Object>();
        this.start = System.currentTimeMillis();
        this.channels = new HashMap<Integer, Channel>();
        this.channels.put(Channel.STDIN, new Channel(p.getStandardInput()));
        this.channels.put(Channel.STDOUT, new Channel(p.getStandardOutput()));
        this.channels.put(Channel.STDERR, new Channel(p.getStandardError()));
        this.callbacks = new HashMap<String, Value>();
        this.callbackTail = -1;
        this.finalizerLock = new Object();
        this.runningFinalizer = false;
        this.runtimeLock = null;
        this.runtimeBusy = null;
        this.globalData = null;
        this.debugInfo = Value.FALSE;
        this.backtraceActive = p.isBacktraceRequested();
        this.dispatcher = null;
        this.signals = new HashMap<Integer, Value>();
        this.blockedSignals = new HashSet<Integer>();
        this.ignoredSignals = new HashSet<Integer>();
        this.asyncException = null;
        this.libraries = new LinkedHashMap<String, List<Class>>();
        this.libraries.put(Context.DUMMY_LIB_NAME, null);
        this.gcMinorHeapSize = 32 * 1024;
        this.gcMajorHeapIncrement = 62 * 1024;
        this.gcSpaceOverhead = 80;
        this.gcVerbose = 0;
        this.gcMaxOverhead = 500;
        this.gcStackLimit = 256 * 1024;
        this.gcMinorCounter = 0;
        this.gcMajorCounter = 0;
        this.gcCompactionCounter = 0;
        this.gcAllocationPolicy = 0;
        this.parserTrace = false;
        this.debuggerEventCount = 0L;
        this.debuggerInUse = false;
        this.debuggerSocket = null;
        this.debuggerTrapBarrier = 0;
    }

    /**
     * Returns the parameters.
     * @return the parameters
     */
    public AbstractParameters getParameters() {
        return this.params;
    }

    /**
     * Tests whether the associated code runner is native.
     * @return <tt>true</tt> if the associated code runner is native,
     *         <tt>false</tt> otherwise
     */
    public boolean isNative() {
        return this.isNative;
    }

    /**
     * Returns the current working directory.
     * @return the current working directory
     */
    public File getPwd() {
        return this.pwd;
    }

    /**
     * Changes the current working directory.
     * @param path new working directory - should not be <tt>null</tt>
     */
    public void setPwd(final File path) {
        assert path != null : "null path";
        this.pwd = path;
    }

    /**
     * Constructs a file, relatively to context. <br/>
     * The passed value should be a string representing a path and is
     * interpreted relatively to current working directory.
     * @param name file path
     * @return a file object corresponding to the passed path
     */
    public File getRealFile(final String name) {
        final File f = new File(name);
        if (f.isAbsolute()) {
            return f;
        } else {
            return new File(getPwd(), name);
        }
    }

    /**
     * Constructs a file, relatively to context. <br/>
     * The passed value should be a string representing a path and is
     * interpreted relatively to current working directory.
     * @param name file path
     * @return a file object corresponding to the passed path
     */
    public File getRealFile(final Value name) {
        return getRealFile(name.asBlock().asString());
    }

    /**
     * Translates a (possibly partial) path into a complete path.
     * @param path to translate
     * @return translated path
     */
    public String resourceNameFromPath(final Value path) {
        final String s = path.asBlock().asString();
        if (s.startsWith("/")) {
            return s;
        } else {
            final String prefix = getPwd().toString();
            if (prefix.endsWith("/")) {
                return prefix + s;
            } else {
                return prefix + "/" + s;
            }
        }
    }

    /**
     * Returns an input stream for passed path. <br/>
     * <b>File hook is used.</b>
     * @param path path of stream source
     * @return an input stream for passed path
     * @throws IOException if it is not possible to poen such a stream
     */
    public InputStream getInputStreamForPath(final Value path) throws IOException {
        if (this.fileHook != null) {
            final InputStream res = this.fileHook.getInputStream(resourceNameFromPath(path));
            if (res != null) {
                return res;
            }
        }
        return new FileInputStream(getRealFile(path));
    }

    /**
     * Returns an output stream for passed path. <br/>
     * <b>File hook is not used.</b>
     * @param path path of stream source
     * @return an output stream for passed path
     * @throws IOException if it is not possible to poen such a stream
     */
    public OutputStream getOutputStreamForPath(final Value path) throws IOException {
        return new FileOutputStream(getRealFile(path));
    }

    /**
     * Changes the file hook (<i>i. e.</i> try to load as a resource first).
     * @param hook file hook (<tt>null</tt> disable file hook)
     */
    public void setFileHook(final FileHook hook) {
        this.fileHook = hook;
    }

    /**
     * Returns the file hook.
     * @return the file hook (<tt>null</tt> means that file hook is disabled)
     */
    public FileHook getFileHook() {
        return this.fileHook;
    }

    /**
     * Returns the filename of the interpreted bytecode.
     * @return the filename of the interpreted bytecode
     */
    public String getFile() {
        return this.file;
    }

    /**
     * Returns the main code runner of the program.
     * @return the main code runner of the program
     */
    public CodeRunner getMainCodeRunner() {
        return this.mainCodeRunner;
    }

    /**
     * Changes the main code runner of the program.
     * @param cr new main code runner - should not be <tt>null</tt>
     */
    public void setMainCodeRunner(final CodeRunner cr) {
        assert cr != null : "null cr";
        this.mainCodeRunner = cr;
    }

    /**
     * Returns the main thread of the program.
     * @return the main thread of the program
     */
    public CadmiumThread getMainThread() {
        return this.mainThread;
    }

    /**
     * Changes the main thread of the program.
     * @param ct new thread - should not be <tt>null</tt>
     */
    public void setMainThread(final CadmiumThread ct) {
        assert ct != null : "null ct";
        this.mainThread = ct;
    }

    /**
     * Returns the program thread group. <br/>
     * This thread group contains all the threads used by this program.
     * @return the program thread group
     */
    public ThreadGroup getThreadGroup() {
        return this.threadGroup;
    }

    /**
     * Adds a thread to the set of additional threads.
     * @param t thread to add to set - should not be <tt>null</tt>
     */
    public void addAdditionalThread(final Thread t) {
        assert t != null : "null t";
        synchronized (this.additThreads) {
            this.additThreads.add(t);
        }
    }

    /**
     * Removes a thread from the set of additional threads.
     * @param t thread to remove from set - should not be <tt>null</tt>
     */
    public void removeAdditionalThread(final Thread t) {
        assert t != null : "null t";
        synchronized (this.additThreads) {
            this.additThreads.remove(t);
        }
    }

    /**
     * Interrupts all the threads in the set of additional threads. <br/>
     * The set is then emptied.
     */
    public void interruptAdditionalThreads() {
        synchronized (this.additThreads) {
            for (Thread t : this.additThreads) {
                t.interrupt();
            }
        }
    }

    /**
     * Returns the atom of a given order.
     * @param atm order of the atom to return
     *            - should be in 0 .. {@link #NB_ATOMS} - 1
     * @return the atom of given order
     */
    public Value getAtom(final int atm) {
        assert (atm >= 0) && (atm < Context.NB_ATOMS) : "invalid atm";
        return this.atoms[atm];
    }

    /**
     * Tests whether a given value is an atom in the intrepreter relative to
     * this context.
     * @param val value to tests for "atomicity" - should not be <tt>null</tt>
     * @return <tt>true</tt> if the value is an atom of the code runner,
     *         <tt>false</tt> otherwise
     */
    public boolean isAtom(final Value val) {
        assert val != null : "null val";
        if (val.isBlock()) {
            final Block bl = val.asBlock();
            int i = 0;
            while ((i < Context.NB_ATOMS) && (this.atoms[i].asBlock() != bl)) {
                i++;
            }
            return i < Context.NB_ATOMS;
        } else {
            return false;
        }
    }

    /**
     * Returns the code to be interpreted.
     * @return the code to be interpreted
     */
    int[] getCode() {
        return this.code;
    }

    /**
     * Appends additional bytecode to current code.
     * @param additCode additional code to append - should not be <tt>null</tt>
     * @return offset of added code, that is code length before addition
     */
    public int appendCode(final int[] additCode) {
        assert additCode != null : "null additCode";
        final int addLen = additCode.length;
        final int oldLen = this.code.length;
        final int newLen = oldLen + addLen;
        final int[] newCode = new int[newLen];
        System.arraycopy(this.code, 0, newCode, 0, oldLen);
        System.arraycopy(additCode, 0, newCode, oldLen, addLen);
        this.code = newCode;
        if (oldLen == 0) {
            this.originalCodeSize = newLen;
        }
        if (this.debuggerInUse && (oldLen == 0)) {
            this.savedCode = new int[newLen];
            System.arraycopy(this.code, 0, this.savedCode, 0, newLen);
        }
        return oldLen;
    }

    /**
     * Returns the original code size.
     * @return the original code size
     */
    public int getOriginalCodeSize() {
        return this.originalCodeSize;
    }

    /**
     * Returns the saved code.
     * @return the saved code
     */
    public int[] getSavedCode() {
        return this.savedCode;
    }

    /**
     * Adds callback tail at the end of code. <br/>
     * Offset of callback tail is set.
     */
    public void setupCallbackTail() {
        this.callbackTail = appendCode(new int[] { Instructions.POP, 1, Instructions.STOP });
    }

    /**
     * Returns the offset of callback tail in code.
     * @return the offset of callback tail in code
     */
    public int getCallbackTail() {
        return this.callbackTail;
    }

    /**
     * Changes code digest.
     * @param digest new code digest - should not be <tt>null</tt>
     */
    void setCodeDigest(final byte[] digest) {
        assert digest != null : "null digest";
        this.codeDigest = Arrays.copyOf(digest, digest.length);
    }

    /**
     * Returns (a copy of) the code digest.
     * @return (a copy of) the code digest
     */
    byte[] getCodeDigest() {
        return Arrays.copyOf(this.codeDigest, this.codeDigest.length);
    }

    /**
     * Registers a custom type. <br/>
     * If a custom type has already been registered with the same identifier,
     * it is replaced.
     * @param ops custom type description - should not be <tt>null</tt>
     */
    public void registerCustom(final Custom.Operations ops) {
        assert ops != null : "null ops";
        this.customs.put(ops.getIdentifier(), ops);
    }

    /**
     * Looks for for a custom type.
     * @param id identifier of type to look for - should not be <tt>null</tt>
     * @return the instance describing the custom type if any,
     *         <tt>null</tt> otherwise
     */
    Custom.Operations findCustom(final String id) {
        assert id != null : "null id";
        return this.customs.get(id);
    }

    /**
     * Registers a value with a slot.
     * @param slot slot identifier - should not be <tt>null</tt>
     * @param value slot data (can be <tt>null</tt>)
     */
    public void registerSlot(final Object slot, final Object value) {
        assert slot != null : "null slot";
        this.slots.put(slot, value);
    }

    /**
     * Returns the value associated with a given slot.
     * @param slot slot identifier - should not be <tt>null</tt>
     * @return the value associated with the slot if any (can be <tt>null</tt>),
     *         <tt>null</tt> otherwise
     */
    public Object getSlot(final Object slot) {
        assert slot != null : "null slot";
        return this.slots.get(slot);
    }

    /**
     * Unregisters a slot.
     * @param slot slot identifier - should not be <tt>null</tt>
     */
    public void removeSlot(final Object slot) {
        assert slot != null : "null slot";
        this.slots.remove(slot);
    }

    /**
     * Returns the timestamp of instance creation, in milliseconds. <br/>
     * Represents program start time.
     * @return the timestamp of instance creation, in milliseconds
     */
    public long getStart() {
        return this.start;
    }

    /**
     * Returns the channel associated with a given file descriptor.
     * @param fd file descriptor
     * @return the channel associated with a given file descriptor if any,
     *         <tt>null</tt> otherwise
     */
    public Channel getChannel(final int fd) {
        return this.channels.get(fd);
    }

    /**
     * Adds a channel. <br/>
     * The file descriptor of the passed channel is also set.
     * @param ch channel to be added - should not be <tt>null</tt>
     * @return the file descriptor associated with the channel, <br/>
     *         that is the lowest free file descriptor
     */
    public int addChannel(final Channel ch) {
        assert ch != null : "null ch";
        int fd = 0;
        while (this.channels.containsKey(fd)) {
            fd++;
        }
        this.channels.put(fd, ch);
        ch.setFD(fd);
        return fd;
    }

    /**
     * Removes a channel. <br/>
     * The removed channel is not modified in any way.
     * @param fd file descriptor of channel to remove
     * @return removed channel if any,
     *         <tt>null</tt> otherwise
     */
    public Channel removeChannel(final int fd) {
        return this.channels.remove(fd);
    }

    /**
     * Removes and closes a channel. <br/>
     * The file descriptor of the passed channel is also set to <tt>-1</tt>.
     * @param fd file descriptor of channel to remove and close
     * @return whether a channel was actually closed
     * @throws IOException if an error occurs while trying to close the channel
     */
    public boolean closeChannel(final int fd) throws IOException {
        final Channel ch = this.channels.remove(fd);
        if (ch != null) {
            ch.setFD(-1);
            ch.close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets a channel. <br/>
     * The file descriptor of the passed channel is also set.
     * @param fd file descriptor for channel
     * @param ch channel - should not be <tt>null</tt>
     */
    public void setChannel(final int fd, final Channel ch) {
        assert ch != null : "null ch";
        this.channels.put(fd, ch);
        ch.setFD(fd);
    }

    /**
     * Constructs and returns a list of all opened output channels.
     * @return a list of all opened output channels
     */
    public Value makeOutChannelsList() {
        Value res = Value.EMPTY_LIST;
        final Iterator<Channel> it = this.channels.values().iterator();
        while (it.hasNext()) {
            final Channel ch = it.next();
            if (ch.asDataOutput() != null) {
                final Block b = Block.createCustom(Custom.CHANNEL_SIZE, Custom.CHANNEL_OPS);
                b.setCustom(ch);
                final Block cons = Block.createBlock(Block.TAG_CONS, Value.createFromBlock(b), res);
                res = Value.createFromBlock(cons);
            }
        }
        return res;
    }

    /**
     * Registers a callback.
     * @param s callback identifier - should not be <tt>null</tt>
     * @param v callback value - should not be <tt>null</tt>
     */
    public void registerCallback(final String s, final Value v) {
        assert s != null : "null s";
        assert v != null : "null v";
        this.callbacks.put(s, v);
    }

    /**
     * Returns a callback.
     * @param s callback identifier - should not be <tt>null</tt>
     * @return value associated with passed name if it exists,
     *         <tt>null</tt> otherwise
     */
    public Value getCallback(final String s) {
        assert s != null : "null s";
        return this.callbacks.get(s);
    }

    /**
     * Acquires the finalizer lock, indicating that some finalizer
     * code will run.
     * @throws FalseExit if another thread has exited the program
     * @throws Fail.Exception if an asynchronous exception should be thrown
     */
    public void acquireFinalizeLock() throws FalseExit, Fail.Exception {
        synchronized (this.finalizerLock) {
            while (this.runningFinalizer) {
                try {
                    this.finalizerLock.wait();
                } catch (final InterruptedException ie) {
                    final FalseExit fe = FalseExit.createFromContext(this);
                    fe.fillInStackTrace();
                    throw fe;
                }
            }
            this.runningFinalizer = true;
        }
    }

    /**
     * Releases the finalizer lock, indicating that no finalizer
     * code is running.
     */
    public void releaseFinalizeLock() {
        synchronized (this.finalizerLock) {
            this.runningFinalizer = false;
            this.finalizerLock.notifyAll();
        }
    }

    /**
     * Tests whether some finalizer code is running.
     * @return <tt>true</tt> if some finalizer code is running,
     *         <tt>false</tt> otherwise
     */
    public boolean isFinalizeLockHeld() {
        return this.runningFinalizer;
    }

    /**
     * Creates runtime lock, as initiliaztion for multi-thread library.
     */
    public void createRuntimeLock() {
        this.runtimeLock = new Object();
    }

    /**
     * Releases runtime lock.
     */
    public void enterBlockingSection() {
        if (this.runtimeLock != null) {
            synchronized (this.runtimeLock) {
                this.runtimeBusy = null;
                this.runtimeLock.notifyAll();
            }
        } else {
            this.runtimeBusy = null;
        }
    }

    /**
     * Acquires runtime lock.
     */
    public void leaveBlockingSection() throws FalseExit, Fail.Exception {
        if (this.runtimeLock != null) {
            synchronized (this.runtimeLock) {
                while (this.runtimeBusy != null) {
                    try {
                        this.runtimeLock.wait();
                    } catch (final InterruptedException ie) {
                        final FalseExit fe = FalseExit.createFromContext(this);
                        fe.fillInStackTrace();
                        throw fe;
                    }
                }
                this.runtimeBusy = Thread.currentThread();
            }
        } else {
            this.runtimeBusy = Thread.currentThread();
        }
    }

    /**
     * Tries to acquire the runtime lock. <br/>
     * <b>THIS IMPLEMENTATION IS EMPTY AND ALWAYS RETURNS <tt>false</tt></b>
     * @return <tt>true</tt> if the runtime lock has been acquired,
     *         <tt>false</tt> otherwise
     */
    public boolean tryLeaveBlockingSection() {
        return false;
    }

    /**
     * Tests whether the runtime is currently running.
     * @return <tt>true</tt> if the runtime is currently running,
     *         <tt>false</tt> otherwise
     */
    public boolean isRuntimeBusy() {
        return this.runtimeBusy != null;
    }

    /**
     * Returns the global data.
     * @return the global data
     */
    public Value getGlobalData() {
        return this.globalData;
    }

    /**
     * Changes the global data.
     * @param gd new global data - should not be <tt>null</tt>
     */
    public void setGlobalData(final Value gd) {
        assert gd != null : "null gd";
        this.globalData = gd;
    }

    /**
     * Resizes the global data. <br/>
     * Global data is resized if and only if requested size is greater than
     * current one.
     * @param sz new size for global data
     */
    public void resizeGlobalData(final int sz) {
        final Block global = getGlobalData().asBlock();
        final int len = global.sizeValues();
        if (sz >= len) {
            final Block bl = Block.createBlock((sz + 0x100) & 0xFFFFFF00, 0);
            for (int i = 0; i < len; i++) {
                bl.set(i, global.get(i));
            }
            setGlobalData(Value.createFromBlock(bl));
        }
    }

    /**
     * Changes debug information.
     * @param di debug information - should not be <tt>null</tt>
     */
    void setDebugInfo(final Value di) {
        assert di != null : "null di";
        this.debugInfo = di;
    }

    /**
     * Returns the debug information.
     * @return the debug information
     */
    Value getDebugInfo() {
        return this.debugInfo;
    }

    /**
     * Tests whether backtrace is active.
     * @return <tt>true</tt> if backtrace is active,
     *         <tt>false</tt> otherwise
     */
    public boolean isBacktraceActive() {
        return this.backtraceActive;
    }

    /**
     * Sets backtrace state.
     * @param b whether backtrace should be active
     */
    public void setBacktraceActive(final boolean b) {
        this.backtraceActive = b;
    }

    /**
     * Sets the primitive dispatcher.
     * @param d primitive dispatcher - should not be <tt>null</tt>
     */
    void setDispatcher(final Dispatcher d) {
        assert d != null : "null d";
        this.dispatcher = d;
    }

    /**
     * Returns the primitive dispatcher.
     * @return the primitive dispatcher
     */
    public Dispatcher getDispatcher() {
        return this.dispatcher;
    }

    /**
     * Returns the set of blocked signals, in system representation.
     * @return the set of blocked signals
     */
    Set<Integer> getBlockedSignals() {
        return this.blockedSignals;
    }

    /**
     * Returns the set of ignored signals, in system representation.
     * @return the set of ignored signals
     */
    Set<Integer> getIgnoredSignals() {
        return this.ignoredSignals;
    }

    /**
     * Clears all signal information. <br/>
     * Removes all signal handlers and clears blocked and ignored sets.
     */
    void clearSignals() {
        Signals.unregisterContext(this);
        this.signals.clear();
        this.blockedSignals.clear();
        this.ignoredSignals.clear();
    }

    /**
     * Returns the signal handler for a given signal.
     * @param signum signal number, in system representation
     * @return handler associated with passed signal
     */
    public Value getSignalHandler(final int signum) {
        final Value res = this.signals.get(signum);
        return res != null ? res : Context.SIGNAL_DEFAULT_HANDLER;
    }

    /**
     * Returns the set of pending signals, in system representation.
     * @return the set of pending signals
     */
    public Set<Integer> getPendingSignals() {
        return Signals.getPendingSignals(this.blockedSignals);
    }

    /**
     * Blocks signals - equivalent to Unix <i>sigprocmask</i>.
     * @param action how passed set should be interpreted - should be one of:
     *               <ul>
     *                 <li>{@link #SIG_SETMASK};</li>
     *                 <li>{@link #SIG_BLOCK};</li>
     *                 <li>{@link #SIG_UNBLOCK}.</li>
     *               </ul>
     * @param s signal set, in system representation
     * @return old blocked signals set, in system representation
     */
    public Set<Integer> blockSignals(final int action, final Set<Integer> s) {
        assert s != null : "null s";
        final Set<Integer> res = new HashSet<Integer>(this.blockedSignals);
        switch(action) {
            case Context.SIG_SETMASK:
                this.blockedSignals.clear();
                this.blockedSignals.addAll(s);
                break;
            case Context.SIG_BLOCK:
                this.blockedSignals.addAll(s);
                break;
            case Context.SIG_UNBLOCK:
                this.blockedSignals.removeAll(s);
                break;
            default:
                assert false : "invalid sigmask action";
        }
        this.blockedSignals.remove((new Signal(Signal.Kind.KILL)).getNumber());
        this.blockedSignals.remove((new Signal(Signal.Kind.STOP)).getNumber());
        return res;
    }

    /**
     * Waits until a signal is caught.
     * @param s set of signals to ignore, in system representation
     *          - should not be <tt>null</tt>
     * @return caught signal, in system representation
     */
    public int waitSignal(final Set<Integer> s) throws FalseExit, Fail.Exception {
        assert s != null : "null s";
        return Signals.waitForSignal(this, s);
    }

    /**
     * Installs a signal handler.
     * @param signal signal to install handler for
     * @param action signal handler
     * @return previous signal handler
     * @throws Fail.Exception if signal is unavailable on platform
     * @throws Fail.Exception if signal is either <i>kill</i> or <i>stop</i>
     */
    public Value installSignalHandler(final Value signal, final Value action) throws Fail.Exception {
        final int signum = Signals.ocamlToSystemIdentifier(signal.asLong());
        if (signum < 0) {
            Fail.invalidArgument("Sys.signal: unavailable signal");
        }
        if ((signum == ((new Signal(Signal.Kind.KILL)).getNumber())) || (signum == ((new Signal(Signal.Kind.STOP)).getNumber()))) {
            Fail.raiseSysError("Invalid argument");
        }
        if (action == Context.SIGNAL_DEFAULT_HANDLER) {
            Signals.unregisterContext(signum, this);
            return registerSignal(signum, Context.SIGNAL_DEFAULT_HANDLER);
        } else if (action == Context.SIGNAL_IGNORE_HANDLER) {
            Signals.registerContext(signum, this);
            return registerSignal(signum, Context.SIGNAL_IGNORE_HANDLER);
        } else {
            Signals.registerContext(signum, this);
            return registerSignal(signum, action.asBlock().get(0));
        }
    }

    /**
     * Actually sets a signal handler.
     * @param signum signal to install handler for
     * @param handler signal handler
     * @return previous handler
     */
    private Value registerSignal(final int signum, final Value handler) {
        final Value res = this.signals.put(signum, handler);
        if (res == null) {
            return Context.SIGNAL_DEFAULT_HANDLER;
        } else if (res.isBlock()) {
            return Value.createFromBlock(Block.createBlock(0, res));
        } else {
            return res;
        }
    }

    /**
     * Sets the asynchronous exception, raised by a signal handler.
     * @param e asynchronous exception - should not be <tt>null</tt>
     */
    public void setAsyncException(final Exception e) {
        assert e != null : "null e";
        synchronized (this) {
            this.asyncException = e;
        }
    }

    /**
     * Returns the asynchronous exception, also setting it to <tt>null</tt>.
     * @return the asynchronous exception
     */
    public Exception getAndClearAsyncException() {
        final Exception res;
        synchronized (this) {
            res = this.asyncException;
            this.asyncException = null;
        }
        return res;
    }

    /**
     * Opens a library. <br/>
     * If the specified filename denotes a jar file, its entries are loaded and
     * will be checked during primitive lookup. If it denotes a file that is
     * not a jar one, the library is opened and it is supposed that primitives
     * will be provided by another way (<i>e.g. </i> builtin primitives).
     * If it is not a file, a try is made to consider the passed path as a
     * fully qualified class name used as a primitive provider.
     * @param filename library path - should not be <tt>null</tt>
     * @return handle of opened library as an abstract value with its 0-index
     *         value equal to library name
     * @throws Fail.Exception if library cannot be opened
     */
    public Value openLib(final Value filename) throws Fail.Exception {
        assert filename != null : "null filename";
        URL url = null;
        if (this.fileHook != null) {
            url = this.fileHook.getURL(resourceNameFromPath(filename));
        }
        if (url == null) {
            final File file = getRealFile(filename);
            if (file.exists()) {
                try {
                    url = file.toURI().toURL();
                } catch (final MalformedURLException mue) {
                }
            }
        }
        final String baseName = filename.asBlock().asString();
        String name = baseName;
        int cnt = 2;
        while (this.libraries.containsKey(name)) {
            name = baseName + cnt++;
        }
        if (url != null) {
            final List<Class> classes = new LinkedList<Class>();
            try {
                final URLClassLoader loader = new URLClassLoader(new URL[] { url }, CustomClassLoader.INSTANCE);
                final JarFile jar = new JarFile(this.file);
                final Enumeration<JarEntry> it = jar.entries();
                while (it.hasMoreElements()) {
                    final JarEntry entry = it.nextElement();
                    final String s = entry.getName();
                    if (s.endsWith(".class") && (s.indexOf('$') == -1)) {
                        final String s1 = s.substring(0, name.length() - ".class".length());
                        final String s2 = s1.replace('/', '.');
                        try {
                            classes.add(Class.forName(s2, true, loader));
                        } catch (final Throwable t) {
                        }
                    }
                }
            } catch (final Throwable t) {
            }
            this.libraries.put(name, classes);
            final Block bl = Block.createBlock(Block.ABSTRACT_TAG, Value.createFromBlock(Block.createString(name)));
            return Value.createFromBlock(bl);
        } else {
            try {
                final Class cl = Class.forName(name);
                this.libraries.put(name, Collections.singletonList(cl));
                final Block bl = Block.createBlock(Block.ABSTRACT_TAG, Value.createFromBlock(Block.createString(name)));
                return Value.createFromBlock(bl);
            } catch (final Throwable t) {
                Fail.failWith("cannot open library");
                return null;
            }
        }
    }

    /**
     * Closes a library. <br/>
     * Its primitive can still be called but cannot be looked up. <br/>
     * Nothing is done if the library has not been opened.
     * @param libname library name - should not be <tt>null</tt>
     */
    public void closeLib(final String libname) {
        assert libname != null : "null libname";
        if (!libname.equals(Context.DUMMY_LIB_NAME)) {
            this.libraries.remove(libname);
        }
    }

    /**
     * Constructs and returns an array containing the handles of all opened
     * libraries, in no particular order. <br/>
     * Each element of the array is an abstract value such that value at index
     * 0 is library name.
     * @return an array containing all opened libraries
     */
    public Value makeLibsArray() {
        final int sz = this.libraries.size();
        final Block res = Block.createBlock(sz, 0);
        int i = 0;
        for (String e : this.libraries.keySet()) {
            final Block l = Block.createBlock(Block.ABSTRACT_TAG, Value.createFromBlock(Block.createString(e)));
            res.set(i++, Value.createFromBlock(l));
        }
        return Value.createFromBlock(res);
    }

    /**
     * Looks up for a given primitive in a given library.
     * @param libName library name - should not be <tt>null</tt>
     * @param symbName primitive name - should not be <tt>null</tt>
     * @return an abstract value describing the primitive if found,
     *         <i>unit</i> otherwise <br/>
     *         such an abstract value has its 0-index value equals to
     *         primitive name and its custom value equal to actual method
     *         instance
     */
    public Value lookupPrimitive(final String libName, final String symbName) {
        assert libName != null : "null libName";
        assert symbName != null : "null symbName";
        if (this.libraries.containsKey(libName)) {
            final List<Class> classes = this.libraries.get(libName);
            if (classes != null) {
                for (Class cl : classes) {
                    try {
                        final Method m = Primitives.lookupPrimitive(symbName, cl);
                        final Block bl = Block.createBlock(Block.ABSTRACT_TAG, Value.createFromBlock(Block.createString(symbName)));
                        bl.setCustom(m);
                        return Value.createFromBlock(bl);
                    } catch (final Throwable t) {
                    }
                }
            }
        }
        try {
            final Method m = Interpreter.getBuiltinPrimitivesMap().get(symbName);
            if (m != null) {
                final Block bl = Block.createBlock(Block.ABSTRACT_TAG, Value.createFromBlock(Block.createString(symbName)));
                bl.setCustom(m);
                return Value.createFromBlock(bl);
            }
        } catch (final CadmiumException ie) {
        }
        return Value.UNIT;
    }

    /**
     * Adds a primitive to underlying dispatcher.
     * @param name primitive name - should not be <tt>null</tt>
     * @param impl primitive implementation - should not be <tt>null</tt>
     * @return the index of the added primitive
     */
    public int addPrimitive(final String name, final Method impl) {
        assert name != null : "null name";
        assert impl != null : "null impl";
        if ((this.dispatcher instanceof DispatcherCompiler.AbstractCompiledDispatcher) && (((DispatcherCompiler.AbstractCompiledDispatcher) this.dispatcher).notCompiled() > Context.RECOMPILE_THRESHOLD)) {
            final int len = this.dispatcher.size();
            final String[] names = new String[len + 1];
            System.arraycopy(this.dispatcher.getNames(), 0, names, 0, len);
            names[len] = name;
            final Method[] impls = new Method[len + 1];
            System.arraycopy(this.dispatcher.getMethods(), 0, impls, 0, len);
            impls[len] = impl;
            this.dispatcher = DispatcherCompiler.make(names, impls);
            if (this.dispatcher == null) {
                this.dispatcher = new ReflectionDispatcher(names, impls);
            }
            return len;
        } else {
            return this.dispatcher.addPrimitive(name, impl);
        }
    }

    /**
     * Returns the gc parameters.
     * @return the gc parameters
     */
    public Value makeGcParams() {
        final Block res = Block.createBlock(7, 0);
        res.set(0, Value.createFromLong(this.gcMinorHeapSize));
        res.set(1, Value.createFromLong(this.gcMajorHeapIncrement));
        res.set(2, Value.createFromLong(this.gcSpaceOverhead));
        res.set(3, Value.createFromLong(this.gcVerbose));
        res.set(4, Value.createFromLong(this.gcMaxOverhead));
        res.set(5, Value.createFromLong(this.gcStackLimit));
        res.set(6, Value.createFromLong(this.gcAllocationPolicy));
        return Value.createFromBlock(res);
    }

    /**
     * Sets the gc parameters.
     * @param mhs stored and returned but not used to control gc
     * @param mhi stored and returned but not used to control gc
     * @param so stored and returned but not used to control gc
     * @param v stored and returned but not used to control gc
     * @param mo stored and returned but not used to control gc
     * @param sl stored and returned but not used to control gc
     * @param ap stored and returned but not used to control gc
     */
    public void setGcParams(final int mhs, final int mhi, final int so, final int v, final int mo, final int sl, final int ap) {
        this.gcMinorHeapSize = mhs;
        this.gcMajorHeapIncrement = mhi;
        this.gcSpaceOverhead = so;
        this.gcVerbose = v;
        this.gcMaxOverhead = mo;
        this.gcStackLimit = sl;
        this.gcAllocationPolicy = (ap >= 0) && (ap <= 1) ? ap : 1;
    }

    /**
     * Returns the GC statistics.
     * @return the GC statistics
     */
    public Value gcStats() {
        final double totalMemory = Runtime.getRuntime().totalMemory() / 4;
        final Value totalMemoryAsValue = Value.createFromLong((int) totalMemory);
        final Block bl = Block.createBlock(15, 0);
        bl.set(0, Value.createFromBlock(Block.createDouble(totalMemory)));
        bl.set(1, Value.createFromBlock(Block.createDouble(totalMemory)));
        bl.set(2, Value.createFromBlock(Block.createDouble(totalMemory)));
        bl.set(3, Value.createFromLong(this.gcMinorCounter));
        bl.set(4, Value.createFromLong(this.gcMajorCounter));
        bl.set(5, totalMemoryAsValue);
        bl.set(6, Value.ONE);
        bl.set(7, Value.ZERO);
        bl.set(8, Value.ZERO);
        bl.set(9, Value.ZERO);
        bl.set(10, Value.ZERO);
        bl.set(11, Value.ZERO);
        bl.set(12, Value.ZERO);
        bl.set(13, Value.createFromLong(this.gcCompactionCounter));
        bl.set(14, totalMemoryAsValue);
        return Value.createFromBlock(bl);
    }

    /**
     * Returns the GC counters.
     * @return the GC counters
     */
    public Value gcCounters() {
        final double totalMemory = Runtime.getRuntime().totalMemory() / 4;
        final Block bl = Block.createBlock(0, Value.createFromBlock(Block.createDouble(totalMemory)), Value.createFromBlock(Block.createDouble(totalMemory)), Value.createFromBlock(Block.createDouble(totalMemory)));
        return Value.createFromBlock(bl);
    }

    /**
     * Increments the gc-minor counter.
     */
    public void incMinorCounter() {
        this.gcMinorCounter++;
    }

    /**
     * Increments the gc-major counter.
     */
    public void incMajorCounter() {
        this.gcMajorCounter++;
    }

    /**
     * Increments the gc-compaction counter.
     */
    public void incCompactionCounter() {
        this.gcCompactionCounter++;
    }

    /**
     * Tests whether parser trace is enabled.
     * @return <tt>true</tt> if parser trace is enabled,
     *         <tt>false</tt> otherwise
     */
    public boolean isParserTraceEnabled() {
        return this.parserTrace;
    }

    /**
     * Sets the state of the parser trace.
     * @param b new state of parser trace
     */
    public void setParserTrace(final boolean b) {
        this.parserTrace = b;
    }

    /**
     * Sets the debugger event count, that is the number of events remaining
     * to execute.
     * @param ec new event count - should be <tt>&gt;= 0</tt>
     */
    public void setDebuggerEventCount(final long ec) {
        assert ec >= 0 : "ec should be >= 0";
        this.debuggerEventCount = ec;
    }

    /**
     * Returns the debugger event count.
     * @return the debugger event count
     */
    public long getDebuggerEventCount() {
        return this.debuggerEventCount;
    }

    /**
     * Decrements the event count.
     * @return <tt>true</tt> if the event count reaches zero after decrement,
     *         <tt>false</tt> otherwise
     */
    public boolean decrementDebuggerEventCount() {
        return --this.debuggerEventCount == 0L;
    }

    /**
     * Enables / disables the debugger.
     * @param b <tt>true</tt> to enable debugger,
     *          <tt>false</tt> to disable debugger
     */
    public void setDebuggerInUse(final boolean b) {
        this.debuggerInUse = b;
    }

    /**
     * Tests whether the debugger is currently in use.
     * @return <tt>true</tt> if the debugger is currently in use,
     *         <tt>false</tt> otherwise
     */
    public boolean isDebuggerInUse() {
        return this.debuggerInUse;
    }

    /**
     * Sets the socket used to communicate with the debugger.
     * @param s socket to be used to communicate with the debugger
     */
    public void setDebuggerSocket(final Socket s) {
        this.debuggerSocket = s;
    }

    /**
     * Returns the socket used to communicate with the debugger.
     * @return the socket used to communicate with the debugger
     */
    public Socket getDebuggerSocket() {
        return this.debuggerSocket;
    }

    /**
     * Sets the trap barrier used by the debugger.
     * @param tb new trap barrier
     */
    public void setDebuggerTrapBarrier(final int tb) {
        this.debuggerTrapBarrier = tb;
    }

    /**
     * Returns the trap barrier used by the debugger.
     * @return the trap barrier used by the debugger
     */
    public int getDebuggerTrapBarrier() {
        return this.debuggerTrapBarrier;
    }
}
