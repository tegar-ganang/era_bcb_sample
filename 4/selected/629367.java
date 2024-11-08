package net.sf.wmutils.issync;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

/**
 * Abstract base class for IsSync mojos.
 */
public abstract class AbstractSyncMojo extends AbstractMojo {

    /**
	 * Sets the IS root directory.
	 * @parameter expression="${issync.isRoot}"
	 * @required
	 */
    private File isRoot;

    /**
	 * Sets the IS package directory. Defaults to
	 * {@code ${isRoot}/packages}.
	 * @parameter expression="${issync.isPackages}"
	 */
    private File isPackages;

    /**
	 * Sets the local package directory. Defaults to {@code src/main/isPackages}.
	 * @parameter expression="${issync.localPackages}" default-value="src/main/isPackages"
	 * @required
	 */
    private File localPackages;

    /**
	 * A set of configured packages, which are being synchronized.
	 * @parameter
	 * @required
	 */
    private Package[] packages;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    protected Package[] getPackages() {
        return packages;
    }

    protected File getIsPackageDir() throws MojoFailureException {
        if (isPackages == null) {
            if (isRoot == null) {
                throw new MojoFailureException("Required parameter isRoot is not set.");
            }
            return new File(isRoot, "packages");
        }
        return isPackages;
    }

    protected MavenProject getProject() {
        return project;
    }

    protected File getLocalPackageDir() {
        if (localPackages == null) {
            return new File(getProject().getBasedir(), "src/main/isPackages");
        }
        return localPackages;
    }

    private DirectoryScanner getDirectoryScanner(Package pPackage, File pDir) {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(pDir);
        scanner.setIncludes(pPackage.getIncludes());
        scanner.setExcludes(pPackage.getExcludes());
        if (pPackage.isUsingDefaultExcludes()) {
            scanner.addDefaultExcludes();
        }
        scanner.scan();
        return scanner;
    }

    protected void sync(Package pPackage, File pFromDir, File pToDir) throws MojoExecutionException {
        final File fromDir = new File(pFromDir, pPackage.getName());
        final File toDir = new File(pToDir, pPackage.getName());
        if (!toDir.isDirectory() && !toDir.mkdir()) {
            throw new MojoExecutionException("Failed to create directory " + toDir.getPath());
        }
        final DirectoryScanner fromDirScanner = getDirectoryScanner(pPackage, fromDir);
        final DirectoryScanner toDirScanner = getDirectoryScanner(pPackage, toDir);
        final Set<String> toDirFiles = new HashSet<String>();
        for (String file : toDirScanner.getIncludedFiles()) {
            toDirFiles.add(file);
        }
        for (String file : fromDirScanner.getIncludedFiles()) {
            final File source = new File(fromDir, file);
            final File target = new File(toDir, file);
            try {
                FileUtils.copyFile(source, target);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy file " + source + " to " + target + ": " + e.getMessage(), e);
            }
            toDirFiles.remove(file);
        }
        for (String file : toDirFiles) {
            final File target = new File(toDir, file);
            try {
                FileUtils.forceDelete(target);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to delete file " + target + ": " + e.getMessage(), e);
            }
        }
    }
}
