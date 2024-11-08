package jpfm;

import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import jpfm.fs.FSUtils;
import jpfm.fs.UtilityServiceProvider;
import jpfm.mount.MountProvider;
import jpfm.mount.Mounts;
import static jpfm.OperatingSystem.*;

/**
 * Class describing the properties of this JPfm revision.
 * Two JPfm revisions should be assumed to be incompatible.
 * For this reason every jar files are suffixed with revision number,
 * and appropriate native libraries are bundled in the jar itself.
 * Two JPfm revision can work on the same system simultaneously.
 * It is unknown what kind of problems might arise by running two JPfm revisions
 * on the same jvm instance, even with separate ClassLoader for each.
 * <br/>
 * If you have just started reading the documentation, it is suggested that
 * you have a look at {@link JPfm.Manager } .
 * After this you should have a look at one of the samples.
 * The samples are present in jpfm-fs project.
 * <br/>
 * <u>Note</u> : jpfm.jar contains the bare minimum classes and native dynamic
 * libraries required, which require jdk6 , and jpfm-fs.jar contains
 * sample implementations some of which require jdk7.
 * @author Shashank Tulsyan
 */
public final class JPfm {

    private static Manager currentManager;

    private static final JPfm JPFM_SINGLETON = new JPfm();

    static {
        FileId.Builder.class.getClass();
        FileDescriptor.Builder.class.getClass();
        FileDescriptor.FDModifier.class.getClass();
    }

    private static FileId.Builder fileIdBuilder = null;

    private static FileDescriptor.Builder fileDescriptorBuilder = null;

    private static FileDescriptor.FDModifier fDModifier = null;

    private static Throwable lastException_static = null;

    /**
     * @param manager
     * @return true if and only if the manager instances passed is the
     * same as the one which is in use
     */
    public static final boolean isManagerValid(Manager manager) {
        currentManager.getLogger().log(Level.FINE, "comparing  {0}?{1}", new Object[] { currentManager, manager });
        if (manager == null) return false;
        return (manager == currentManager);
    }

    public static synchronized void setFileIdBuilder(FileId.Builder builder) {
        if (builder == null) throw new IllegalArgumentException("Trying to set builder to null");
        fileIdBuilder = builder;
    }

    public static synchronized void setFileDescriptorBuilder(FileDescriptor.Builder fileD) {
        if (fileD == null) throw new IllegalArgumentException("Trying to set builder to null");
        fileDescriptorBuilder = fileD;
    }

    public static synchronized void setFDModifier(FileDescriptor.FDModifier fDM) {
        if (fDM == null) throw new IllegalArgumentException("Trying to set builder to null");
        fDModifier = fDM;
    }

    public FileDescriptor.FDModifier getFileDescriptorModifier() {
        return fDModifier;
    }

    public FileDescriptor.Builder getFileDescriptorBuilder() {
        return fileDescriptorBuilder;
    }

    public FileId.Builder getFileIdBuilder() {
        return fileIdBuilder;
    }

    private JPfm() {
    }

    static boolean managerInitialized() {
        return (currentManager != null);
    }

    static Logger getManagerLogger() {
        return currentManager.getLogger();
    }

    static boolean hasBeenInitialized() {
        if (!managerInitialized()) return false;
        if (currentManager == DefaultManager.INSTANCE) if (JPfmMount.isJpfmApiCreated()) return true;
        return true;
    }

    /**
     * The version is specified by the revision number of the
     * project's svn repository.
     * @return
     */
    public static final int getJPfmVersion() {
        return 113;
    }

    /**
     * The minimum version of pismo file mount that is required
     * for this version of jpfm.
     * @return
     */
    public static final int getMinimumVersionOfPismoFileMountRequired() {
        return 159;
    }

    public static final OperatingSystem[] getOperatingSystemsSupportedByDefaultImplementation() {
        return new OperatingSystem[] { new Windows(new MachineUtils.MachineType[] { MachineUtils.MachineType.X86, MachineUtils.MachineType.AMD64 }, new String[] { "98", "2000", "XP", "Vista", "7" }), new Linux(new MachineUtils.MachineType[] { MachineUtils.MachineType.X86, MachineUtils.MachineType.AMD64 }, new String[] { "Ubuntu 9, KUbuntu 10" }), new Macintosh(new MachineUtils.MachineType[] { MachineUtils.MachineType.X86, MachineUtils.MachineType.AMD64, MachineUtils.MachineType.POWERPC, MachineUtils.MachineType.POWERPC64 }, new String[] { "OSX 10.5.8" }) };
    }

