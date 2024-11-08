package com.google.code.play;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Initializes Play! Maven project:
 * - Checks Play! home directory and creates temporary Play! home in "target" directory
 * if no Play! home directory defined and there is Play! framework zip dependency
 * in the project.
 * - Adds application and dependent modules sources to Maven project as compile source roots.
 * - Adds application and dependent modules resources to Maven project as resources.
 * - Adds application and dependent modules test sources to Maven project as test compile source roots.
 * - Adds application and dependent modules test resources to Maven project as test resources.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @goal initialize
 * @phase initialize
 * @requiresDependencyResolution compile
 */
public class PlayInitializeMojo extends AbstractPlayMojo {

    public static final String playFrameworkVersionFilePath = "framework/src/play/version";

    /**
     * Default Play! id (profile).
     * 
     * @parameter expression="${play.id}" default-value=""
     * @since 1.0.0
     */
    private String playId;

    /**
     * Should application classes be compiled.
     * 
     * @parameter expression="${play.compileApp}" default-value="true"
     * @since 1.0.0
     */
    private boolean compileApp;

    /**
     * Should test classes be compiled.
     * 
     * @parameter expression="${play.compileTest}" default-value="true"
     * @since 1.0.0
     */
    private boolean compileTest;

    /**
     * Should temporary Play! home directory be cleaned before it's reinitializing.
     * If true, homeOverwrite is meaningless.
     * 
     * @parameter expression="${play.homeClean}" default-value="false"
     * @since 1.0.0
     */
    private boolean homeClean;

    /**
     * Should existing temporary Play! home content be overwritten.
     * 
     * @parameter expression="${play.homeOverwrite}" default-value="false"
     * @since 1.0.0
     */
    private boolean homeOverwrite;

