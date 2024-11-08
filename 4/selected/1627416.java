package net.sf.maven.plugins;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Goal which touches a timestamp file.
 *
 * @goal one-jar
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class OneJarMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Used for attaching the artifact in the project.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * All the dependencies including trancient dependencies.
     *
     * @parameter default-value="${project.artifacts}"
     * @required
     * @readonly
     */
    private Collection<Artifact> artifacts;

    /**
     * The directory for the resulting file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * Name of the main JAR.
     *
     * @parameter expression="${project.build.finalName}.jar"
     * @readonly
     * @required
     */
    private String mainJarFilename;

    /**
     * Implementation Version of the jar.  Defaults to the build's version.
     *
     * @parameter expression="${project.version}"
     * @required
     */
    private String implementationVersion;

    /**
     * Name of the generated JAR.
     *
     * @parameter expression="${project.build.finalName}.one-jar.jar"
     * @required
     */
    private String filename;

    /**
     * The version of one-jar to use.  Has a default, so typically no need to specify this.
     *
     * @parameter expression="${onejar-version}" default-value="0.96"
     */
    private String onejarVersion;

    /**
     * The main class that one-jar should activate
     *
     * @parameter expression="${onejar-mainclass}"
     */
    private String mainClass;

    /**
     * Strip the version numbers of all libraries.
     * 
     * @parameter default-value="false"
     */
    private boolean stripVersion;

    /**
     * Map to define a mapping of library names to target names.
     * @parameter
     */
    private Map<String, String> fileNameMap;

    /**
     * The policy file for one-jar to use in jnlp
     * exxpression="${basedir}/src/main/resources/one-jar.policy"
     *
     * @parameter 
     * 
     * 
     *  
     */
    private File policyFile;

    public void execute() throws MojoExecutionException {
        displayPluginInfo();
        JarOutputStream out = null;
        JarInputStream template = null;
        try {
            File onejarFile = new File(outputDirectory, filename);
            out = new JarOutputStream(new FileOutputStream(onejarFile, false), getManifest());
            if (getLog().isDebugEnabled()) {
                getLog().debug("Adding main jar main/[" + mainJarFilename + "]");
            }
            addToZip(new File(outputDirectory, mainJarFilename), "main/", out);
            List<File> jars = getFilesForStrings(artifacts, false, true);
            if (getLog().isDebugEnabled()) {
                getLog().debug("Adding [" + jars.size() + "] libaries...");
            }
            for (File jar : jars) {
                if (jar.isFile() && jar.getName().endsWith(".jar")) {
                    addToZip(jar, "lib/", out);
                } else {
                    addToZip(jar, "binlib/", out);
                }
            }
            getLog().debug("Adding one-jar components...");
            template = openOnejarTemplateArchive();
            ZipEntry entry;
            while ((entry = template.getNextEntry()) != null) {
                if (!"boot-manifest.mf".equals(entry.getName())) {
                    addToZip(out, entry, template);
                }
            }
            if (this.policyFile != null) {
                addToZip(out, new ZipEntry(this.policyFile.getName()), new FileInputStream(this.policyFile));
            }
            final String format = this.project.getArtifact().getType();
            getLog().debug("format:" + format);
            getLog().debug("file:" + onejarFile.getAbsolutePath());
            projectHelper.attachArtifact(this.project, format, "one-jar", onejarFile);
        } catch (IOException e) {
            getLog().error(e);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(template);
        }
    }

    private void displayPluginInfo() {
        getLog().info("Using One-Jar to create a single-file distribution");
        getLog().info("Implementation Version: " + implementationVersion);
        getLog().info("Using One-Jar version: " + onejarVersion);
        getLog().info("More info on One-Jar: http://one-jar.sourceforge.net/");
        getLog().info("License for One-Jar:  http://one-jar.sourceforge.net/one-jar-license.txt");
        getLog().info("One-Jar file: " + outputDirectory.getAbsolutePath() + File.separator + filename);
    }

    private String getOnejarArchiveName() {
        return "one-jar-boot-" + onejarVersion + ".jar";
    }

    private JarInputStream openOnejarTemplateArchive() throws IOException {
        return new JarInputStream(getClass().getClassLoader().getResourceAsStream(getOnejarArchiveName()));
    }

    private Manifest getManifest() throws IOException {
        ZipInputStream zipIS = openOnejarTemplateArchive();
        Manifest manifest = new Manifest(getFileBytes(zipIS, "boot-manifest.mf"));
        IOUtils.closeQuietly(zipIS);
        if (mainClass != null) {
            manifest.getMainAttributes().putValue("One-Jar-Main-Class", mainClass);
        }
        if (implementationVersion != null) {
            manifest.getMainAttributes().putValue("ImplementationVersion", implementationVersion);
        }
        return manifest;
    }

    private void addToZip(File sourceFile, String zipfilePath, JarOutputStream out) throws IOException {
        final String strippedFileName = stripVersionNumber(sourceFile.getName());
        final String mappedFileName = mapFileName(strippedFileName);
        addToZip(out, new ZipEntry(zipfilePath + mappedFileName), new FileInputStream(sourceFile));
    }

    private void addToZip(JarOutputStream out, ZipEntry entry, InputStream in) throws IOException {
        out.putNextEntry(entry);
        IOUtils.copy(in, out);
        out.closeEntry();
    }

    /**
     * Strip the version number if the plugin parameter stripVersion is set to true.
     * Its only a simple cut from the last "-" until the end of filename.
     * 
     */
    private String stripVersionNumber(final String rawFileName) {
        if (!stripVersion) {
            return rawFileName;
        }
        String fileName = rawFileName;
        String ext = fileName.substring(fileName.length() - 4);
        int pos = fileName.lastIndexOf("-");
        if (pos > -1) {
            fileName = fileName.substring(0, pos) + ext;
        }
        getLog().debug("add:" + fileName);
        return fileName;
    }

    private String mapFileName(final String rawFileName) {
        String fileName = rawFileName;
        if (fileNameMap != null && fileNameMap.containsKey(rawFileName)) {
            fileName = fileNameMap.get(rawFileName);
            getLog().debug("map:" + rawFileName + " to:" + fileName);
        }
        return fileName;
    }

    private InputStream getFileBytes(ZipInputStream is, String name) throws IOException {
        ZipEntry entry = null;
        while ((entry = is.getNextEntry()) != null) {
            if (entry.getName().equals(name)) {
                byte[] data = IOUtils.toByteArray(is);
                return new ByteArrayInputStream(data);
            }
        }
        return null;
    }

    /**
     * Returns File objects for each parameter string. TODO: Replace with Transformer
     *
     * @param artifacts          <em>(Must be non-<code>null</code>)</em>
     * @param includeDirectories
     * @param includeFiles
     * @return <em>(Must be non-<code>null</code>)</em>
     */
    private List<File> getFilesForStrings(Collection<Artifact> artifacts, boolean includeDirectories, boolean includeFiles) {
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
}