    public static final OperatingSystem[] getOperatingSystemsFullyTestedOn() {
        return new OperatingSystem[] { new Windows(new MachineUtils.MachineType[] { MachineUtils.MachineType.X86, MachineUtils.MachineType.AMD64 }, new String[] { "XP", "Vista", "7" }), new Linux(new MachineUtils.MachineType[] { MachineUtils.MachineType.X86, MachineUtils.MachineType.AMD64 }, new String[] { "Ubuntu 9, KUbuntu 10" }), new Macintosh(new MachineUtils.MachineType[] { MachineUtils.MachineType.X86, MachineUtils.MachineType.AMD64 }, new String[] { "OSX 10.5.8" }) };
    }

    public static boolean canSupport(OperatingSystem operatingSystem) {
        if (currentManager == null) throw new IllegalStateException("Manager not set");
        currentManager.canSupport(operatingSystem);
        return false;
    }

    /**
     * @return last exception and clears it's value
     */
    public static Throwable getLastException() {
        Throwable me = lastException_static;
        lastException_static = null;
        return me;
    }

    /**
     * JPfm allows alternate service providers to it's filesystem api.
     * If this function is called, then the default implementation is used.
     * If the Manager was already set and is something other than
     * the Manager that implementor is trying to set then this
     * method throws an exception.
     * @return An instance to the the default manager
     */
    public static final synchronized Manager setDefaultManager() {
        if (currentManager instanceof DefaultManager) return null;
        if (setManager(DefaultManager.getDefaultManager())) {
            return DefaultManager.getDefaultManager();
        }
        Mounts.setMountProvider(JPFM_SINGLETON, null);
        throw new NullPointerException("JPfm\'s default manager could not be initialized. Check the log for reasons.");
    }

    public static final synchronized Manager setDefaultManager(DefaultManager.LibraryLoader libraryLoader) {
        return setDefaultManager(libraryLoader, true);
    }

    /**
     * 
     * @param libraryLoader
     * @param useAsFallback If this is set true the custom loadLibrary function is called
     * if and only if the internal library loading fails for somereason.
     * if this is set false, only the custom loadLibrary is called.
     * @return 
     */
    public static final synchronized Manager setDefaultManager(DefaultManager.LibraryLoader libraryLoader, boolean useAsFallback) {
        if (currentManager instanceof DefaultManager) return null;
        Manager manager = new DefaultManager(libraryLoader, useAsFallback);
        if (setManager(manager)) return manager;
        return null;
    }

    private static final class SetMangerCallTrace extends Throwable {

        private SetMangerCallTrace(String s) {
            super(s);
        }
    }

    public static final synchronized boolean setManager(Manager manager) {
        if (currentManager != null) {
            if (currentManager == manager) return true;
            throw new IllegalStateException("JPfm.Manager already set as " + currentManager + " trying to change with " + manager);
        }
        if (manager == null) throw new NullPointerException("JPfm.Manager cannot be set to null");
        currentManager = manager;
        Mounts.setMountProvider(JPFM_SINGLETON, manager.getMountProvider(JPFM_SINGLETON));
        FSUtils.setServiceProvider(JPFM_SINGLETON, manager.getUtilityServiceProvider(JPFM_SINGLETON));
        FormatterListener.setManager(JPFM_SINGLETON, manager);
        boolean loadingSuccessful = currentManager.loadLibrary(JPFM_SINGLETON);
        if (loadingSuccessful) {
            manager.getLogger().log(Level.FINE, "JPfm.setManager call trace", new SetMangerCallTrace("JPfm.setManager call trace"));
            return true;
        }
        currentManager.dispose(JPFM_SINGLETON);
        currentManager = null;
        return false;
    }

