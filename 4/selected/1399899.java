package com.google.code.play;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Extracts project dependencies to "lib" and "modules" directories.
 * It's like Play! framework's "dependencies" command, but uses Maven dependencies,
 * instead of "conf/dependencies.yml" file.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 * @goal dependencies
 * @requiresDependencyResolution test
 */
public class PlayDependenciesMojo extends AbstractDependencyProcessingPlayMojo {

    /**
     * Skip dependencies extraction.
     * 
     * @parameter expression="${play.dependenciesSkip}" default-value="false"
     * @required
     * @since 1.0.0
     */
    private boolean dependenciesSkip;

    /**
     * Should project's "lib" and "modules" subdirectories be cleaned before dependency resolution.
     * If true, dependenciesOverwrite is meaningless.
     * 
     * @parameter expression="${play.dependenciesClean}" default-value="false"
     * @since 1.0.0
     */
    private boolean dependenciesClean;

    /**
     * Should existing dependencies be overwritten.
     * 
     * @parameter expression="${play.dependenciesOverwrite}" default-value="false"
     * @since 1.0.0
     */
    private boolean dependenciesOverwrite;

    /**
     * Should jar dependencies be processed. They are necessary for Play! Framework,
     * but not needed for Maven build (Maven uses dependency mechanism).
     * 
     * @parameter expression="${play.dependenciesSkipJars}" default-value="false"
     * @since 1.0.0
     */
    private boolean dependenciesSkipJars;

    /**
     * To look up Archiver/UnArchiver implementations.
     * 
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     * @readonly
     */
    private ArchiverManager archiverManager;

    protected void internalExecute() throws MojoExecutionException, MojoFailureException, IOException {
        if (dependenciesSkip) {
            getLog().info("Dependencies extraction skipped");
            return;
        }
        File baseDir = project.getBasedir();
        try {
            if (dependenciesClean) {
                if (!dependenciesSkipJars) {
                    FileUtils.deleteDirectory(new File(baseDir, "lib"));
                }
                FileUtils.deleteDirectory(new File(baseDir, "modules"));
            }
            Set<?> projectArtifacts = project.getArtifacts();
            Set<Artifact> excludedArtifacts = new HashSet<Artifact>();
            Artifact playSeleniumJunit4Artifact = getDependencyArtifact(projectArtifacts, "com.google.code.maven-play-plugin", "play-selenium-junit4", "jar");
            if (playSeleniumJunit4Artifact != null) {
                excludedArtifacts.addAll(getDependencyArtifacts(projectArtifacts, playSeleniumJunit4Artifact));
            }
            Set<Artifact> filteredArtifacts = new HashSet<Artifact>();
            for (Iterator<?> iter = projectArtifacts.iterator(); iter.hasNext(); ) {
                Artifact artifact = (Artifact) iter.next();
                if (artifact.getArtifactHandler().isAddedToClasspath() && !Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && !excludedArtifacts.contains(artifact)) {
                    filteredArtifacts.add(artifact);
                }
            }
            Map<String, Artifact> moduleArtifacts = findAllModuleArtifacts(true);
            for (Map.Entry<String, Artifact> moduleArtifactEntry : moduleArtifacts.entrySet()) {
                String moduleName = moduleArtifactEntry.getKey();
                Artifact moduleZipArtifact = moduleArtifactEntry.getValue();
                if (!Artifact.SCOPE_PROVIDED.equals(moduleZipArtifact.getScope())) {
                    checkPotentialReactorProblem(moduleZipArtifact);
                    File moduleZipFile = moduleZipArtifact.getFile();
                    String moduleSubDir = String.format("modules/%s-%s/", moduleName, moduleZipArtifact.getVersion());
                    File moduleDirectory = new File(baseDir, moduleSubDir);
                    createModuleDirectory(moduleDirectory, dependenciesOverwrite || moduleDirectory.lastModified() < moduleZipFile.lastModified());
                    if (moduleDirectory.list().length == 0) {
                        UnArchiver zipUnArchiver = archiverManager.getUnArchiver("zip");
                        zipUnArchiver.setSourceFile(moduleZipFile);
                        zipUnArchiver.setDestDirectory(moduleDirectory);
                        zipUnArchiver.setOverwrite(false);
                        zipUnArchiver.extract();
                        moduleDirectory.setLastModified(System.currentTimeMillis());
                        if ("scala".equals(moduleName)) {
                            scalaHack(moduleDirectory, filteredArtifacts);
                        }
                        if (!dependenciesSkipJars) {
                            Set<Artifact> dependencySubtree = getModuleDependencyArtifacts(filteredArtifacts, moduleZipArtifact);
                            if (!dependencySubtree.isEmpty()) {
                                File moduleLibDir = new File(moduleDirectory, "lib");
                                createLibDirectory(moduleLibDir);
                                for (Artifact classPathArtifact : dependencySubtree) {
                                    File jarFile = classPathArtifact.getFile();
                                    if (dependenciesOverwrite) {
                                        FileUtils.copyFileToDirectory(jarFile, moduleLibDir);
                                    } else {
                                        if (jarFile == null) {
                                            getLog().info("null file");
                                        }
                                        FileUtils.copyFileToDirectoryIfModified(jarFile, moduleLibDir);
                                    }
                                    filteredArtifacts.remove(classPathArtifact);
                                }
                            }
                        }
                    }
                }
            }
            if (!dependenciesSkipJars && !filteredArtifacts.isEmpty()) {
                File libDir = new File(baseDir, "lib");
                createLibDirectory(libDir);
                for (Iterator<?> iter = filteredArtifacts.iterator(); iter.hasNext(); ) {
                    Artifact classPathArtifact = (Artifact) iter.next();
                    File jarFile = classPathArtifact.getFile();
                    if (dependenciesOverwrite) {
                        FileUtils.copyFileToDirectory(jarFile, libDir);
                    } else {
                        FileUtils.copyFileToDirectoryIfModified(jarFile, libDir);
                    }
                }
            }
        } catch (ArchiverException e) {
            throw new MojoExecutionException("?", e);
        } catch (DependencyTreeBuilderException e) {
            throw new MojoExecutionException("?", e);
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("?", e);
        }
    }

