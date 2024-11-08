package org.netbeans;

import java.util.*;
import java.beans.*;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.openide.util.*;
import org.openide.modules.ModuleInfo;
import java.security.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.openide.modules.SpecificationVersion;
import org.openide.modules.Dependency;
import org.openide.ErrorManager;
import org.netbeans.JarClassLoader;

/** Manages a collection of modules.
 * Must use {@link #mutex} to access its important methods.
 * @author Jesse Glick
 */
public final class ModuleManager {

    public static final String PROP_MODULES = "modules";

    public static final String PROP_ENABLED_MODULES = "enabledModules";

    public static final String PROP_CLASS_LOADER = "classLoader";

    static boolean PRINT_TOPOLOGICAL_EXCEPTION_STACK_TRACES = !Boolean.getBoolean("suppress.topological.exception");

    private final HashSet modules = new HashSet(100);

    private final Map modulesByName = new HashMap(100);

    private final Map moduleProblems = new HashMap(100);

    private final Map providersOf = new HashMap(25);

    private final ModuleInstaller installer;

    private ModuleFactory moduleFactory;

    private SystemClassLoader classLoader;

    private List classLoaderPatches;

    private final Object classLoaderLock = new String("ModuleManager.classLoaderLock");

    private final Events ev;

    /** Create a manager, initially with no managed modules.
     * The handler for installing modules is given.
     * Also the sink for event messages must be given.
     */
    public ModuleManager(ModuleInstaller installer, Events ev) {
        this.installer = installer;
        this.ev = ev;
        String patches = System.getProperty("netbeans.systemclassloader.patches");
        if (patches != null) {
            System.err.println("System class loader patches: " + patches);
            classLoaderPatches = new ArrayList();
            StringTokenizer tok = new StringTokenizer(patches, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                File f = new File(tok.nextToken());
                if (f.isDirectory()) {
                    classLoaderPatches.add(f);
                } else {
                    try {
                        classLoaderPatches.add(new JarFile(f));
                    } catch (IOException ioe) {
                        Util.err.annotate(ioe, ErrorManager.UNKNOWN, "Problematic file: " + f, null, null, null);
                        Util.err.notify(ioe);
                    }
                }
            }
        } else {
            classLoaderPatches = Collections.EMPTY_LIST;
        }
        classLoader = new SystemClassLoader(classLoaderPatches, new ClassLoader[] { installer.getClass().getClassLoader() }, Collections.EMPTY_SET);
        if (!Boolean.getBoolean("netbeans.use-app-classloader")) updateContextClassLoaders(classLoader, true);
        moduleFactory = (ModuleFactory) Lookup.getDefault().lookup(ModuleFactory.class);
        if (moduleFactory == null) {
            moduleFactory = new ModuleFactory();
        }
    }

    /** Access for ManifestSection. 
     * @since JST-PENDING needed by ManifestSection
     */
    public final Events getEvents() {
        return ev;
    }

    private final Mutex.Privileged MUTEX_PRIVILEGED = new Mutex.Privileged();

    private final Mutex MUTEX = new Mutex(MUTEX_PRIVILEGED);

    /** Get a locking mutex for this module installer.
     * All calls other than adding or removing property change
     * listeners, or getting the module lookup, called on this
     * class must be done within the scope of this mutex
     * (with read or write access as appropriate). Methods
     * on ModuleInfo need not be called within it; methods
     * specifically on Module do need to be called within it
     * (read access is sufficient). Note that property changes
     * are fired with read access already held for convenience.
     * Please avoid entering the mutex from "sensitive" threads
     * such as the event thread, the folder recognizer/lookup
     * thread, etc., or with other locks held (such as the Children
     * mutex), especially when entering the mutex as a writer:
     * actions such as enabling modules in particular can call
     * arbitrary foreign module code which may do a number of
     * strange things (including consuming a significant amount of
     * time and waiting for other tasks such as lookup or data
     * object recognition). Use the request processor or the IDE's
     * main startup thread or the execution engine to be safe.
     */
    public final Mutex mutex() {
        return MUTEX;
    }

    /** Classes in this package can, if careful, use the privileged form.
     * @since JST-PENDING this had to be made public as the package is now split in two
     */
    public final Mutex.Privileged mutexPrivileged() {
        return MUTEX_PRIVILEGED;
    }

    /** Manages changes accumulating in this manager and fires them when ready.
     */
    private ChangeFirer firer = new ChangeFirer(this);

    /** True while firer is firing changes.
     */
    private boolean readOnly = false;

    /** Sets the r/o flag. Access from ChangeFirer.
     * @param ro if true, cannot make any changes until set to false again
     */
    void readOnly(boolean ro) {
        readOnly = ro;
    }

    /** Assert that the current thread state permits writing.
     * Currently does not check that there is a write mutex!
     * (Pending #13352.)
     * But does check that I am not firing changes.
     * @throws IllegalThreadStateException if currently firing changes
     */
    void assertWritable() throws IllegalThreadStateException {
        if (readOnly) {
            throw new IllegalThreadStateException("You are attempting to make changes to " + this + " in a property change callback. This is illegal. You may only make module system changes while holding a write mutex and not inside a change callback. See #16328.");
        }
    }

    private PropertyChangeSupport changeSupport;

    /** Add a change listener.
     * Only the declared properties will be fired, and they are
     * not guaranteed to be fired synchronously with the change
     * (currently they are not in fact, for safety). The change
     * events are not guaranteed to provide an old and new value,
     * so you will need to use the proper
     * getter methods. When the changes are fired, you are inside
     * the mutex with read access.
     */
    public final void addPropertyChangeListener(PropertyChangeListener l) {
        synchronized (this) {
            if (changeSupport == null) changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(l);
    }

    /** Remove a change listener. */
    public final void removePropertyChangeListener(PropertyChangeListener l) {
        if (changeSupport != null) changeSupport.removePropertyChangeListener(l);
    }

    final void firePropertyChange(String prop, Object old, Object nue) {
        if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
            Util.err.log("ModuleManager.propertyChange: " + prop + ": " + old + " -> " + nue);
        }
        if (changeSupport != null) changeSupport.firePropertyChange(prop, old, nue);
    }