    /**
     * A JPfm.Manager is responsible for loading the required
     * native libraries or other associated resources.
     * JPfm.Manager also binds the API to the service provider.
     * So running on any of {@link JPfm#getOperatingSystemsSupportedByDefaultImplementation() }
     * users do not need to set a custom manager.
     * <br/>
     * In general case one would write the following before using
     * JPfm library :
     * <pre>
     * JPfm.Manager manager = JPfm.setDefaultManager();
     * </pre>
     * The above line is required only once.
     * After this has been called {@link jpfm.mount.Mounts} class should be usable.
     * What this command does is, it delegates all responsibility of loading appropriate
     * native dynamic library to the default implementation.
     * DefaultManager expects to find appropriate native library in the
     * same folder in which jpfm.jar resides (
     * example name : jpfm_amd64_rev113.dll ,
     * jpfm_x86_rev113.dll , libjpfm_amd64_rev113.so, libjpfm_x86_rev113.so ,
     * libjpfm_amd64_rev113.dylib , libjpfm_x86_rev113.dylib )
     * <br/>
     * Optionally you may wish to write a fallback mechanism for this , example :
     * <pre>
     * if(JPfm.setDefaultManager()==null){
     *      JPfm.setManager(
     *          // instance of a custon implementation of Manager
     *          // by you.
     *      );
     * }else {
     *      // implies success
     * }
     * </pre>
     * <br/>
     * <br/>
     * The Manager instance is often used to protect privileged tasks.
     * For example:
     * {@link jpfm.mount.Mount#getThreadGroup(jpfm.JPfm.Manager) } requires
     * an instance of the manager to be passed. The default implementation
     * would check if the the manager is same as the one in use, and only
     * then will it allow access to the thread group of the java threads
     * working at the native level for the filesystem.
     * The reason we do this is to protect implementors from using deprecated
     * functions likes {@link Thread#stop()} and causing problem, which might
     * even have adverse effects at the kernel level. <br/>
     * The functions that are useful only for profiling the filesystem,
     * or debugging the JPfm default implementation are protected using this
     * way. Those functions would probably be useless for normal api users.
     * <br/><br/>
     * You can also add a library compiled by yourself by copying it same folder
     * in which jpfm.jar resides if there isn't one for the desired platform.
     * For now we intend to support : x86 & amd64 (linux, windows and mac)
     * and ppc & ppc64  for mac . See {@link DefaultManager.LibraryLoader } <br/>
     * JPfm native libraries should be buildable on any platform that supports
     * FUSE (like even opensolaris and even on sparc architecture ) or a
     * derivative of FreeBSD.
     * <br/>
     * <br/>
     * <small>
     * <u>For people who want to implement a custom service provider</u><br/>
     * Should have a look at the source code, specially src/jpfm/Impl.java .
     * For more information contact shashaanktulsyan@gmail.com
     * </small>
     */
    public static interface Manager {

        public boolean canSupport(OperatingSystem operatingSystem);

        /**
         * Load native libraries required by jpfm.
         * @throws SecurityException
         * @throws UnsatisfiedLinkError
         * @throws NullPointerException
         */
        public boolean loadLibrary(JPfm caller) throws SecurityException, UnsatisfiedLinkError, NullPointerException;

        /**
         * Used by this manager itself.
         * @return
         */
        public Logger getLogger();

        /**
         * Called when the manager is no longer required and must
         * free any resources it allocated.  <br/>
         * This should not be relied upon for deleting extracted native libraries.
         * @param caller The JPfm instance calling this dispose method.
         * If null implies someone else is trying to dispose this manager,
         * the manager should throw an exception in this case.
         */
        public void dispose(JPfm caller);

        public MountProvider getMountProvider(JPfm caller);

        public UtilityServiceProvider getUtilityServiceProvider(JPfm caller);

        /**
         * @return last exception occurred. The previous value is cleared after this is called
         */
        public Throwable getLastException();
    }

    public static final class DefaultManager implements Manager {

        private static final Logger LOGGER = Logger.getLogger(Math.random() + "-" + DefaultManager.class.getName() + "-" + Math.random());

        private static final String BIN_PATH = "/jpfm/nativelibraries/";

        private String externalLibaryPath = null;

        private final LibraryLoader libraryLoader;

        private final boolean useAsFallback;

        private Throwable lastException_my_copy = null;

        private DefaultManager() {
            this.libraryLoader = null;
            useAsFallback = false;
            LOGGER.setLevel(Level.ALL);
        }

