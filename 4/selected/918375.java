package net.sf.maven.plugins.onejar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * @author SchubertT006
 * @goal 1jar
 * @phase package
 * @requiersProject
 * @requiresDependencyResolution runtime
 */
public class OneJarMojo extends AbstractMojo {

    protected final Log logger = getLog();

    /**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
    protected MavenProject project;

    /**
	 * Used for attaching the artifact in the project.
	 * 
	 * @component
	 * 
	 * @required
	 * @readonly
	 */
    protected MavenProjectHelper projectHelper;

    /**
	 * Name of the 1jar File.
	 * 
	 * @parameter expression="${project.build.finalName}.one-jar.jar"
	 * @required
	 * 
	 */
    protected String finalFileName;

    /**
	 * The directory for artifacts.
	 * 
	 * @parameter default-value="${project.build.directory}"
	 * @required
	 * @readonly
	 */
    protected File buildDirectory;

    /**
	 * All the dependencies including transient dependencies.
	 * 
	 * @parameter default-value="${project.artifacts}"
	 * @required
	 * @readonly
	 */
    protected Collection<Artifact> artifacts;

    /**
	 * Name of the main JAR.
	 * 
	 * @parameter expression="${project.build.finalName}.jar"
	 * @readonly
	 * @required
	 */
    protected String mainJarFilename;

    /**
	 * Implementation Version of the jar. Defaults to the build's version.
	 * 
	 * @parameter default-value="${project.version}"
	 * @required
	 */
    protected String implementationVersion;

    /**
	 * The main class that one-jar should activate
	 * 
	 * @parameter default-value="${one-jar.main.class}"
	 */
    protected String mainClass;

    /**
	 * Strip the version numbers of all libraries.
	 * 
	 * @parameter default-value="false"
	 */
    protected boolean stripVersion;

    /**
	 * Map to define a mapping of library names to target names.
	 * 
	 * @parameter
	 */
    protected Map<String, String> fileNameMap;

    /**
	 * The license must be include in the final jar
	 * 
	 * @parameter 
	 *            default-value="http://one-jar.sourceforge.net/one-jar-license.txt"
	 * @required
	 */
    protected URL onejarLicenseURL;

    /**
	 * Intern list to flag the dependency libraries which should be expand into
	 * the target jar.
	 * 
	 * @parameter
	 * 
	 */
    protected List<String> unpackJARs = new ArrayList<String>();

    /**
	 * Prints the License info to the logger output stream.
	 */
    protected void printLicenseInfo() {
        logger.info("1jar-maven-plugin:");
        logger.info("Plugin home: http://sf-mvn-plugins.sourceforge.net/1jar-maven-plugin");
        logger.info("License for One-Jar:  http://one-jar.sourceforge.net/one-jar-license.txt");
    }

    protected void printInfo(final String message) {
        logger.info(message);
    }

    protected void printError(final String message) {
        logger.error(message);
    }

    protected void printError(final Exception e) {
        logger.error(e);
    }

    protected void printDebug(final String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }

    protected void printDebug(final Exception e) {
        if (logger.isDebugEnabled()) {
            logger.debug(e);
        }
    }

    /**
	 * Looks for the next ZipEntry with given name and return a input stream of
	 * entry.
	 * 
	 * @param libFiles
	 *            Dependency List as files
	 * @return Last founded file "boot-manifest.mf" as Manifest
	 * @throws IOException
	 *             if occurred an error during the reading of zip stream
	 */
    protected Manifest extractOneJarManifestTemplate(final List<File> libFiles) throws IOException {
        Manifest manifest = null;
        final String ENTRY_NAME = "boot-manifest.mf";
        for (File libFile : libFiles) {
            final InputStream inStream = new FileInputStream(libFile);
            final JarInputStream jarInStream = new JarInputStream(inStream);
            final ZipInputStream zipInStream = jarInStream;
            try {
                ZipEntry entry = zipInStream.getNextEntry();
                while (entry != null && manifest == null) {
                    if (entry.getName().equals(ENTRY_NAME)) {
                        byte[] data = IOUtils.toByteArray(zipInStream);
                        final ByteArrayInputStream entryStream = new ByteArrayInputStream(data);
                        manifest = new Manifest(entryStream);
                        this.unpackJARs.add(libFile.getName());
                        break;
                    }
                    entry = zipInStream.getNextEntry();
                }
            } finally {
                IOUtils.closeQuietly(zipInStream);
            }
        }
        if (manifest == null) {
            logger.debug("CHECK if you have defined a dependency to a one-jar implementation -> you need this!!!");
            manifest = new Manifest();
        }
        return manifest;
    }

    /**
	 * @return
	 * @throws IOException
	 */
    protected Manifest createManifest(final List<File> libFiles) throws IOException {
        final Manifest manifest = extractOneJarManifestTemplate(libFiles);
        if (mainClass != null) {
            manifest.getMainAttributes().putValue("One-Jar-Main-Class", mainClass);
        }
        if (implementationVersion != null) {
            manifest.getMainAttributes().putValue("ImplementationVersion", implementationVersion);
        }
        return manifest;
    }

