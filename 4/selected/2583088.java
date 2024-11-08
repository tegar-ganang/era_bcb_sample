package org.intelligentsia.keystone;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which touches a timestamp file.
 * 
 * TODO add updater utility on demand when this tools will be ready.
 * 
 * @goal custom
 * 
 * @phase package
 */
public class BootStrapMojo extends AbstractMojo {

    /**
	 * Projet en cours de deploiement.
	 * 
	 * @parameter expression="${project}"
	 */
    private org.apache.maven.project.MavenProject project;

    /**
	 * The Jar archiver needed for archiving.
	 * 
	 * @parameter
	 *            expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	 * @required
	 */
    private JarArchiver jarArchiver;

    /**
	 * The maven archive configuration to use
	 * 
	 * @parameter
	 */
    protected MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
	 * Build directory.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
    private File buildDirectory;

    /** @component */
    private org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /** @component */
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /** @parameter default-value="${localRepository}" */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    /** @parameter default-value="${project.remoteArtifactRepositories}" */
    private List<?> remoteRepositories;

    /**
	 * @component
	 * @required
	 * @readonly
	 */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
	 * @component
	 * @required
	 * @readonly
	 */
    private ArtifactCollector artifactCollector;

    /**
	 * @component
	 * @required
	 * @readonly
	 */
    private DependencyTreeBuilder treeBuilder;

    /**
	 * support for accessing archives
	 * 
	 * @component
	 */
    private ArchiverManager archiverManager;

    /** @parameter default-value="${plugin.artifacts}" */
    private java.util.List<Artifact> pluginArtifacts;

    /**
	 * java main class to run
	 * 
	 * @parameter expression="${mainClass}"
	 */
    private final String mainClass = null;

    /**
	 * Parameter for Bootstrap: true|false (default true) if true halt on first
	 * error, neither continue as he can
	 * 
	 * @parameter expression="${haltOnError}"
	 */
    private final boolean haltOnError = true;

    /**
	 * Parameter for Bootstrap: true|false (default true) if true explode inner
	 * jar in lib, tools, and plugins directory according jar inner resources
	 * 
	 * @parameter expression="${explode}"
	 */
    private final boolean explode = true;

    /**
	 * Parameter for Bootstrap: true|false (default false) clean up local
	 * 'tools' file system on startup
	 * 
	 * @parameter expression="${cleanUpTools}"
	 */
    private final boolean cleanUpTools = false;

    /**
	 * Parameter for Bootstrap: true|false (default true) clean up local 'lib'
	 * file system on startup (and shutdown if BootStrap.cleanUpBeforeShutdown
	 * is setted)
	 * 
	 * @parameter expression="${cleanUpLib}"
	 */
    private final boolean cleanUpLib = true;

    /**
	 * Parameter for Bootstrap: true|false (default false) clean up local
	 * 'plugins' file system on startup (and shutdown if
	 * BootStrap.cleanUpBeforeShutdown is setted)
	 * 
	 * @parameter expression="${cleanUpPlugins}"
	 */
    private final boolean cleanUpPlugins = false;

    /**
	 * Parameter for Bootstrap: true|false (default false) activate 'verbose'
	 * mode
	 * 
	 * @parameter expression="${verbose}"
	 */
    private final boolean verbose = false;

    /**
	 * Parameter for Bootstrap: true|false (default true) activate 'info' mode
	 * 
	 * @parameter expression="${info}"
	 */
    private final boolean info = true;

    /**
	 * Parameter for Bootstrap: log file of bootstrap (default is none)
	 * 
	 * @parameter expression="${logFile}"
	 */
    private final String logFile = null;

    /**
	 * Parameter for Bootstrap: true|false (default false) incluse java home
	 * librarie
	 * 
	 * @parameter expression="${includeJavaHomeLib}"
	 */
    private final boolean includeJavaHomeLib = false;

    /**
	 * Parameter for Bootstrap: true|false (default false) include system class
	 * loader
	 * 
	 * @parameter expression="${includeSystemClassLoader}"
	 */
    private final boolean includeSystemClassLoader = false;

    /**
	 * Parameter for Bootstrap: true|false (default true) include plugins folder
	 * in classpath
	 * 
	 * @parameter expression="${includePlugins}"
	 */
    private final boolean includePlugins = true;