        private DefaultManager(LibraryLoader libraryLoader, boolean useAsFallback) {
            this.libraryLoader = libraryLoader;
            this.useAsFallback = useAsFallback;
            LOGGER.setLevel(Level.ALL);
        }

        public Throwable getLastException() {
            Throwable toRet = lastException_my_copy;
            lastException_my_copy = null;
            return toRet;
        }

        public UtilityServiceProvider getUtilityServiceProvider(JPfm caller) {
            if (caller == null) {
                throw new IllegalArgumentException("Only valid instance of JPfm can access this function.");
            }
            return Impl.DefaultSP.INSTANCE;
        }

        /**
         * For those who want to load alternate version of native libraries
         * or have a custom mechanism for doing this for some reason,
         * can call {@link JPfm#setDefaultManager(jpfm.JPfm.DefaultManager.LibraryLoader) }
         * can pass an instance of this. The DefaultManager will do basic checks after calling
         * {@link LibraryLoader#loadLibrary(java.util.logging.Logger)  } function on this. If it fails failure would be reported to JPfm.
         */
        public static interface LibraryLoader {

            /**
             * @return true if and only if succeeded in loading native libraries
             */
            public boolean loadLibrary(Logger logger);
        }

        private static DefaultManager INSTANCE = new DefaultManager();

        private static DefaultManager getDefaultManager() {
            return INSTANCE;
        }

        public boolean canSupport(OperatingSystem operatingSystem) {
            OperatingSystem[] oses = getOperatingSystemsSupportedByDefaultImplementation();
            for (OperatingSystem os : oses) {
                if (operatingSystem == os) return true;
                if (os.getName().equalsIgnoreCase(operatingSystem.getName())) return true;
            }
            return false;
        }

        private boolean customLibraryLoader() {
            synchronized (this) {
                LOGGER.log(Level.INFO, "Using custom libraryLoader {0} ", libraryLoader);
                libraryLoader.loadLibrary(LOGGER);
                try {
                    JPfmMount.performPreMountingChecks();
                } catch (MountException me) {
                    LOGGER.log(Level.SEVERE, "Perform premounting checks failed ", me);
                    lastException_my_copy = me;
                    lastException_static = me;
                    return false;
                } catch (NoClassDefFoundError ex) {
                    LOGGER.log(Level.SEVERE, "", ex);
                    lastException_my_copy = ex;
                    lastException_static = ex;
                    return false;
                } catch (NoSuchMethodError ex) {
                    LOGGER.log(Level.SEVERE, "A java function required by the native library was not found.\n" + " The version of classes and native library might be different," + " or the class might have been modified externally.", ex);
                    lastException_my_copy = ex;
                    lastException_static = ex;
                    return false;
                }
                LOGGER.fine("success");
            }
            return true;
        }

        @Override
        public boolean loadLibrary(JPfm caller) throws SecurityException, UnsatisfiedLinkError, NullPointerException {
            if (libraryLoader != null && !useAsFallback) {
                return customLibraryLoader();
            }
            synchronized (this) {
                String libraryName = generateLibraryName();
                if (!useLibraryPresentOutsideJarFile(libraryName)) {
                    LOGGER.info("loading native library using custom loader");
                    if (useAsFallback) {
                        if (!customLibraryLoader()) {
                            LOGGER.info("Custom library loader failed");
                            return false;
                        }
                    } else {
                        LOGGER.info("Could not find native libraries outside jar, and no fallback LibraryLoader is available.");
                        return false;
                    }
                } else {
                    LOGGER.fine("loading native library");
                    System.load(externalLibaryPath);
                    LOGGER.info("Using native library : " + externalLibaryPath);
                }
                LOGGER.fine("loading native library successful.");
                LOGGER.fine("Performing pre mounting checks");
                try {
                    JPfmMount.performPreMountingChecks();
                } catch (MountException me) {
                    LOGGER.log(Level.SEVERE, "Perform premounting checks failed ", me);
                    lastException_my_copy = me;
                    lastException_static = me;
                    return false;
                } catch (NoClassDefFoundError ex) {
                    lastException_my_copy = ex;
                    lastException_static = ex;
                    return false;
                } catch (NoSuchMethodError ex) {
                    LOGGER.log(Level.SEVERE, "A java function required by the native library was not found.\n" + " The version of classes and native library might be different," + " or the class might have been modified externally.", ex);
                    lastException_my_copy = ex;
                    lastException_static = ex;
                    return false;
                }
                LOGGER.fine("success");
            }
            return true;
        }

