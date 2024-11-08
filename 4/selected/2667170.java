package org.ops4j.pax.construct.lifecycle;

import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * An extension of the Provision mojo that instead of provisioning,  
 * collects all of the files needed to provision and zips them into 
 * a single file suitable for distribution.
 *
 * @goal package
 * @aggregator true
 * 
 * @requiresProject false
 */
public class PackageMojo extends ProvisionMojo {

    /**
	 * The packageOutputDirectory name. 
	 *
	 * @parameter default-value="pax-runner-${project.build.finalName}"
	 */
    private String packageOutputDirectory;

    private String runnerDirName;

    private File runnerDir;

    public void execute() throws MojoExecutionException {
        runnerDirName = System.getProperty("project.build.directory", "target") + "/" + packageOutputDirectory;
        runnerDir = new File(runnerDirName);
        super.execute();
        getLog().info("Zipping runner as: " + runnerDirName);
        try {
            ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(runnerDirName + ".zip"));
            addFiles(runnerDir, zipStream);
            zipStream.close();
        } catch (IOException ioe) {
            throw new MojoExecutionException("Couldn't create zip file", ioe);
        }
    }

    private void addFiles(final File work, final ZipOutputStream zipStream) throws IOException {
        File[] files = work.listFiles();
        int cut = runnerDir.getAbsolutePath().length() - runnerDir.getName().length();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                addFiles(f, zipStream);
            } else if (!f.isHidden()) {
                String fileName = f.getAbsolutePath().substring(cut);
                zipStream.putNextEntry(new ZipEntry(fileName));
                InputStream input = new FileInputStream(f);
                byte[] buffer = new byte[1024];
                int b;
                while ((b = input.read(buffer)) != -1) zipStream.write(buffer, 0, b);
                input.close();
            }
        }
    }

    protected List getDeployCommands() {
        List l = super.getDeployCommands();
        l.add("--executor=script");
        l.add("--workingDirectory=" + runnerDirName);
        return l;
    }
}
