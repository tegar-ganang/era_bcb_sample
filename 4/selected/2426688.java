package protoj.lang.archive;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import protoj.lang.ArgRunnable;
import protoj.lang.ProjectLayout;
import protoj.lang.StandardProject;
import protoj.lang.StandardProjectComponentPolicy.StandardProjectComponent;
import protoj.lang.ant.JarWrapper;

/**
 * Responsible for creating one or more archives from the project classes
 * directory. Call {@link #addEntry(String, String, String, String)} to
 * configure each additional archive with the specified name and
 * {@link #createArchive(String)} to create one of the added archive. During
 * creation, the <code>config</code> argument specified in the constructor will
 * be called back to give the caller a chance to provide further archive
 * configuration. Also call {@link #initIncludeArchives(String, String...)} to
 * cause any additional archives from the {@link ProjectLayout#getLibDir()} to
 * be merged with the added archive.
 * 
 * @author Ashley Williams
 * 
 */
@StandardProjectComponent
public final class ClassesArchive {

    /**
	 * Adds classes specific information to a wrapped {@link ArchiveEntry}
	 * instance.
	 * 
	 * @author Ashley Williams
	 * 
	 */
    public static final class ClassesEntry {

        /**
		 * See {@link #getArchiveEntry()}.
		 */
        private ArchiveEntry<ClassesArchive> archiveEntry;

        /**
		 * See {@link #getPomResource()}.
		 */
        private String pomResource;

        /**
		 * See {@link #getGpgOptions()}.
		 */
        private String gpgOptions;

        private boolean isClasspathLib;

        /**
		 * See the {@link ArchiveEntry} accessors.
		 * 
		 * @param parent
		 * @param name
		 * @param fileName
		 * @param manifest
		 * @param includes
		 * @param excludes
		 */
        public ClassesEntry(StandardProject parent, String name, String fileName, String manifest, String includes, String excludes) {
            this.archiveEntry = new ArchiveEntry<ClassesArchive>(name, parent, fileName, manifest, includes, excludes);
            this.isClasspathLib = false;
        }

        /**
		 * Pass-thru method to {@link ArchiveEntry#initConfig(ArgRunnable)}.
		 * 
		 * @param config
		 */
        public void initConfig(ArgRunnable<ClassesArchive> config) {
            archiveEntry.initConfig(config);
        }

        /**
		 * Call this method if the archive should be copied to the lib/
		 * directory after creation. If this isn't called then this archive
		 * won't be copied.
		 */
        public void initClasspathLib() {
            this.isClasspathLib = true;
        }

        /**
		 * Specifies the information used in publishing the artifact. Javadoc
		 * and source archives of the same name will automatically get attached
		 * and published so there is no equivalent method on those.
		 * <p>
		 * The pomResource is used to specify the location on the classpath of
		 * the maven pom file describing the artifact to be published, for
		 * example "/proj/pom-pub.xml". Veolcity markup for interacting with the
		 * domain objects can be specified in in order to avoid hardcoded
		 * values.
		 * <p>
		 * If the gpg executable is available to the operating system then
		 * options can be specified to produce the ascii armored detached
		 * signature file to also be published. Simply specify a string that
		 * will be passed to String.format() whose first parameter is the name
		 * of the file to be signed and the second parameter is the name of the
		 * signature file to be created. Here is an example:
		 * 
		 * <pre>
		 * &quot;--armor --output %2$s --detach-sign %1$s&quot;
		 * </pre>
		 * 
		 * The publish feature will correctly pass in the two arguments for each
		 * archive. Hint: to supply additional options such as --local-user,
		 * read in a system property when composing the string.
		 * 
		 * @param pomResource
		 *            the name of the classpath pom resource used to publish the
		 *            artifact.
		 * @param gpgOptions
		 *            the options to the gpg executable that will generate the
		 *            ascii armored detached signature file, or null if signing
		 *            isn't required.
		 */
        public void initPublish(String pomResource, String gpgOptions) {
            this.pomResource = pomResource;
            this.gpgOptions = gpgOptions;
        }