    /** For access from Module. */
    final void fireReloadable(Module m) {
        firer.change(new ChangeFirer.Change(m, Module.PROP_RELOADABLE, null, null));
        firer.fire();
    }

    private final Util.ModuleLookup lookup = new Util.ModuleLookup();

    /** Retrieve set of modules in Lookup form.
     * The core top manager should install this into the set of
     * available lookups. Will fire lookup events when the
     * set of modules changes (not for enabling/disabling/etc.).
     * No other subsystem should make any attempt to provide an instance of
     * ModuleInfo via lookup, so an optimization could be to jump
     * straight to this lookup when ModuleInfo/Module is requested.
     */
    public Lookup getModuleLookup() {
        return lookup;
    }

    final void fireModulesCreatedDeleted(Set created, Set deleted) {
        Util.err.log("lookup created: " + created + " deleted: " + deleted);
        lookup.changed();
    }

    /** Get a set of {@link Module}s being managed.
     * No two contained modules may at any time share the same code name base.
     * @see #PROP_MODULES
     */
    public Set getModules() {
        return (Set) modules.clone();
    }

    /** Get a set of modules managed which are currently enabled.
     * Convenience method only.
     * @see #PROP_ENABLED_MODULES
     */
    public final Set getEnabledModules() {
        Set s = new HashSet(modules);
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Module m = (Module) it.next();
            if (!m.isEnabled()) {
                it.remove();
            }
        }
        return s;
    }

    /** Convenience method to find a module by name.
     * Returns null if there is no such managed module.
     */
    public final Module get(String codeNameBase) {
        return (Module) modulesByName.get(codeNameBase);
    }

    /**
     * Get a set of modules depended upon or depending on this module.
     * <p>Note that provide-require dependencies are listed alongside direct
     * dependencies; a module with a required token is considered to depend on
     * <em>all</em> modules providing that token (though in fact only one is needed
     * to enable it).
     * <p>Illegal cyclic dependencies are omitted.
     * @param m a module to start from; may be enabled or not, but must be owned by this manager
     * @param reverse if true, find modules depending on this module; if false, find
     *                modules this module depends upon
     * @param transitive if true, these dependencies are considered transitively as well
     * @return a set (possibly empty) of modules managed by this manager, never including m
     * @since org.netbeans.core/1 > 1.17
     */
    public Set getModuleInterdependencies(Module m, boolean reverse, boolean transitive) {
        return Util.moduleInterdependencies(m, reverse, transitive, modules, modulesByName, providersOf);
    }

    /** Get a classloader capable of loading from any
     * of the enabled modules or their declared extensions.
     * Should be used as the result of TopManager.systemClassLoader.
     * Thread-safe.
     * @see #PROP_CLASS_LOADER
     */
    public ClassLoader getClassLoader() {
        synchronized (classLoaderLock) {
            return classLoader;
        }
    }

    /** Mark the current class loader as invalid and make a new one. */
    private void invalidateClassLoader() {
        synchronized (classLoaderLock) {
            classLoader.destroy();
        }
        Set foundParents = new HashSet(modules.size() * 4 / 3 + 2);
        List parents = new ArrayList(modules.size() + 1);
        ClassLoader base = ModuleManager.class.getClassLoader();
        foundParents.add(base);
        parents.add(base);
        Iterator it = modules.iterator();
        while (it.hasNext()) {
            Module m = ((Module) it.next());
            if (!m.isEnabled()) {
                continue;
            }
            if (foundParents.add(m.getClassLoader())) {
                parents.add(m.getClassLoader());
            }
        }
        if (moduleFactory.removeBaseClassLoader()) {
            parents.remove(base);
        }
        ClassLoader[] parentCLs = (ClassLoader[]) parents.toArray(new ClassLoader[parents.size()]);
        SystemClassLoader nue;
        try {
            nue = new SystemClassLoader(classLoaderPatches, parentCLs, modules);
        } catch (IllegalArgumentException iae) {
            Util.err.notify(iae);
            nue = new SystemClassLoader(classLoaderPatches, new ClassLoader[] { ModuleManager.class.getClassLoader() }, Collections.EMPTY_SET);
        }
        synchronized (classLoaderLock) {
            classLoader = nue;
            updateContextClassLoaders(classLoader, false);
        }
        firer.change(new ChangeFirer.Change(this, PROP_CLASS_LOADER, null, null));
    }

    private static void updateContextClassLoaders(ClassLoader l, boolean force) {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null) g = g.getParent();
        while (true) {
            int s = g.activeCount() + 1;
            Thread[] ts = new Thread[s];
            int x = g.enumerate(ts, true);
            if (x < s) {
                for (int i = 0; i < x; i++) {
                    if (force || (ts[i].getContextClassLoader() instanceof SystemClassLoader)) {
                        ts[i].setContextClassLoader(l);
                    } else {
                        Util.err.log("Not touching context class loader " + ts[i].getContextClassLoader() + " on thread " + ts[i].getName());
                    }
                }
                Util.err.log("Set context class loader on " + x + " threads");
                break;
            } else {
                Util.err.log("Race condition getting all threads, restarting...");
                continue;
            }
        }
    }

    /** A classloader giving access to all the module classloaders at once. */
    private final class SystemClassLoader extends JarClassLoader {

        private final PermissionCollection allPermissions;

        private final StringBuffer debugme;

        private boolean empty = true;

        public SystemClassLoader(List files, ClassLoader[] parents, Set modules) throws IllegalArgumentException {
            super(files, parents, false);
            allPermissions = new Permissions();
            allPermissions.add(new AllPermission());
            allPermissions.setReadOnly();
            debugme = new StringBuffer(100 + 50 * modules.size());
            debugme.append("SystemClassLoader[");
            Iterator it = files.iterator();
            while (it.hasNext()) {
                Object o = (Object) it.next();
                String s;
                if (o instanceof File) {
                    s = ((File) o).getAbsolutePath();
                } else {
                    s = ((JarFile) o).getName();
                }
                if (empty) {
                    empty = false;
                } else {
                    debugme.append(',');
                }
                debugme.append(s);
            }
            record(modules);
            debugme.append(']');
        }

        private void record(Collection modules) {
            Iterator it = modules.iterator();
            while (it.hasNext()) {
                if (empty) {
                    empty = false;
                } else {
                    debugme.append(',');
                }
                Module m = (Module) it.next();
                debugme.append(m.getCodeNameBase());
            }
        }

        public void append(ClassLoader[] ls, List modules) throws IllegalArgumentException {
            super.append(ls);
            debugme.deleteCharAt(debugme.length() - 1);
            record(modules);
            debugme.append(']');
        }

        protected void finalize() throws Throwable {
            super.finalize();
            Util.err.log("Collected system class loader");
        }

        public String toString() {
            if (debugme == null) {
                return "SystemClassLoader";
            }
            return debugme.toString();
        }

        protected boolean isSpecialResource(String pkg) {
            if (installer.isSpecialResource(pkg)) {
                return true;
            }
            return super.isSpecialResource(pkg);
        }

        /** Provide all permissions for any code loaded from the files list
         * (i.e. with netbeans.systemclassloader.patches).
         */
        protected PermissionCollection getPermissions(CodeSource cs) {
            return allPermissions;
        }
    }

    /** @see #create(File,Object,boolean,boolean,boolean)
     * @deprecated since org.netbeans.core/1 1.3
     */
    public Module create(File jar, Object history, boolean reloadable, boolean autoload) throws IOException, DuplicateException {
        return create(jar, history, reloadable, autoload, false);
    }

    /** Create a module from a JAR and add it to the managed set.
     * Will initially be disabled, unless it is eager and can
     * be enabled immediately.
     * May throw an IOException if the JAR file cannot be opened
     * for some reason, or is malformed.
     * If there is already a module of the same name managed,
     * throws a duplicate exception. In this case you may wish
     * to delete the original and try again.
     * You must give it some history object which can be used
     * to provide context for where the module came from and
     * whether it has been here before.
     * You cannot request that a module be both autoload and eager.
     */
    public Module create(File jar, Object history, boolean reloadable, boolean autoload, boolean eager) throws IOException, DuplicateException {
        assertWritable();
        ev.log(Events.START_CREATE_REGULAR_MODULE, jar);
        Module m = moduleFactory.create(jar.getAbsoluteFile(), history, reloadable, autoload, eager, this, ev);
        ev.log(Events.FINISH_CREATE_REGULAR_MODULE, jar);
        subCreate(m);
        if (m.isEager()) {
            List immediate = simulateEnable(Collections.EMPTY_SET);
            if (!immediate.isEmpty()) {
                if (!immediate.contains(m)) throw new IllegalStateException("Can immediately enable modules " + immediate + ", but not including " + m);
                boolean ok = true;
                Iterator it = immediate.iterator();
                while (it.hasNext()) {
                    Module other = (Module) it.next();
                    if (!other.isAutoload() && !other.isEager()) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    Util.err.log("Enabling " + m + " immediately");
                    enable(Collections.EMPTY_SET);
                }
            }
        }
        return m;
    }

    /** Create a fixed module (e.g. from classpath).
     * Will initially be disabled.
     */
    public Module createFixed(Manifest mani, Object history, ClassLoader loader) throws InvalidException, DuplicateException {
        assertWritable();
        if (mani == null || loader == null) throw new IllegalArgumentException("null manifest or loader");
        ev.log(Events.START_CREATE_BOOT_MODULE, history);
        Module m = moduleFactory.createFixed(mani, history, loader, this, ev);
        ev.log(Events.FINISH_CREATE_BOOT_MODULE, history);
        subCreate(m);
        return m;
    }

    /** Used by Module to communicate with the ModuleInstaller re. dependencies. */
    void refineDependencies(Module m, Set dependencies) {
        installer.refineDependencies(m, dependencies);
    }

    /** Allows the installer to add provides (used to provide name of platform we run on)
     */
    String[] refineProvides(Module m) {
        return installer.refineProvides(m);
    }

    /** Used by Module to communicate with the ModuleInstaller re. classloader. */
    public void refineClassLoader(Module m, List parents) {
        installer.refineClassLoader(m, parents);
    }

    /** Use by OneModuleClassLoader to communicate with the ModuleInstaller re. masking. */
    public boolean shouldDelegateResource(Module m, Module parent, String pkg) {
        Module.PackageExport[] exports = (parent == null) ? null : parent.getPublicPackages();
        if (exports != null) {
            boolean exported = false;
            if (parent.isDeclaredAsFriend(m)) {
                for (int i = 0; i < exports.length; i++) {
                    if (exports[i].recursive ? pkg.startsWith(exports[i].pkg) : pkg.equals(exports[i].pkg)) {
                        exported = true;
                        break;
                    }
                }
            }
            if (!exported) {
                boolean impldep = false;
                Dependency[] deps = m.getDependenciesArray();
                for (int i = 0; i < deps.length; i++) {
                    if (deps[i].getType() == Dependency.TYPE_MODULE && deps[i].getComparison() == Dependency.COMPARE_IMPL && deps[i].getName().equals(parent.getCodeName())) {
                        impldep = true;
                        break;
                    }
                }
                if (!impldep) {
                    if (Util.err.isLoggable(ErrorManager.INFORMATIONAL)) {
                        Util.err.log("Refusing to load non-public package " + pkg + " for " + m + " from parent module " + parent + " without an impl dependency");
                    }
                    return false;
                }
            }
        }
        if (pkg.startsWith("META-INF/")) {
            return false;
        }
        return installer.shouldDelegateResource(m, parent, pkg);
    }

    public boolean isSpecialResource(String pkg) {
        return installer.isSpecialResource(pkg);
    }

    Manifest loadManifest(File jar) throws IOException {
        return installer.loadManifest(jar);
    }

    private void subCreate(Module m) throws DuplicateException {
        Util.err.log("created: " + m);
        Module old = get(m.getCodeNameBase());
        if (old != null) {
            throw new DuplicateException(old, m);
        }
        modules.add(m);
        modulesByName.put(m.getCodeNameBase(), m);
        possibleProviderAdded(m);
        lookup.add(m);
        firer.created(m);
        firer.change(new ChangeFirer.Change(this, PROP_MODULES, null, null));
        clearProblemCache();
        firer.fire();
    }

    private void possibleProviderAdded(Module m) {
        String[] provides = m.getProvides();
        for (int i = 0; i < provides.length; i++) {
            Set providing = (Set) providersOf.get(provides[i]);
            if (providing == null) {
                providing = new HashSet(10);
                providersOf.put(provides[i], providing);
            }
            providing.add(m);
        }
    }

    /** Remove a module from the managed set.
     * Must be disabled first.
     * Must not be a "fixed" module.
     */
    public void delete(Module m) throws IllegalArgumentException {
        assertWritable();
        if (m.isFixed()) throw new IllegalArgumentException("fixed module: " + m);
        if (m.isEnabled()) throw new IllegalArgumentException("enabled module: " + m);
        ev.log(Events.DELETE_MODULE, m);
        modules.remove(m);
        modulesByName.remove(m.getCodeNameBase());
        possibleProviderRemoved(m);
        lookup.remove(m);
        firer.deleted(m);
        firer.change(new ChangeFirer.Change(this, PROP_MODULES, null, null));
        firer.change(new ChangeFirer.Change(m, Module.PROP_VALID, Boolean.TRUE, Boolean.FALSE));
        clearProblemCache();
        m.destroy();
        firer.fire();
    }

    private void possibleProviderRemoved(Module m) {
        String[] provides = m.getProvides();
        for (int i = 0; i < provides.length; i++) {
            Set providing = (Set) providersOf.get(provides[i]);
            if (providing != null) {
                providing.remove(m);
                if (providing.isEmpty()) {
                    providersOf.remove(provides[i]);
                }
            } else {
            }
        }
    }

    /** Reload a module.
     * This could make a fresh copy of its JAR file preparing
     * to enable it with different contents; at least it will
     * rescan the manifest.
     * It must currently be disabled and not "fixed", and it will
     * stay disabled after this call; to actually reinstall it
     * requires a separate call.
     * It may or may not actually be marked "reloadable", but
     * for greatest reliability it should be.
     * Besides actually reloading the contents, any cached information
     * about failed dependencies or runtime problems with the module
     * is cleared so it may be tried again.
     */
    public void reload(Module m) throws IllegalArgumentException, IOException {
        assertWritable();
        Util.err.log("reload: " + m);
        if (m.isFixed()) throw new IllegalArgumentException("reload fixed module: " + m);
        if (m.isEnabled()) throw new IllegalArgumentException("reload enabled module: " + m);
        possibleProviderRemoved(m);
        try {
            m.reload();
        } catch (IOException ioe) {
            delete(m);
            throw ioe;
        }
        possibleProviderAdded(m);
        firer.change(new ChangeFirer.Change(m, Module.PROP_MANIFEST, null, null));
        moduleProblems.remove(m);
        firer.change(new ChangeFirer.Change(m, Module.PROP_PROBLEMS, null, null));
        clearProblemCache();
        firer.fire();
    }

    /** Enable a single module.
     * Must have satisfied its dependencies.
     * Must not be an autoload module, when supported.
     */
    public final void enable(Module m) throws IllegalArgumentException, InvalidException {
        enable(Collections.singleton(m));
    }

    /** Disable a single module.
     * Must not be required by any enabled modules.
     * Must not be an autoload module, when supported.
     */
    public final void disable(Module m) throws IllegalArgumentException {
        disable(Collections.singleton(m));
    }

    /** Enable a set of modules together.
     * Must have satisfied their dependencies
     * (possibly with one another).
     * Must not contain autoload nor eager modules.
     * Might contain fixed modules (they can only be installed once of course).
     * It is permissible to pass in modules which in fact at runtime cannot
     * satisfy their package dependencies, or which {@link ModuleInstaller#prepare}
     * rejects on the basis of missing contents. In such a case InvalidException
     * will be thrown and nothing will be installed. The InvalidException in such
     * a case should contain a reference to the offending module.
     */
    public void enable(Set modules) throws IllegalArgumentException, InvalidException {
        assertWritable();
        Util.err.log("enable: " + modules);
        ev.log(Events.PERF_START, "ModuleManager.enable");
        List toEnable = simulateEnable(modules);
        ev.log(Events.PERF_TICK, "checked the required ordering and autoloads");
        Util.err.log("enable: toEnable=" + toEnable);
        {
            Set testing = new HashSet(toEnable);
            if (!testing.containsAll(modules)) {
                Set bogus = new HashSet(modules);
                bogus.removeAll(testing);
                throw new IllegalArgumentException("Not all requested modules can be enabled: " + bogus);
            }
            Iterator it = testing.iterator();
            while (it.hasNext()) {
                Module m = (Module) it.next();
                if (!modules.contains(m) && !m.isAutoload() && !m.isEager()) {
                    throw new IllegalArgumentException("Would also need to enable " + m);
                }
            }
        }
        Util.err.log("enable: verified dependencies");
        ev.log(Events.PERF_TICK, "verified dependencies");
        ev.log(Events.START_ENABLE_MODULES, toEnable);
        {
            LinkedList fallback = new LinkedList();
            boolean tryingClassLoaderUp = false;
            Dependency failedPackageDep = null;
            try {
                Iterator teIt = toEnable.iterator();
                ev.log(Events.PERF_START, "module preparation");
                while (teIt.hasNext()) {
                    Module m = (Module) teIt.next();
                    fallback.addFirst(m);
                    Util.err.log("enable: bringing up: " + m);
                    ev.log(Events.PERF_START, "bringing up classloader on " + m.getCodeName());
                    try {
                        Dependency[] dependencies = m.getDependenciesArray();
                        Set parents = new HashSet(dependencies.length * 4 / 3 + 1);
                        for (int i = 0; i < dependencies.length; i++) {
                            Dependency dep = dependencies[i];
                            if (dep.getType() != Dependency.TYPE_MODULE) {
                                continue;
                            }
                            String name = (String) Util.parseCodeName(dep.getName())[0];
                            Module parent = get(name);
                            if (parent == null) throw new IOException("Parent " + name + " not found!");
                            parents.add(parent);
                        }
                        m.classLoaderUp(parents);
                    } catch (IOException ioe) {
                        tryingClassLoaderUp = true;
                        InvalidException ie = new InvalidException(m, ioe.toString());
                        Util.err.annotate(ie, ioe);
                        throw ie;
                    }
                    m.setEnabled(true);
                    ev.log(Events.PERF_END, "bringing up classloader on " + m.getCodeName());
                    ev.log(Events.PERF_START, "package dependency check on " + m.getCodeName());
                    Util.err.log("enable: checking package dependencies for " + m);
                    Dependency[] dependencies = m.getDependenciesArray();
                    for (int i = 0; i < dependencies.length; i++) {
                        Dependency dep = dependencies[i];
                        if (dep.getType() != Dependency.TYPE_PACKAGE) {
                            continue;
                        }
                        if (!Util.checkPackageDependency(dep, m.getClassLoader())) {
                            failedPackageDep = dep;
                            throw new InvalidException(m, "Dependency failed on " + dep);
                        }
                        Util.err.log("Successful check for: " + dep);
                    }
                    ev.log(Events.PERF_END, "package dependency check on " + m.getCodeName());
                    ev.log(Events.PERF_START, "ModuleInstaller.prepare " + m.getCodeName());
                    installer.prepare(m);
                    ev.log(Events.PERF_END, "ModuleInstaller.prepare " + m.getCodeName());
                }
                ev.log(Events.PERF_END, "module preparation");
            } catch (InvalidException ie) {
                Module bad = ie.getModule();
                if (bad == null) throw new IllegalStateException("Problem with no associated module: " + ie);
                Set probs = (Set) moduleProblems.get(bad);
                if (probs == null) throw new IllegalStateException("Were trying to install a module that had never been checked: " + bad);
                if (!probs.isEmpty()) throw new IllegalStateException("Were trying to install a module that was known to be bad: " + bad);
                if (failedPackageDep != null) {
                    probs.add(failedPackageDep);
                } else {
                    probs.add(ie);
                }
                clearProblemCache();
                firer.change(new ChangeFirer.Change(bad, Module.PROP_PROBLEMS, Collections.EMPTY_SET, Collections.singleton(probs.iterator().next())));
                Util.err.log("enable: will roll back from: " + ie);
                Iterator fbIt = fallback.iterator();
                while (fbIt.hasNext()) {
                    Module m = (Module) fbIt.next();
                    if (m.isFixed()) {
                        continue;
                    }
                    m.setEnabled(false);
                    if (tryingClassLoaderUp) {
                        tryingClassLoaderUp = false;
                    } else {
                        m.classLoaderDown();
                        System.gc();
                        System.runFinalization();
                        m.cleanup();
                    }
                }
                firer.fire();
                throw ie;
            }
            if (classLoader != null) {
                Util.err.log("enable: adding to system classloader");
                List nueclassloaders = new ArrayList(toEnable.size());
                Iterator teIt = toEnable.iterator();
                if (moduleFactory.removeBaseClassLoader()) {
                    ClassLoader base = ModuleManager.class.getClassLoader();
                    nueclassloaders.add(moduleFactory.getClasspathDelegateClassLoader(this, base));
                    while (teIt.hasNext()) {
                        ClassLoader c1 = ((Module) teIt.next()).getClassLoader();
                        if (c1 != base) {
                            nueclassloaders.add(c1);
                        }
                    }
                } else {
                    while (teIt.hasNext()) {
                        nueclassloaders.add(((Module) teIt.next()).getClassLoader());
                    }
                }
                classLoader.append((ClassLoader[]) (nueclassloaders.toArray(new ClassLoader[nueclassloaders.size()])), toEnable);
            } else {
                Util.err.log("enable: no class loader yet, not appending");
            }
            Util.err.log("enable: continuing to installation");
            installer.load(toEnable);
        }
        {
            Util.err.log("enable: firing changes");
            firer.change(new ChangeFirer.Change(this, PROP_ENABLED_MODULES, null, null));
            Iterator it = toEnable.iterator();
            while (it.hasNext()) {
                Module m = (Module) it.next();
                firer.change(new ChangeFirer.Change(m, ModuleInfo.PROP_ENABLED, Boolean.FALSE, Boolean.TRUE));
                if (!m.isFixed()) {
                    firer.change(new ChangeFirer.Change(m, Module.PROP_CLASS_LOADER, null, null));
                }
            }
        }
        ev.log(Events.FINISH_ENABLE_MODULES, toEnable);
        firer.fire();
    }

    /** Disable a set of modules together.
     * Must not be required by any enabled
     * modules (except one another).
     * Must not contain autoload nor eager modules.
     * Must not contain fixed modules.
     */
    public void disable(Set modules) throws IllegalArgumentException {
        assertWritable();
        Util.err.log("disable: " + modules);
        if (modules.isEmpty()) {
            return;
        }
        List toDisable = simulateDisable(modules);
        Util.err.log("disable: toDisable=" + toDisable);
        {
            Iterator it = toDisable.iterator();
            while (it.hasNext()) {
                Module m = (Module) it.next();
                if (!modules.contains(m) && !m.isAutoload() && !m.isEager()) {
                    throw new IllegalArgumentException("Would also need to disable: " + m);
                }
            }
        }
        Util.err.log("disable: verified dependencies");
        ev.log(Events.START_DISABLE_MODULES, toDisable);
        {
            installer.unload(toDisable);
            Iterator it = toDisable.iterator();
            while (it.hasNext()) {
                Module m = (Module) it.next();
                installer.dispose(m);
                m.setEnabled(false);
                m.classLoaderDown();
            }
            System.gc();
            System.runFinalization();
            it = toDisable.iterator();
            while (it.hasNext()) {
                Module m = (Module) it.next();
                m.cleanup();
            }
        }
        Util.err.log("disable: finished, will notify changes");
        {
            firer.change(new ChangeFirer.Change(this, PROP_ENABLED_MODULES, null, null));
            invalidateClassLoader();
            Iterator it = toDisable.iterator();
            while (it.hasNext()) {
                Module m = (Module) it.next();
                firer.change(new ChangeFirer.Change(m, ModuleInfo.PROP_ENABLED, Boolean.TRUE, Boolean.FALSE));
                firer.change(new ChangeFirer.Change(m, Module.PROP_CLASS_LOADER, null, null));
            }
        }
        ev.log(Events.FINISH_DISABLE_MODULES, toDisable);
        firer.fire();
    }

    /** Simulate what would happen if a set of modules were to be enabled.
     * None of the listed modules may be autoload modules, nor eager, nor currently enabled,
     * though they may be fixed (if they have not yet been enabled).
     * It may happen that some of them do not satisfy their dependencies.
     * It may also happen that some of them require other, currently disabled,
     * modules to be enabled in order for them to be enabled.
     * It may further happen that some currently disabled eager modules could
     * be enabled as a result of these modules being enabled.
     * The returned set is the set of all modules that actually could be enabled.
     * It will include the requested modules, minus any that cannot satisfy
     * their dependencies (even on each other), plus any managed but currently
     * disabled modules that would need to be enabled (including autoload modules
     * required by some listed module but not by any currently enabled module),
     * plus any eager modules which can be enabled with the other enablements
     * (and possibly any autoloads needed by those eager modules).
     * Where a requested module requires some token, either it will not be included
     * in the result (in case the dependency cannot be satisfied), or it will, and
     * all modules providing that token which can be included will be included, even
     * if it would suffice to choose only one - unless a module providing that token
     * is already enabled or in the requested list,
     * in which case just the requested module will be listed.
     * Modules are returned in an order in which they could be enabled (where
     * base modules are always enabled before dependent modules).
     * Note that the returned list might include modules which in fact cannot be
     * enabled either because some package dependencies (which are checked only
     * on a live classloader) cannot be met; or {@link ModuleInstaller#prepare}
     * indicates that the modules are not in a valid format to install; or
     * creating the module classloader fails unexpectedly.
     */
    public List simulateEnable(Set modules) throws IllegalArgumentException {
        Set willEnable = new HashSet(modules.size() * 2 + 1);
        Iterator it = modules.iterator();
        while (it.hasNext()) {
            Module m = (Module) it.next();
            if (m.isAutoload()) throw new IllegalArgumentException("Cannot simulate enabling an autoload: " + m);
            if (m.isEager()) throw new IllegalArgumentException("Cannot simulate enabling an eager module: " + m);
            if (m.isEnabled()) throw new IllegalArgumentException("Already enabled: " + m);
            if (!m.isValid()) throw new IllegalArgumentException("Not managed by me: " + m + " in " + m);
            maybeAddToEnableList(willEnable, modules, m, true);
        }
        Set stillDisabled = new HashSet(this.modules);
        it = stillDisabled.iterator();
        while (it.hasNext()) {
            Module m = (Module) it.next();
            if (m.isEnabled() || willEnable.contains(m)) {
                it.remove();
            }
        }
        while (searchForPossibleEager(willEnable, stillDisabled, modules)) {
        }
        Map deps = Util.moduleDependencies(willEnable, modulesByName, providersOf);
        try {
            List l = Utilities.topologicalSort(willEnable, deps);
            Collections.reverse(l);
            return l;
        } catch (TopologicalSortException ex) {
            if (PRINT_TOPOLOGICAL_EXCEPTION_STACK_TRACES) {
                Util.err.notify(ErrorManager.INFORMATIONAL, ex);
            }
            Util.err.log(ErrorManager.WARNING, "Cyclic module dependencies, will refuse to enable: " + deps);
            return Collections.EMPTY_LIST;
        }
    }

    private void maybeAddToEnableList(Set willEnable, Set mightEnable, Module m, boolean okToFail) {
        if (!missingDependencies(m).isEmpty()) {
            if (!okToFail) throw new IllegalStateException("Module was supposed to be OK: " + m);
            return;
        }
        if (willEnable.contains(m)) {
            return;
        }
        willEnable.add(m);
        Dependency[] dependencies = m.getDependenciesArray();
        for (int i = 0; i < dependencies.length; i++) {
            Dependency dep = dependencies[i];
            if (dep.getType() == Dependency.TYPE_MODULE) {
                String codeNameBase = (String) Util.parseCodeName(dep.getName())[0];
                Module other = get(codeNameBase);
                if (other == null) throw new IllegalStateException("Should have found module: " + codeNameBase);
                if (!other.isEnabled()) {
                    maybeAddToEnableList(willEnable, mightEnable, other, false);
                }
            } else if (dep.getType() == Dependency.TYPE_REQUIRES) {
                String token = dep.getName();
                Set providers = (Set) providersOf.get(token);
                if (providers == null) throw new IllegalStateException("Should have found a provider of: " + token);
                Iterator provIt = providers.iterator();
                boolean foundOne = false;
                while (provIt.hasNext()) {
                    Module other = (Module) provIt.next();
                    if (other.isEnabled() || (other.getProblems().isEmpty() && mightEnable.contains(other))) {
                        foundOne = true;
                        break;
                    }
                }
                if (foundOne) {
                    continue;
                }
                provIt = providers.iterator();
                while (provIt.hasNext()) {
                    Module other = (Module) provIt.next();
                    maybeAddToEnableList(willEnable, mightEnable, other, true);
                    if (!foundOne && willEnable.contains(other)) {
                        foundOne = true;
                    }
                }
                if (!foundOne) {
                    throw new IllegalStateException("Should have found a nonproblematic provider of: " + token);
                }
            }
        }
    }

    private boolean searchForPossibleEager(Set willEnable, Set stillDisabled, Set mightEnable) {
        boolean found = false;
        Iterator it = stillDisabled.iterator();
        FIND_EAGER: while (it.hasNext()) {
            Module m = (Module) it.next();
            if (willEnable.contains(m)) {
                it.remove();
                continue;
            }
            if (m.isEager()) {
                if (couldBeEnabledWithEagers(m, willEnable, new HashSet())) {
                    found = true;
                    it.remove();
                    maybeAddToEnableList(willEnable, mightEnable, m, false);
                }
            }
        }
        return found;
    }

    private boolean couldBeEnabledWithEagers(Module m, Set willEnable, Set recursion) {
        if (m.isEnabled() || willEnable.contains(m)) return true;
        if (!m.isAutoload() && !m.isEager()) return false;
        if (!m.getProblems().isEmpty()) return false;
        if (!recursion.add(m)) {
            return true;
        }
        Dependency[] dependencies = m.getDependenciesArray();
        for (int i = 0; i < dependencies.length; i++) {
            Dependency dep = dependencies[i];
            if (dep.getType() == Dependency.TYPE_MODULE) {
                String codeNameBase = (String) Util.parseCodeName(dep.getName())[0];
                Module other = get(codeNameBase);
                if (other == null) throw new IllegalStateException("Should have found module: " + codeNameBase);
                if (!couldBeEnabledWithEagers(other, willEnable, recursion)) return false;
            } else if (dep.getType() == Dependency.TYPE_REQUIRES) {
                Set providers = (Set) providersOf.get(dep.getName());
                if (providers == null) throw new IllegalStateException("Should have found a provider of: " + dep.getName());
                Iterator provIt = providers.iterator();
                boolean foundOne = false;
                while (provIt.hasNext()) {
                    Module other = (Module) provIt.next();
                    if (couldBeEnabledWithEagers(other, willEnable, recursion)) {
                        foundOne = true;
                        break;
                    }
                }
                if (!foundOne) return false;
            }
        }
        return true;
    }

    /** Simulate what would happen if a set of modules were to be disabled.
     * None of the listed modules may be autoload modules, nor eager, nor currently disabled, nor fixed.
     * The returned set will list all modules that would actually be disabled,
     * meaning the listed modules, plus any currently enabled but unlisted modules
     * (including autoloads) that require some listed modules, plus any autoloads
     * which would no longer be needed as they were only required by modules
     * otherwise disabled.
     * Provide-require pairs count for purposes of disablement: if the set of
     * requested modules includes all remaining enabled providers of some token,
     * and modules requiring that token will need to be disabled as well.
     * Modules are returned in an order in which they could be disabled (where
     * dependent modules are always disabled before base modules).
     */
    public List simulateDisable(Set modules) throws IllegalArgumentException {
        if (modules.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Set willDisable = new HashSet(20);
        Iterator it = modules.iterator();
        while (it.hasNext()) {
            Module m = (Module) it.next();
            if (m.isAutoload()) throw new IllegalArgumentException("Cannot disable autoload: " + m);
            if (m.isEager()) throw new IllegalArgumentException("Cannot disable eager module: " + m);
            if (m.isFixed()) throw new IllegalArgumentException("Cannot disable fixed module: " + m);
            if (!m.isEnabled()) throw new IllegalArgumentException("Already disabled: " + m);
            addToDisableList(willDisable, m);
        }
        Set stillEnabled = new HashSet(getEnabledModules());
        stillEnabled.removeAll(willDisable);
        while (searchForUnusedAutoloads(willDisable, stillEnabled)) {
        }
        Map deps = Util.moduleDependencies(willDisable, modulesByName, providersOf);
        try {
            return Utilities.topologicalSort(willDisable, deps);
        } catch (TopologicalSortException ex) {
            if (PRINT_TOPOLOGICAL_EXCEPTION_STACK_TRACES) {
                Util.err.notify(ErrorManager.INFORMATIONAL, ex);
            }
            Util.err.log(ErrorManager.WARNING, "Cyclic module dependencies, will turn them off in a random order: " + deps);
            return new ArrayList(willDisable);
        }
    }

    private void addToDisableList(Set willDisable, Module m) {
        if (willDisable.contains(m)) {
            return;
        }
        willDisable.add(m);
        Iterator it = modules.iterator();
        while (it.hasNext()) {
            Module other = (Module) it.next();
            if (other.isFixed() || !other.isEnabled() || willDisable.contains(other)) {
                continue;
            }
            Dependency[] depenencies = other.getDependenciesArray();
            for (int i = 0; i < depenencies.length; i++) {
                Dependency dep = depenencies[i];
                if (dep.getType() == Dependency.TYPE_MODULE) {
                    if (dep.getName().equals(m.getCodeName())) {
                        addToDisableList(willDisable, other);
                        break;
                    }
                } else if (dep.getType() == Dependency.TYPE_REQUIRES) {
                    if (m.provides(dep.getName())) {
                        Iterator thirdModules = getEnabledModules().iterator();
                        boolean foundOne = false;
                        while (thirdModules.hasNext()) {
                            Module third = (Module) thirdModules.next();
                            if (third.isEnabled() && !willDisable.contains(third) && third.provides(dep.getName())) {
                                foundOne = true;
                                break;
                            }
                        }
                        if (!foundOne) {
                            addToDisableList(willDisable, other);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean searchForUnusedAutoloads(Set willDisable, Set stillEnabled) {
        boolean found = false;
        Iterator it = stillEnabled.iterator();
        FIND_AUTOLOADS: while (it.hasNext()) {
            Module m = (Module) it.next();
            if (m.isAutoload()) {
                Iterator it2 = stillEnabled.iterator();
                while (it2.hasNext()) {
                    Module other = (Module) it2.next();
                    Dependency[] dependencies = other.getDependenciesArray();
                    for (int i = 0; i < dependencies.length; i++) {
                        Dependency dep = dependencies[i];
                        if (dep.getType() == Dependency.TYPE_MODULE) {
                            if (dep.getName().equals(m.getCodeName())) {
                                continue FIND_AUTOLOADS;
                            }
                        } else if (dep.getType() == Dependency.TYPE_REQUIRES) {
                            if (m.provides(dep.getName())) {
                                continue FIND_AUTOLOADS;
                            }
                        }
                    }
                }
                found = true;
                it.remove();
                willDisable.add(m);
            }
        }
        return found;
    }

    private static final Object PROBING_IN_PROCESS = new Object();

    Set missingDependencies(Module probed) {
        synchronized (moduleProblems) {
            return _missingDependencies(probed);
        }
    }

    private Set _missingDependencies(Module probed) {
        Set probs = (Set) moduleProblems.get(probed);
        if (probs == null) {
            probs = new HashSet(8);
            probs.add(PROBING_IN_PROCESS);
            moduleProblems.put(probed, probs);
            Dependency[] dependencies = probed.getDependenciesArray();
            for (int i = 0; i < dependencies.length; i++) {
                Dependency dep = dependencies[i];
                if (dep.getType() == Dependency.TYPE_PACKAGE) {
                } else if (dep.getType() == Dependency.TYPE_MODULE) {
                    Object[] depParse = Util.parseCodeName(dep.getName());
                    String codeNameBase = (String) depParse[0];
                    int relVersionMin = (depParse[1] != null) ? ((Integer) depParse[1]).intValue() : -1;
                    int relVersionMax = (depParse[2] != null) ? ((Integer) depParse[2]).intValue() : relVersionMin;
                    Module other = get(codeNameBase);
                    if (other == null) {
                        probs.add(dep);
                        continue;
                    }
                    if (relVersionMin == relVersionMax) {
                        if (relVersionMin != other.getCodeNameRelease()) {
                            probs.add(dep);
                            continue;
                        }
                        if (dep.getComparison() == Dependency.COMPARE_IMPL && !Utilities.compareObjects(dep.getVersion(), other.getImplementationVersion())) {
                            probs.add(dep);
                            continue;
                        }
                        if (dep.getComparison() == Dependency.COMPARE_SPEC && new SpecificationVersion(dep.getVersion()).compareTo(other.getSpecificationVersion()) > 0) {
                            probs.add(dep);
                            continue;
                        }
                    } else if (relVersionMin < relVersionMax) {
                        int otherRel = other.getCodeNameRelease();
                        if (otherRel < relVersionMin || otherRel > relVersionMax) {
                            probs.add(dep);
                            continue;
                        }
                        if (dep.getComparison() == Dependency.COMPARE_IMPL) {
                            throw new IllegalStateException("No such thing as ranged impl dep");
                        }
                        if (dep.getComparison() == Dependency.COMPARE_SPEC && otherRel == relVersionMin && new SpecificationVersion(dep.getVersion()).compareTo(other.getSpecificationVersion()) > 0) {
                            probs.add(dep);
                            continue;
                        }
                    } else {
                        throw new IllegalStateException("Upside-down rel vers range");
                    }
                    if (!other.isEnabled()) {
                        if (!_missingDependencies(other).isEmpty()) {
                            probs.add(dep);
                            continue;
                        }
                    }
                } else if (dep.getType() == Dependency.TYPE_REQUIRES) {
                    String token = dep.getName();
                    Set providers = (Set) providersOf.get(token);
                    if (providers == null) {
                        probs.add(dep);
                    } else {
                        Iterator provIt = providers.iterator();
                        boolean foundOne = false;
                        while (provIt.hasNext()) {
                            Module other = (Module) provIt.next();
                            if (other.isEnabled()) {
                                foundOne = true;
                            } else {
                                if (_missingDependencies(other).isEmpty()) {
                                    foundOne = true;
                                }
                            }
                        }
                        if (!foundOne) {
                            probs.add(dep);
                        }
                    }
                } else {
                    if (!Util.checkJavaDependency(dep)) {
                        probs.add(dep);
                    }
                }
            }
            probs.remove(PROBING_IN_PROCESS);
        }
        return probs;
    }

    /** Forget about any possible "soft" problems there might have been.
     * Next time anyone asks, recompute them.
     * Currently enabled modules are left alone (no problems).
     * Otherwise, any problems which are "hard" (result from failed
     * Java/IDE/package dependencies, runtime errors, etc.) are left alone;
     * "soft" problems of inter-module dependencies are cleared
     * so they will be recomputed next time, and corresponding
     * changes are fired (since the next call to getProblem might
     * return a different result).
     */
    private void clearProblemCache() {
        Iterator it = moduleProblems.entrySet().iterator();
        SCAN_CACHE: while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Module m = (Module) entry.getKey();
            if (!m.isEnabled()) {
                Set s = (Set) entry.getValue();
                if (s != null) {
                    Iterator probsIt = s.iterator();
                    while (probsIt.hasNext()) {
                        Object problem = probsIt.next();
                        if (problem instanceof InvalidException) {
                            continue SCAN_CACHE;
                        }
                        Dependency dep = (Dependency) problem;
                        if (dep.getType() != Dependency.TYPE_MODULE && dep.getType() != Dependency.TYPE_REQUIRES) {
                            continue SCAN_CACHE;
                        }
                    }
                    it.remove();
                    firer.change(new ChangeFirer.Change(m, Module.PROP_PROBLEMS, null, null));
                }
            }
        }
    }

    /** Try to shut down the system.
     * First all modules are asked if they wish to close, in the proper order.
     * Assuming they say yes, then they are informed of the close.
     * Returns true if they all said yes.
     */
    public boolean shutDown() {
        return shutDown(null);
    }

    /**
     * Try to shut down the system.
     * First all modules are asked if they wish to close, in the proper order.
     * Assuming they say yes, a hook is run, then they are informed of the close.
     * If they did not agree to close, the hook is not run.
     * @param midHook a hook to run before closing modules if they agree to close
     * @return true if they all said yes and the module system is now shut down
     * @since org.netbeans.core/1 1.11
     */
    public boolean shutDown(Runnable midHook) {
        assertWritable();
        Set unorderedModules = getEnabledModules();
        Map deps = Util.moduleDependencies(unorderedModules, modulesByName, providersOf);
        List modules;
        try {
            modules = Utilities.topologicalSort(unorderedModules, deps);
        } catch (TopologicalSortException ex) {
            if (PRINT_TOPOLOGICAL_EXCEPTION_STACK_TRACES) {
                Util.err.notify(ErrorManager.INFORMATIONAL, ex);
            }
            Util.err.log(ErrorManager.WARNING, "Cyclic module dependencies, will not shut down cleanly: " + deps);
            return true;
        }
        if (!installer.closing(modules)) {
            return false;
        }
        if (midHook != null) {
            try {
                midHook.run();
            } catch (RuntimeException e) {
                Util.err.notify(e);
            } catch (LinkageError e) {
                Util.err.notify(e);
            }
        }
        installer.close(modules);
        return true;
    }
}
