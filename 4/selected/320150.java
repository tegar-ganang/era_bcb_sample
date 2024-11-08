package net.sf.buildbox.maven.bbx;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import net.sf.buildbox.maven.BuildboxMojoBase;
import net.sf.buildbox.maven.OutputArtifact;
import net.sf.buildbox.strictlogging.api.*;
import net.sf.buildbox.util.CommandLineExec;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Installs all output artifacts, without updating metadata. Removes previous installation completely.
 * Optionally, the installation can be done as "move and link" to repository.
 *
 * @goal install
 */
public class BbxInstallMojo extends BuildboxMojoBase {

    private static final Catalog CAT = StrictCatalogFactory.getCatalog(Catalog.class);

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    MavenProject project;

    /**
     * Whether to move files to repository and create links to them.
     * It is faster, takes less space, and usually has no other impacts.
     *
     * @parameter expression="${bbx.install.symlink}" default-value="false"
     */
    boolean symlink;

    /**
     * @parameter expression="${settings.localRepository}"
     */
    File localRepository;

    /**
     * File to obtain list of attached artifacts.
     *
     * @parameter expression="${bbx.install.outputFile}" default-value="${project.build.directory}/output-maven-artifacts.xml"
     */
    File outputFile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Set<OutputArtifact> outputArtifacts = OutputArtifact.readMapping(project);
            outputFile.getParentFile().mkdirs();
            final PrintStream pw = new PrintStream(outputFile);
            try {
                OutputArtifact.xmlSave(outputArtifacts, pw);
                logger.log(CAT.artifactListing(outputFile));
            } finally {
                pw.close();
            }
            final File targetDirectory = new File(localRepository, String.format("%s/%s/%s", project.getGroupId().replace('.', '/'), project.getArtifactId(), project.getVersion()));
            FileUtils.deleteDirectory(targetDirectory);
            FileUtils.mkdir(targetDirectory.getAbsolutePath());
            doInstall(outputArtifacts, targetDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void doInstall(Set<OutputArtifact> outputArtifacts, File targetDirectory) throws IOException, InterruptedException {
        logger.log(CAT.installingTo(targetDirectory));
        final String prefix = new File(project.getBuild().getDirectory()).getAbsolutePath() + File.separator;
        for (OutputArtifact artifact : outputArtifacts) {
            final File src = artifact.getFile();
            final String destName = String.format("%s-%s%s.%s", project.getArtifactId(), project.getVersion(), artifact.getClassifier() == null ? "" : "-" + artifact.getClassifier(), artifact.getExtension());
            final File dest = new File(targetDirectory, destName);
            boolean canSymlink = src.getAbsolutePath().startsWith(prefix);
            if (symlink && canSymlink) {
                logger.log(CAT.linking(destName, src));
                FileUtils.rename(src, dest);
                symlink(src, dest);
            } else {
                logger.log(CAT.copying(destName, src));
                FileUtils.copyFile(src, dest);
            }
        }
    }

    public void symlink(File source, File target) throws IOException, InterruptedException {
        new CommandLineExec("ln", "-s", target.getAbsolutePath(), source.getAbsolutePath()) {

            @Override
            protected void log(String s) {
                logger.debug(s);
            }
        }.call();
    }

    private static interface Catalog extends StrictCatalog {

        @StrictCatalogEntry(severity = Severity.INFO, format = "Installing: %s from %s")
        LogMessage copying(String destName, File srcFile);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Installing: %s from %s (symlink)")
        LogMessage linking(String destName, File src);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Installing to: %s")
        LogMessage installingTo(File targetDirectory);

        @StrictCatalogEntry(severity = Severity.DEBUG, format = "Output artifacts are saved to: %s")
        LogMessage artifactListing(File outputFile);
    }
}