        /**
		 * The maven pom resource on the classpath used to publish the artifact
		 * in this entry. See {@link #initPublish(String, String)} for more
		 * information.
		 * 
		 * @return
		 */
        public String getPomResource() {
            return pomResource;
        }

        /**
		 * The options to gpg responsible for creating the ascii armored
		 * detached signature file. See {@link #initPublish(String, String)} for
		 * more information.
		 * 
		 * @return
		 */
        public String getGpgOptions() {
            return gpgOptions;
        }

        /**
		 * The wrapped helper.
		 * 
		 * @return
		 */
        public ArchiveEntry<ClassesArchive> getArchiveEntry() {
            return archiveEntry;
        }

        /**
		 * True if the generated jar is to be copied to the lib/ directory,
		 * false otherwise.
		 * 
		 * @return
		 */
        public boolean isClasspathLib() {
            return isClasspathLib;
        }
    }

    /**
	 * The configuration information for each added archive.
	 */
    private TreeMap<String, ClassesEntry> entries = new TreeMap<String, ClassesEntry>();

    /**
	 * See {@link #getCurrentAssembleTask()}.
	 */
    private JarWrapper currentAssembleTask;

    /**
	 * See {@link #getCurrentEntry()}.
	 */
    private ArchiveEntry<ClassesArchive> currentEntry;

    /**
	 * Use to enable creation of an additional archive.
	 * 
	 * @param name
	 *            the name of the jar file to be created, without the extension.
	 * @param manifest
	 *            the name of the manifest to be used from the manifest
	 *            directory, without the extension. Can be null if a default
	 *            manifest is required.
	 * @param includes
	 *            the resources that should be included in the archive, can be
	 *            null to include all resources
	 * @param excludes
	 *            the resources that should be excluded in the archive, can be
	 *            null to exclude no resources
	 */
    public void addEntry(String name, String manifest, String includes, String excludes) {
        String fileName = name + ".jar";
        ClassesEntry entry = new ClassesEntry(getParent(), name, fileName, manifest, includes, excludes);
        entries.put(name, entry);
    }

    /**
	 * Convenience method that schedules the creation of a single jar file with
	 * the given name. See
	 * {@link ClassesArchive#addEntry(String, String, String, String)} .
	 * 
	 * @param name
	 */
    public void addEntry(String name) {
        addEntry(name, null, null, null);
    }

    /**
	 * Pass-thru method to {@link ArchiveEntry#initConfig(ArgRunnable)}.
	 * 
	 * @param config
	 */
    public void initConfig(String name, ArgRunnable<ClassesArchive> config) {
        getEntry(name).initConfig(config);
    }

    /**
	 * Call this method if publishing to a maven repository is required.
	 * Pass-thru method to {@link ClassesEntry#initPublish(String, String)}.
	 * 
	 * @param name
	 * @param pomName
	 * @param gpgOptions
	 */
    public void initPublish(String name, String pomName, String gpgOptions) {
        getEntry(name).initPublish(pomName, gpgOptions);
    }

    /**
	 * Call this method if the archive should be copied to the lib/ directory
	 * after creation. Pass-thru method to
	 * {@link ClassesEntry#initClasspathLib()}.
	 * 
	 * @param name
	 */
    public void initClasspathLib(String name) {
        getEntry(name).initClasspathLib();
    }

    /**
	 * Pass-thru method that ensures the specified archives are merged to the
	 * named archive. See {@link ArchiveEntry#initMergeArchives(String...)}.
	 * 
	 * @param name
	 * @param archives
	 */
    public void initIncludeArchives(String name, String... archives) {
        getEntry(name).getArchiveEntry().initMergeArchives(archives);
    }

