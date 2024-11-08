package org.reddwarfserver.maven.plugin.sgs;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Abstract Mojo which provides common functionality to all RedDwarf
 * Mojos that deploy artifacts into a directory.
 */
public abstract class AbstractDirectoryMojo extends AbstractSgsMojo {

    /**
     * If true, artifacts to deploy will be unpacked into the deploy directory.
     * Otherwise, they will simply be copied.  Defaults to "false"
     *
     * @parameter default-value="false"
     * @since 1.0-alpha-1
     */
    protected boolean unpack;

    /**
     * Component used for acquiring unpacking utilities.
     *
     * @component
     * @readonly
     * @required
     * @since 1.0-alpha-1
     */
    protected ArchiverManager archiverManager;

    public abstract File getDirectory() throws MojoExecutionException;

    public abstract File[] getFiles() throws MojoExecutionException;

    public void execute() throws MojoExecutionException {
        this.checkConfig();
        File directory = getDirectory();
        this.checkDirectory(directory);
        try {
            for (File f : this.getFiles()) {
                this.checkFile(f);
                if (unpack) {
                    this.getLog().info("Extracting " + f + " into " + directory);
                    UnArchiver unArchiver = archiverManager.getUnArchiver(f);
                    unArchiver.setSourceFile(f);
                    unArchiver.setDestDirectory(directory);
                    unArchiver.extract();
                } else {
                    this.getLog().info("Copying " + f + " to " + directory);
                    FileUtils.copyFileToDirectory(f, directory);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("File copy failed", e);
        } catch (NoSuchArchiverException nsae) {
            throw new MojoExecutionException("Unknown archive", nsae);
        } catch (ArchiverException ae) {
            throw new MojoExecutionException("Error unpacking", ae);
        }
    }
}