        private boolean useLibraryPresentOutsideJarFile(String libraryName) {
            String fullLibraryPath = JPfm.class.getProtectionDomain().getCodeSource().getLocation().toString();
            if (!fullLibraryPath.endsWith(".jar")) {
                LOGGER.log(Level.INFO, "Running in development mode. Native libraries not found.\nPath : " + fullLibraryPath + "\nTrying to guess location");
                fullLibraryPath = fullLibraryPath.substring(0, fullLibraryPath.lastIndexOf('/'));
                fullLibraryPath = fullLibraryPath.substring(0, fullLibraryPath.lastIndexOf('/'));
                fullLibraryPath = fullLibraryPath.substring(0, fullLibraryPath.lastIndexOf('/') + 1);
                fullLibraryPath = fullLibraryPath + "resources/";
            }
            fullLibraryPath = fullLibraryPath.substring(0, fullLibraryPath.lastIndexOf('/') + 1);
            fullLibraryPath = fullLibraryPath + libraryName;
            try {
                java.io.File externalLibaryFile = new java.io.File(new java.net.URL(fullLibraryPath).toURI());
                externalLibaryPath = externalLibaryFile.getAbsolutePath();
                if (!externalLibaryFile.exists()) throw new FileNotFoundException(externalLibaryPath);
            } catch (Exception ule) {
                LOGGER.log(Level.INFO, "Native libraries not found.\nPath : " + fullLibraryPath + "\n", ule);
                externalLibaryPath = browseLibaryPathGUI(libraryName);
                if (externalLibaryPath == null) {
                    LOGGER.severe("Could not load native library, giving up");
                }
                return true;
            }
            return true;
        }