    private void createLibDirectory(File libDirectory) throws IOException {
        if (libDirectory.exists()) {
            if (!libDirectory.isDirectory()) {
                throw new IOException(String.format("\"%s\" is not a directory", libDirectory.getCanonicalPath()));
            }
        } else {
            if (!libDirectory.mkdirs()) {
                throw new IOException(String.format("Cannot create \"%s\" directory", libDirectory.getCanonicalPath()));
            }
        }
    }

    private void checkPotentialReactorProblem(Artifact artifact) {
        File artifactFile = artifact.getFile();
        if (artifactFile.isDirectory()) {
            throw new ArchiverException(String.format("\"%s:%s:%s:%s\" dependent artifact's file is a directory, not a file. This is probably Maven reactor build problem.", artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getVersion()));
        }
    }

    private void scalaHack(File scalaModuleDirectory, Set<Artifact> filteredArtifacts) throws IOException {
        Set<?> projectArtifacts = project.getArtifacts();
        for (Iterator<?> iter = projectArtifacts.iterator(); iter.hasNext(); ) {
            Artifact artifact = (Artifact) iter.next();
            if ("org.scala-lang".equals(artifact.getGroupId()) && ("scala-compiler".equals(artifact.getArtifactId()) || "scala-library".equals(artifact.getArtifactId())) && "jar".equals(artifact.getType())) {
                File jarFile = artifact.getFile();
                FileUtils.copyFileIfModified(jarFile, new File(scalaModuleDirectory, "lib/" + artifact.getArtifactId() + ".jar"));
                filteredArtifacts.remove(artifact);
            }
        }
    }
}