    /**
	 * If true then add updater utility
	 * 
	 * @parameter expression="${withUpdate}"
	 */
    @SuppressWarnings("unused")
    private final boolean withUpdate = false;

    /**
	 * if true, the final boot archive will replace project artifact.
	 * 
	 * @parameter expression="${replaceProjectArtifact}"
	 */
    private final boolean replaceProjectArtifact = false;

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Build Bootstrap");
        final File root = new File(buildDirectory, "bootstrap");
        if (root.exists()) {
            BootStrapMojo.delete(root);
        }
        if (!root.mkdirs()) {
            throw new MojoExecutionException("Cannot create output directory " + root.getName());
        }
        copyBootStrap(root);
        writeBootStrapProperties(root);
        final File libFile = copyDependencies(root);
        copyMainArtifact(libFile);
        packageApplication(root);
    }

    /**
	 * Copy all runtimes dependencies of the project inside %root%/lib
	 * 
	 * @param root
	 * @throws MojoExecutionException
	 * @return library directory
	 */
    private File copyDependencies(final File root) throws MojoExecutionException {
        getLog().info("copy runtime dependencies");
        final File libDirectory = new File(root, "lib");
        if (!libDirectory.mkdirs()) {
            throw new MojoExecutionException("Cannot create libraries directory " + libDirectory.getName());
        }
        try {
            final Set<Artifact> artifacts = new HashSet<Artifact>();
            final ArtifactFilter artifactFilter = new ScopeArtifactFilter(null);
            final DependencyNode rootNode = treeBuilder.buildDependencyTree(project, localRepository, artifactFactory, artifactMetadataSource, artifactFilter, artifactCollector);
            for (final Iterator<?> iterator = rootNode.getChildren().iterator(); iterator.hasNext(); ) {
                final DependencyNode child = (DependencyNode) iterator.next();
                collect(child, artifacts);
            }
            for (final Artifact artifact : artifacts) {
                try {
                    resolver.resolve(artifact, remoteRepositories, localRepository);
                } catch (final ArtifactResolutionException e) {
                    throw new MojoExecutionException("Unable to resolve " + artifact, e);
                } catch (final ArtifactNotFoundException e) {
                    throw new MojoExecutionException("Unable to resolve " + artifact, e);
                }
                FileUtils.copyFileToDirectory(artifact.getFile(), libDirectory);
            }
        } catch (final DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Error analysing dependency", e);
        } catch (final IOException e) {
            throw new MojoExecutionException("Error copying libs", e);
        }
        return libDirectory;
    }

    private void collect(final DependencyNode node, final Set<Artifact> artifacts) {
        if (node.getState() == DependencyNode.INCLUDED) {
            final Artifact artifact = node.getArtifact();
            if (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
                getLog().info("Adding Artefact: " + artifact.toString());
                artifacts.add(artifact);
                for (final Iterator<?> iterator = node.getChildren().iterator(); iterator.hasNext(); ) {
                    final DependencyNode child = (DependencyNode) iterator.next();
                    artifacts.add(node.getArtifact());
                    collect(child, artifacts);
                }
            }
        }
    }

    /**
	 * Copy project artifact in lib directory.
	 * 
	 * @param libDirectory
	 * @throws MojoExecutionException
	 */
    private void copyMainArtifact(final File libDirectory) throws MojoExecutionException {
        try {
            FileUtils.copyFileToDirectory(project.getArtifact().getFile(), libDirectory);
        } catch (final IOException e) {
            throw new MojoExecutionException("Error copying project artifact ", e);
        }
    }

    /**
	 * Write properties file "META-INF/keystone.properties"
	 */
    private void writeBootStrapProperties(final File root) throws MojoExecutionException {
        getLog().info("write Bootstrap properties");
        final Properties properties = new Properties();
        if (mainClass != null) {
            properties.put("Main-Class", mainClass);
        }
        properties.put("BootStrap.haltOnError", Boolean.toString(haltOnError));
        properties.put("BootStrap.explode", Boolean.toString(explode));
        properties.put("BootStrap.cleanUpTools", Boolean.toString(cleanUpTools));
        properties.put("BootStrap.cleanUpLib", Boolean.toString(cleanUpLib));
        properties.put("BootStrap.cleanUpPlugins", Boolean.toString(cleanUpPlugins));
        properties.put("BootStrap.verbose", Boolean.toString(verbose));
        properties.put("BootStrap.info", Boolean.toString(info));
        if (logFile != null) {
            properties.put("BootStrap.logFile", logFile);
        }
        properties.put("BootStrap.includeJavaHomeLib", Boolean.toString(includeJavaHomeLib));
        properties.put("BootStrap.includeSystemClassLoader", Boolean.toString(includeSystemClassLoader));
        properties.put("BootStrap.includePlugins", Boolean.toString(includePlugins));
        final File metainf = new File(root, "META-INF");
        if (!metainf.exists()) {
            if (!metainf.mkdirs()) {
                throw new MojoExecutionException("Unable to create META-INF directory");
            }
        }
        final File keystone = new File(metainf, "keystone.properties");
        FileWriter w = null;
        try {
            w = new FileWriter(keystone);
            properties.store(w, "AUTO generated");
        } catch (final IOException e) {
            throw new MojoExecutionException("Error writing properties file META-INF/keystone.properties ", e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (final IOException e) {
                }
            }
        }
    }

    private void packageApplication(final File root) throws MojoExecutionException {
        final String archiveName = project.getBuild().getFinalName() + ".boot.jar";
        getLog().info("package boot: " + archiveName);
        final File custFile = new File(buildDirectory, archiveName);
        archive.setAddMavenDescriptor(false);
        final MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(custFile);
        try {
            jarArchiver.setManifest(new File(new File(root, "META-INF"), "MANIFEST.MF"));
            archiver.getArchiver().addDirectory(root);
            final File keystone = new File(project.getBuild().getOutputDirectory(), "keystone.properties");
            if (keystone.exists()) {
                archiver.getArchiver().addFile(keystone, "keystone.properties");
            }
            archiver.createArchive(project, archive);
            if (replaceProjectArtifact) {
                project.getArtifact().setFile(custFile);
            }
            final Artifact artifact = artifactFactory.createProjectArtifact(project.getGroupId(), project.getArtifactId() + "-boot", project.getVersion());
            artifact.setFile(custFile);
            project.addAttachedArtifact(artifact);
        } catch (final ArchiverException e) {
            throw new MojoExecutionException("Exception while packaging", e);
        } catch (final ManifestException e) {
            throw new MojoExecutionException("Exception while packaging", e);
        } catch (final IOException e) {
            throw new MojoExecutionException("Exception while packaging", e);
        } catch (final DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Exception while packaging", e);
        }
    }

    private void copyBootStrap(final File root) throws MojoExecutionException {
        String pluginVersion = "";
        for (final Artifact a : pluginArtifacts) {
            if (a.getGroupId().equals("org.intelligentsia.keystone") && a.getArtifactId().equals("boot")) {
                pluginVersion = a.getVersion();
                break;
            }
        }
        try {
            final Artifact artifact = artifactFactory.createArtifact("org.intelligentsia.keystone", "boot", pluginVersion, "compile", "jar");
            resolver.resolve(artifact, remoteRepositories, localRepository);
            final File artifactFile = artifact.getFile();
            final UnArchiver unArchiver = archiverManager.getUnArchiver(artifactFile);
            unArchiver.setSourceFile(artifactFile);
            unArchiver.setDestDirectory(root);
            try {
                unArchiver.extract();
            } catch (final IOException ex) {
                throw new MojoExecutionException("Unable to unarchive " + artifactFile.getName(), ex);
            }
        } catch (final ArtifactNotFoundException e) {
            throw new MojoExecutionException("Exception while copying bootsrap", e);
        } catch (final ArtifactResolutionException e) {
            throw new MojoExecutionException("Exception while copying bootsrap", e);
        } catch (final ArchiverException e) {
            throw new MojoExecutionException("Exception while copying bootsrap", e);
        } catch (final NoSuchArchiverException e) {
            throw new MojoExecutionException("Exception while copying bootsrap", e);
        }
    }

    /**
	 * Utility to delete file (directory or single file)
	 * 
	 * @param from
	 * @return
	 */
    private static boolean delete(final File from) {
        if ((from != null) && from.exists()) {
            if (from.isDirectory()) {
                for (final File child : from.listFiles()) {
                    BootStrapMojo.delete(child);
                }
            }
            return from.delete();
        }
        return false;
    }
}
