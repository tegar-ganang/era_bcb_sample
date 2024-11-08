package protoj.lang.archive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.listener.Log4jListener;
import org.apache.tools.ant.taskdefs.Untar;
import org.aspectj.lang.SoftException;
import protoj.lang.ArgRunnable;
import protoj.lang.ProjectLayout;
import protoj.lang.StandardProject;
import protoj.lang.StandardProjectComponentPolicy.StandardProjectComponent;
import protoj.lang.archive.ClassesArchive.ClassesEntry;

/**
 * Provides support for the creation of source, javadoc, classes jar files,
 * project tar file and a self extracting project archive. This class is an
 * aggregate of implementation objects so see the {@link SourceArchive},
 * {@link JavadocArchive}, {@link ClassesArchive} and {@link ProjectArchive}
 * classes for more information.
 * 
 * @author Ashley Williams
 * 
 */
@StandardProjectComponent
public final class ArchiveFeature {

    /**
	 * The name of the tar gz file created with
	 * {@link #createSelfExtractingArchive(boolean, boolean, boolean)}.
	 */
    private static final String PROJECT_DIST_ARCHIVE = "project-dist";

    /**
	 * The name of the self extracting archive, see
	 * {@link #initSelfExtractingArchive(String, String, String, String, String)}
	 * .
	 */
    private String extractingArchiveName;

    /**
	 * Enables the creation of a self extracting jar file with the name
	 * specified by the extractingArchiveName argument and whose contents are a
	 * compressed tar of the whole project. The resultant archive is executable
	 * and so when executed will extract the project into the same directory.
	 * <p>
	 * This works by initializing the project archive with a call to
	 * {@link #MISSING()} and configuring a new classes archive with a call to
	 * {@link ClassesArchive#addEntry(String, String, String, String)} and
	 * coordinating between the two. Therefore it is advisable not to separately
	 * call initProjectArchive.
	 * 
	 * 
	 * @param extractingArchiveName
	 *            the name of the archive
	 * @param jarManifest
	 *            the name of the manifest, null to accept default
	 * @param tarPrefix
	 *            the name of the root directory when the tar is extracted
	 * @param tarUserName
	 *            the user name for the tar entries
	 * @param tarGroup
	 *            the group for the tar entries
	 */
    public void initSelfExtractingArchive(String extractingArchiveName, String jarManifest, String tarPrefix, String tarUserName, String tarGroup) {
        this.extractingArchiveName = extractingArchiveName;
        getParent().initProjectArchive(PROJECT_DIST_ARCHIVE, tarPrefix, tarUserName, tarGroup);
        getParent().getClassesArchive().addEntry(extractingArchiveName, jarManifest, null, "**/*");
        getParent().getClassesArchive().getEntry(extractingArchiveName).initConfig(new ArgRunnable<ClassesArchive>() {

            public void run(ClassesArchive archive) {
                archive.initExecutableJar(ArchiveFeature.class.getName());
            }
        });
        getParent().getClassesArchive().initIncludeArchives(extractingArchiveName, getLayout().getLocation(Task.class).getName(), getLayout().getLocation(AntMain.class).getName(), getLayout().getLocation(Logger.class).getName(), getLayout().getLocation(IOUtils.class).getName(), getLayout().getLocation(StandardProject.class).getName(), getLayout().getLocation(SoftException.class).getName());
    }

    /**
	 * Creates a self extracting archive of the project as initialized in
	 * {@link #initSelfExtractingArchive(String, String, String, String, String)}
	 * .
	 * 
	 * <ol>
	 * <li>the first party jars are updated in the lib directory for those
	 * archives that are supposed to be on the classpath - see
	 * {@link ClassesArchive#initClasspathLib(String)}</li>
	 * <li>the project tar file is created in the classes directory - see
	 * {@link ProjectArchive#createArchive(boolean, boolean, boolean)} for an
	 * explanation of the tar parameters below</li>
	 * <li>the self extracting jar is created from the files in the classes
	 * directory, including importantly the project tar file just mentioned</li>
	 * <li></li>
	 * <li></li>
	 * </ol>
	 * 
	 * @param noSrc
	 * @param noClasses
	 * @param isGlobalRwx
	 */
    public void createSelfExtractingArchive(boolean noSrc, boolean noClasses, boolean isGlobalRwx) {
        File srcFile = getParent().getProjectArchive().getArchiveFile();
        File destFile = new File(getLayout().getClassesDir(), srcFile.getName());
        destFile.delete();
        final ClassesArchive classesArchive = getParent().getClassesArchive();
        classesArchive.visit(new ArgRunnable<ClassesEntry>() {

            public void run(ClassesEntry entry) {
                if (entry.isClasspathLib()) {
                    String name = entry.getArchiveEntry().getName();
                    classesArchive.createArchive(name);
                }
            }
        });
        getParent().getProjectArchive().createArchive(noSrc, noClasses, isGlobalRwx);
        FileUtils.copyFile(srcFile, destFile);
        getParent().getClassesArchive().createArchive(extractingArchiveName);
    }