    /**
	 * Specifies that all the classes archives except for the excluded list in
	 * the {@link ProjectLayout#getLibDir()} should get merged during creation
	 * of the named archive.
	 * 
	 * @param name
	 */
    public void initExcludeArchives(String name, String... archives) {
        final List<String> excluded = Arrays.asList(archives);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return !excluded.contains(name) && getParent().getArchiveFeature().isClassesJar(name);
            }
        };
        getEntry(name).getArchiveEntry().initMergeArchives(filter);
    }

    /**
	 * Delegates to {@link #createArchive(String)} for each archive that was
	 * added through a call to {@link #addEntry(String, String, String, String)}
	 * .
	 */
    public void createArchives() {
        Set<String> keys = entries.keySet();
        for (String key : keys) {
            createArchive(key);
        }
    }

    /**
	 * Creates the jar for the given name under
	 * {@link ProjectLayout#getArchiveDir()}. The jar file will have a name of
	 * [name].jar. Additionally copies the jar to the lib directory if it is a
	 * classpath lib, i.e. initClasspathLib() has been called.
	 * 
	 * @param name
	 *            the name as specified in the call to
	 *            {@link #addEntry(String, String, String, String)} .
	 */
    public void createArchive(String name) {
        ProjectLayout layout = getParent().getLayout();
        currentEntry = getEntry(name).getArchiveEntry();
        currentAssembleTask = currentEntry.createAssembleTask(layout.getClassesDir());
        ArgRunnable<ClassesArchive> config = currentEntry.getConfig();
        if (config != null) {
            config.run(this);
        }
        currentAssembleTask.getJar().execute();
        boolean isClasspathLib = getEntry(name).isClasspathLib();
        if (isClasspathLib) {
            FileUtils.copyFileToDirectory(currentEntry.getArtifact(), layout.getLibDir());
        }
    }

    /**
	 * Accessor for the entry corresponding to the given name.
	 * 
	 * @param name
	 * @return
	 */
    public ClassesEntry getEntry(String name) {
        return entries.get(name);
    }

    /**
	 * Convenience method that delegates to {@link #initExecutableJar(String)},
	 * but using the current thread main class by default.
	 */
    public void initExecutableJar() {
        String currentMainClass = getParent().getCurrentMainClass();
        initExecutableJar(currentMainClass);
    }

    /**
	 * Marks the archive as an executable jar with the specified mainClass.
	 * Works in conjunction with the
	 * {@link #initIncludeArchives(String, String...)} method, since any archive
	 * that is to be merged won't appear in the Class-Path attribute value.
	 * <p>
	 * This should be invoked on the callback to the config parameter specified
	 * in the call to {@link #addEntry(String, String, String, String)}. Ensures
	 * the given ant assemble task is configured to create an executable jar.
	 * The jar files in the lib directory are used to form the Class-Path string
	 * in the manifest file.
	 * 
	 * @param mainClass
	 */
    public void initExecutableJar(String mainClass) {
        StringBuilder classPath = new StringBuilder();
        String[] libFiles = getParent().getLayout().getLibDir().list();
        List<String> mergeArchives = getCurrentEntry().getMergeArchives();
        for (String libFile : libFiles) {
            boolean isClassesJar = getParent().getArchiveFeature().isClassesJar(libFile);
            boolean isMergeArchive = mergeArchives.contains(libFile);
            if (isClassesJar && !isMergeArchive) {
                classPath.append(libFile);
                classPath.append(" ");
            }
        }
        getCurrentAssembleTask().initManifest("Main-Class", mainClass);
        getCurrentAssembleTask().initManifest("Class-Path", classPath.toString());
    }

    /**
	 * Convenient method that visits each entry in this archive.
	 * 
	 * @param visitor
	 */
    public void visit(ArgRunnable<ClassesEntry> visitor) {
        Collection<ClassesEntry> values = entries.values();
        for (ClassesEntry entry : values) {
            visitor.run(entry);
        }
    }

    /**
	 * The ant task responsible for creating the jar during the call to
	 * {@link #createArchive(String)}.
	 * 
	 * @return
	 */
    public JarWrapper getCurrentAssembleTask() {
        return currentAssembleTask;
    }

    /**
	 * The instance used to hold information about the the archive being created
	 * during the call to {@link #createArchive(String)}.
	 * 
	 * @return
	 */
    public ArchiveEntry<ClassesArchive> getCurrentEntry() {
        return currentEntry;
    }
}