    /**
     * To look up Archiver/UnArchiver implementations.
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;

    protected void internalExecute() throws MojoExecutionException, MojoFailureException, IOException {
        String playVersion = null;
        Set<?> artifacts = project.getArtifacts();
        for (Iterator<?> iter = artifacts.iterator(); iter.hasNext(); ) {
            Artifact artifact = (Artifact) iter.next();
            if ("play".equals(artifact.getArtifactId())) {
                playVersion = artifact.getVersion();
                break;
            }
        }
        getLog().debug("Play! version: " + playVersion);
        File playHome = prepareAndGetPlayHome(playVersion);
        File baseDir = project.getBasedir();
        ConfigurationParser configParser = getConfiguration(playId);
        Map<String, File> modules = new HashMap<String, File>();
        Map<String, String> modulePaths = configParser.getModules();
        for (Map.Entry<String, String> modulePathEntry : modulePaths.entrySet()) {
            String moduleName = modulePathEntry.getKey();
            String modulePath = modulePathEntry.getValue();
            modulePath = modulePath.replace("${play.path}", playHome.getPath());
            modules.put(moduleName, new File(modulePath));
        }
        if ((playVersion != null) && "1.2".compareTo(playVersion) <= 0) {
            File modulesDir = new File(baseDir, "modules");
            if (modulesDir.isDirectory()) {
                File[] files = modulesDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String moduleName = file.getName();
                        if (file.isDirectory()) {
                            getLog().debug("Added module '" + moduleName + "': " + file.getAbsolutePath());
                            modules.put(moduleName, file);
                        } else if (file.isFile()) {
                            String realModulePath = readFileFirstLine(file);
                            file = new File(realModulePath);
                            getLog().debug("Added module '" + moduleName + "': " + file.getAbsolutePath());
                            modules.put(moduleName, file);
                        }
                    }
                }
            }
        }
        if (compileApp) {
            File appPath = new File(baseDir, "app");
            project.addCompileSourceRoot(appPath.getAbsolutePath());
            getLog().debug("Added source directory: " + appPath.getAbsolutePath());
            File confPath = new File(baseDir, "conf");
            Resource resource = new Resource();
            resource.setDirectory(confPath.getAbsolutePath());
            project.addResource(resource);
            getLog().debug("Added resource: " + resource.getDirectory());
            for (File modulePath : modules.values()) {
                File moduleAppPath = new File(modulePath, "app");
                if (moduleAppPath.isDirectory()) {
                    project.addCompileSourceRoot(moduleAppPath.getAbsolutePath());
                    getLog().debug("Added source directory: " + moduleAppPath.getAbsolutePath());
                }
            }
        }
        if (compileTest) {
            File testPath = new File(baseDir, "test");
            project.addTestCompileSourceRoot(testPath.getAbsolutePath());
            getLog().debug("Added test source directory: " + testPath.getAbsolutePath());
        }
    }

    protected File prepareAndGetPlayHome(String playDependencyVersion) throws MojoExecutionException, IOException {
        File targetDir = new File(project.getBuild().getDirectory());
        File playTmpDir = new File(targetDir, "play");
        File playTmpHomeDir = new File(playTmpDir, "home");
        Artifact frameworkArtifact = findFrameworkArtifact(true);
        Map<String, Artifact> moduleArtifacts = findAllModuleArtifacts(true);
        if (frameworkArtifact != null) {
            try {
                decompressFrameworkAndSetPlayHome(frameworkArtifact, moduleArtifacts, playDependencyVersion, playTmpHomeDir);
            } catch (ArchiverException e) {
                throw new MojoExecutionException("?", e);
            } catch (NoSuchArchiverException e) {
                throw new MojoExecutionException("?", e);
            }
        } else {
            throw new MojoExecutionException("Missing Play! framework dependency.");
        }
        return playTmpHomeDir;
    }

    private void decompressFrameworkAndSetPlayHome(Artifact frameworkAtifact, Map<String, Artifact> moduleArtifacts, String playDependencyVersion, File playTmpHomeDir) throws MojoExecutionException, NoSuchArchiverException, IOException {
        File warningFile = new File(playTmpHomeDir, "WARNING.txt");
        if (homeClean) {
            FileUtils.deleteDirectory(playTmpHomeDir);
        }
        if (playTmpHomeDir.exists()) {
            if (playTmpHomeDir.isDirectory()) {
                if (warningFile.exists()) {
                    if (!warningFile.isFile()) {
                        throw new MojoExecutionException(String.format("Play! home directory warning file \"%s\" is not a file", warningFile.getCanonicalPath()));
                    }
                } else {
                    throw new MojoExecutionException(String.format("Play! home directory warning file \"%s\" does not exist", warningFile.getCanonicalPath()));
                }
            } else {
                throw new MojoExecutionException(String.format("Play! home directory \"%s\" is not a directory", playTmpHomeDir.getCanonicalPath()));
            }
        }
        createDir(playTmpHomeDir);
        if (!warningFile.exists()) {
            writeToFile(warningFile, "This directory is generated automatically. Don't change its content.");
        }
        File frameworkDir = new File(playTmpHomeDir, "framework");
        if (!frameworkDir.exists() || frameworkDir.lastModified() < frameworkAtifact.getFile().lastModified()) {
            UnArchiver zipUnArchiver = archiverManager.getUnArchiver("zip");
            zipUnArchiver.setSourceFile(frameworkAtifact.getFile());
            zipUnArchiver.setDestDirectory(playTmpHomeDir);
            zipUnArchiver.setOverwrite(false);
            zipUnArchiver.extract();
            File playFrameworkVersionFile = new File(playTmpHomeDir, playFrameworkVersionFilePath);
            createDir(playFrameworkVersionFile.getParentFile());
            writeToFile(playFrameworkVersionFile, playDependencyVersion);
            frameworkDir.setLastModified(System.currentTimeMillis());
        }
        File modulesDirectory = new File(playTmpHomeDir, "modules");
        for (Map.Entry<String, Artifact> moduleArtifactEntry : moduleArtifacts.entrySet()) {
            String moduleName = moduleArtifactEntry.getKey();
            Artifact moduleArtifact = moduleArtifactEntry.getValue();
            File zipFile = moduleArtifact.getFile();
            if (Artifact.SCOPE_PROVIDED.equals(moduleArtifact.getScope())) {
                File moduleDirectory = new File(modulesDirectory, moduleName);
                createModuleDirectory(moduleDirectory, homeOverwrite || moduleDirectory.lastModified() < zipFile.lastModified());
                if (moduleDirectory.list().length == 0) {
                    UnArchiver zipUnArchiver = archiverManager.getUnArchiver("zip");
                    zipUnArchiver.setSourceFile(zipFile);
                    zipUnArchiver.setDestDirectory(moduleDirectory);
                    zipUnArchiver.setOverwrite(false);
                    zipUnArchiver.extract();
                    moduleDirectory.setLastModified(System.currentTimeMillis());
                    if ("scala".equals(moduleName)) {
                        scalaHack(moduleDirectory);
                    }
                }
            }
        }
    }

    private void createDir(File directory) throws IOException {
        if (directory.exists()) {
            if (directory.isDirectory()) {
                if (homeOverwrite) {
                    FileUtils.cleanDirectory(directory);
                }
            } else {
                throw new IOException(String.format("\"%s\" is not a directory", directory.getCanonicalPath()));
            }
        } else {
            if (!directory.mkdirs()) {
                throw new IOException(String.format("Cannot create \"%s\" directory", directory.getCanonicalPath()));
            }
        }
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException(String.format("Cannot create \"%s\" directory", directory.getCanonicalPath()));
            }
        }
    }

    private void scalaHack(File scalaModuleDirectory) throws IOException {
        Set<?> projectArtifacts = project.getArtifacts();
        for (Iterator<?> iter = projectArtifacts.iterator(); iter.hasNext(); ) {
            Artifact artifact = (Artifact) iter.next();
            if ("org.scala-lang".equals(artifact.getGroupId()) && ("scala-compiler".equals(artifact.getArtifactId()) || "scala-library".equals(artifact.getArtifactId())) && "jar".equals(artifact.getType())) {
                File jarFile = artifact.getFile();
                FileUtils.copyFileIfModified(jarFile, new File(scalaModuleDirectory, "lib/" + artifact.getArtifactId() + ".jar"));
            }
        }
    }
}