        private String browseLibaryPathGUI(final String libraryName) {
            javax.swing.JOptionPane.showMessageDialog(null, "Please browse the location of " + libraryName, "Could not find native libary", javax.swing.JOptionPane.ERROR_MESSAGE);
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser(libraryName);
            fileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

                @Override
                public boolean accept(File f) {
                    return (f.getName().equalsIgnoreCase(libraryName)) || f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return libraryName;
                }
            });
            int retVal = fileChooser.showOpenDialog(null);
            if (retVal == javax.swing.JFileChooser.APPROVE_OPTION) {
                return fileChooser.getSelectedFile().getAbsoluteFile().getPath();
            } else {
                return null;
            }
        }

        private boolean useLibraryPresentInsideJarFile(String libraryName) {
            InputStream libraryStream = JPfm.class.getResourceAsStream(BIN_PATH + libraryName);
            if (libraryStream == null) {
                RuntimeException exception = new RuntimeException("Cannot find the native library {" + libraryName + "} in the expected location in this jar file");
                LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
                return false;
            }
            LOGGER.log(Level.INFO, "Choosing native library {0}", libraryName);
            if (!writeLibrary("", libraryName, libraryStream)) {
                LOGGER.log(Level.INFO, "failed writing file, check logs for detail");
                return false;
            }
            String repeatedSeparator = File.separator + File.separator;
            while (externalLibaryPath.contains(repeatedSeparator)) externalLibaryPath = externalLibaryPath.replace(repeatedSeparator, File.separator);
            return true;
        }

        /**
         * @param  path System.getProperty("java.io.tmpdir") + File.separatorChar + path + File.separatorChar + name
         */
        private boolean writeLibrary(String path, String name, InputStream libraryStream) {
            try {
                externalLibaryPath = System.getProperty("java.io.tmpdir") + File.separatorChar + path + File.separatorChar + name;
                File fileOut = new File(externalLibaryPath);
                boolean alreadyExists = false;
                if (fileOut.exists()) {
                    LOGGER.log(Level.INFO, "Native library already exists, overwritting : {0}", fileOut.getAbsolutePath());
                    alreadyExists = true;
                }
                LOGGER.log(Level.INFO, "Writing native library to: {0}", fileOut.getAbsolutePath());
                try {
                    fileOut.createNewFile();
                    FileChannel libraryFileChannel = new FileOutputStream(fileOut).getChannel();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = libraryStream.read(buf)) > 0) {
                        libraryFileChannel.write(ByteBuffer.wrap(buf, 0, len));
                    }
                    libraryFileChannel.force(true);
                    libraryFileChannel.close();
                } catch (Exception any) {
                    LOGGER.log(Level.INFO, "Exception occured while attempting to write native library", any);
                    libraryStream.close();
                    if (alreadyExists) return true;
                    return false;
                }
                libraryStream.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load required system library " + name, e);
                return false;
            }
            LOGGER.fine("Finshed writing native library file");
            return true;
        }

        public Logger getLogger() {
            return LOGGER;
        }

        /**
         * This is part of the api. 
         * @return name of the library which should be loaded
         */
        public static String generateLibraryName() {
            StringBuilder sb = new StringBuilder(23);
            if (!SystemUtils.IS_OS_WINDOWS) sb.append("lib");
            sb.append("jpfm_");
            MachineUtils.MachineType thisMachineType = MachineUtils.getRuntimeSystemArchitecture();
            if (thisMachineType == MachineUtils.MachineType.UNKNOWN) {
                LOGGER.log(Level.WARNING, "Cannot determine machine architecture {0} assuming x86", System.getProperty("os.arch"));
                sb.append(MachineUtils.MachineType.X86.name().toLowerCase());
            } else {
                sb.append(thisMachineType.name().toLowerCase());
            }
            sb.append("_rev");
            sb.append(Integer.toString(getJPfmVersion()));
            LOGGER.info(SystemUtils.OS_NAME);
            if (SystemUtils.IS_OS_WINDOWS) sb.append(".dll"); else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_DARWIN) sb.append(".dylib"); else sb.append(".so");
            return sb.toString();
        }

        /**
         *
         * @return
         */
        public static String generateLibraryNameWithoutExtension() {
            StringBuilder sb = new StringBuilder(23);
            sb.append("jpfm_");
            MachineUtils.MachineType thisMachineType = MachineUtils.getRuntimeSystemArchitecture();
            if (thisMachineType == MachineUtils.MachineType.UNKNOWN) {
                LOGGER.log(Level.WARNING, "Cannot determine machine architecture {0} assuming x86", System.getProperty("os.arch"));
                sb.append(MachineUtils.MachineType.X86.name().toLowerCase());
            } else {
                sb.append(thisMachineType.name().toLowerCase());
            }
            sb.append("_rev");
            sb.append(Integer.toString(getJPfmVersion()));
            LOGGER.info(SystemUtils.OS_NAME);
            return sb.toString();
        }

        public void dispose(JPfm caller) {
            if (caller == null) {
                throw new IllegalArgumentException("Only valid instance of JPfm can access this function.");
            }
            if (true) return;
            LOGGER.info("Disposing manager");
            synchronized (this) {
                try {
                    File f = new File(externalLibaryPath);
                    if (f.exists()) {
                        f.delete();
                        LOGGER.log(Level.INFO, "{0} native library file deleted successfully.", externalLibaryPath);
                    } else {
                        return;
                    }
                } catch (Exception any) {
                    LOGGER.log(Level.INFO, "failed to delete extracted native libaries", any);
                }
            }
        }

        public MountProvider getMountProvider(JPfm caller) {
            if (caller == null) {
                throw new IllegalArgumentException("Only valid instance of JPfm can access this function.");
            }
            return Impl.DefaultMountProvider.INSTANCE;
        }
    }

    /**
     * just for testing, calling this is of no use.
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (true) throw new Exception("DO NOT CALL ME");
        JPfm.setManager(new DefaultManager(new DefaultManager.LibraryLoader() {

            @Override
            public boolean loadLibrary(Logger logger) {
                System.load("F:\\neembuu\\nbvfs_native\\jpfm\\dist\\Debug\\MinGW-Windows\\libjpfm.dll");
                try {
                    System.err.println("attach");
                    System.in.read();
                } catch (Exception e) {
                }
                return true;
            }
        }, true));
        System.out.println("getPismoVersionResult=" + JPfmMount.getPismoVersion());
        DefaultManager.getDefaultManager().dispose(JPFM_SINGLETON);
    }
}