    /**
	 * Strip the version number if the plugin parameter stripVersion is set to
	 * true. Its only a simple cut from the last "-" until the end of filename.
	 * 
	 */
    protected String stripVersionNumber(final String rawFileName) {
        if (!stripVersion) {
            return rawFileName;
        }
        String fileName = rawFileName;
        String ext = fileName.substring(fileName.length() - 4);
        int pos = fileName.lastIndexOf("-");
        if (pos > -1) {
            fileName = fileName.substring(0, pos) + ext;
        }
        printDebug("add:" + fileName);
        return fileName;
    }

    protected String mapFileName(final String rawFileName) {
        String fileName = rawFileName;
        if (fileNameMap != null && fileNameMap.containsKey(rawFileName)) {
            fileName = fileNameMap.get(rawFileName);
            printDebug("map:" + rawFileName + " to:" + fileName);
        }
        return fileName;
    }

    protected void addEntryToZip(JarOutputStream out, ZipEntry entry, InputStream in) throws IOException {
        out.putNextEntry(entry);
        IOUtils.copy(in, out);
        out.closeEntry();
    }

    /**
	 * Add the given File to archive into given path. During this process the
	 * filename will be stripped form version and than mapped via hash map.
	 * (depend of stripVersion is true and hash map contains the file as key)
	 * 
	 * @param out
	 * @param zipfilePath
	 * @param sourceFile
	 * @throws IOException
	 */
    protected void addFileToZip(final JarOutputStream out, final String zipfilePath, final File sourceFile) throws IOException {
        final String strippedFileName = stripVersionNumber(sourceFile.getName());
        final String mappedFileName = mapFileName(strippedFileName);
        addEntryToZip(out, new ZipEntry(zipfilePath + mappedFileName), new FileInputStream(sourceFile));
    }

    /**
	 * Returns File objects for each parameter string.
	 * 
	 * @param artifacts
	 *            <em>(Must be non-<code>null</code>)</em>
	 * @param includeDirectories
	 * @param includeFiles
	 * @return <em>(Must be non-<code>null</code>)</em>
	 */
    protected List<File> getDependencyFiles(final Collection<Artifact> artifacts, final boolean includeDirectories, boolean includeFiles) {
        List<File> files = new ArrayList<File>(artifacts.size());
        for (Artifact artifact : artifacts) {
            File file = artifact.getFile();
            if (file.isFile() && includeFiles) {
                files.add(file);
            }
            if (file.isDirectory() && includeDirectories) {
                files.add(file);
            }
        }
        return files;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        printLicenseInfo();
        final File finalFile = new File(buildDirectory, finalFileName);
        final List<File> libFiles = getDependencyFiles(artifacts, false, true);
        Manifest manifest = null;
        try {
            manifest = createManifest(libFiles);
        } catch (IOException e) {
            logger.error(e);
        }
        JarOutputStream out = null;
        try {
            out = new JarOutputStream(new FileOutputStream(finalFile, false), manifest);
            if (this.onejarLicenseURL != null) {
                logger.info("Adding one-jar-license.txt to archive.");
                final URLConnection con = onejarLicenseURL.openConnection();
                final InputStream inStream = con.getInputStream();
                addEntryToZip(out, new ZipEntry("one-jar-license.txt"), inStream);
            }
            logger.info("Adding MainJAR to archive at main/" + mainJarFilename);
            addFileToZip(out, "main/", new File(buildDirectory, mainJarFilename));
            logger.info("Adding [" + libFiles.size() + "] libaries...");
            for (File lib : libFiles) {
                final String libName = lib.getName();
                logger.info("Add lib: " + libName);
                if (this.unpackJARs != null && this.unpackJARs.contains(libName) && libName.endsWith(".jar")) {
                    final File file = lib.getAbsoluteFile();
                    final FileInputStream fileInStream = new FileInputStream(file);
                    final JarInputStream jarInStream = new JarInputStream(fileInStream);
                    try {
                        logger.info("Unpack " + libName + " contents...");
                        ZipEntry entry = jarInStream.getNextEntry();
                        while (entry != null) {
                            try {
                                addEntryToZip(out, entry, jarInStream);
                            } catch (Exception e) {
                                printDebug(e);
                            }
                            entry = jarInStream.getNextEntry();
                        }
                    } finally {
                        IOUtils.closeQuietly(jarInStream);
                    }
                    continue;
                }
                if (lib.isFile() && libName.endsWith(".jar")) {
                    addFileToZip(out, "lib/", lib);
                } else {
                    addFileToZip(out, "binlib/", lib);
                }
            }
        } catch (IOException e) {
            logger.error(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
        final String format = this.project.getArtifact().getType();
        logger.info("Adding file:" + finalFile.getAbsolutePath() + "\n       to the list of artifacts with format " + format);
        projectHelper.attachArtifact(this.project, format, "one-jar", finalFile);
    }
}