    /**
	 * Deletes the target/ and classes/ directories as well as any jars that
	 * have been copied to the lib/ directory. These are identified by finding
	 * those classes archive entries that were configured with a call to
	 * {@link ClassesArchive#initClasspathLib(String)}.
	 */
    public void clean() {
        FileUtils.deleteDirectory(getLayout().getTargetDir());
        FileUtils.deleteDirectory(getLayout().getClassesDir());
        getParent().getClassesArchive().visit(new ArgRunnable<ClassesEntry>() {

            public void run(ClassesEntry entry) {
                if (entry.isClasspathLib()) {
                    String name = entry.getArchiveEntry().getName();
                    File file = new File(getLayout().getLibDir(), name);
                    FileUtils.deleteDirectory(file);
                }
            }
        });
    }

    /**
	 * Returns whether or not the given jar name represents a valid classes jar
	 * file. This is a jar file that doesn't contain the strings
	 * {@link ProjectLayout#getSourcePostfix()},
	 * {@link ProjectLayout#getJavadocPostfix()} or
	 * {@link ProjectLayout#getSrcPostfix()}.
	 * 
	 * @param name
	 * @return
	 */
    public boolean isClassesJar(String name) {
        return name.endsWith(".jar") && !isSourcesJar(name) && !isJavadocJar(name);
    }

    /**
	 * Returns whether or not the given jar name represents a valid javadoc jar
	 * file. This is a jar file that contain the string
	 * {@link ProjectLayout#getJavadocPostfix()}.
	 * 
	 * @param name
	 * @return
	 */
    public boolean isJavadocJar(String name) {
        return name.endsWith(".jar") && name.contains(getLayout().getJavadocPostfix());
    }

    /**
	 * Returns whether or not the given jar name represents a valid sources jar
	 * file. This is a jar file that contains either of the strings
	 * {@link ProjectLayout#getSourcePostfix()} or
	 * {@link ProjectLayout#getSrcPostfix()}.
	 * 
	 * @param name
	 * @return
	 */
    public boolean isSourcesJar(String name) {
        return name.endsWith(".jar") && (name.contains(getLayout().getSourcePostfix()) || name.contains(getLayout().getSrcPostfix()));
    }

    /**
	 * The parent layout for convenient access.
	 * 
	 * @return
	 */
    public ProjectLayout getLayout() {
        return getParent().getLayout();
    }

    /**
	 * Extracts the project contained in this executable jar. This use-case is
	 * therefore only available when called from the executable jar created in
	 * the first place with a call to
	 * {@link ArchiveFeature#createSelfExtractingArchive(boolean, boolean, boolean)}
	 * .
	 */
    public static void main(String[] args) {
        File container = new File(ArchiveFeature.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (container == null) {
            throw new RuntimeException("this use-case isn't being invoked from the executable jar");
        }
        JarFile jarFile = new JarFile(container);
        String artifactName = PROJECT_DIST_ARCHIVE + ".tar.gz";
        File artifactFile = new File(".", artifactName);
        ZipEntry artifactEntry = jarFile.getEntry(artifactName);
        InputStream source = jarFile.getInputStream(artifactEntry);
        try {
            FileOutputStream dest = new FileOutputStream(artifactFile);
            try {
                IOUtils.copy(source, dest);
            } finally {
                IOUtils.closeQuietly(dest);
            }
        } finally {
            IOUtils.closeQuietly(source);
        }
        Project project = new Project();
        project.setName("project");
        project.init();
        Target target = new Target();
        target.setName("target");
        project.addTarget(target);
        project.addBuildListener(new Log4jListener());
        Untar untar = new Untar();
        untar.setTaskName("untar");
        untar.setSrc(artifactFile);
        untar.setDest(new File("."));
        Untar.UntarCompressionMethod method = new Untar.UntarCompressionMethod();
        method.setValue("gzip");
        untar.setCompression(method);
        untar.setProject(project);
        untar.setOwningTarget(target);
        target.addTask(untar);
        untar.execute();
    }
}
