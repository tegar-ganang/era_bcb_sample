package org.lambkin.maven.plugin;

import java.io.File;
import org.codehaus.plexus.util.FileUtils;

/**
 * @goal deploy
 * @requiresDependencyResolution runtime
 */
public class DeployMojo extends AbstractLambkinMojo {

    /**
   * @parameter expression="${project.build.directory}/${project.artifactId}.war"
   */
    private File warFile;

    private File deployDirectory = new File("F:\\develop\\working\\tomcat\\webapps");

    protected void doValidate() throws Exception {
        System.out.println("\n*************************\tBegin doValidate()\t*************************\n");
        getLog().info("warFile = " + warFile.getAbsolutePath());
        getLog().info("deployDirectory = " + deployDirectory.getAbsolutePath());
        getLog().info("size = " + project.getArtifacts().size());
        System.out.println("\n\n*************************\tEnd doValidate()\t*************************\n");
    }

    protected void doExecute() throws Exception {
        System.out.println("\n*************************\tBegin doExecute()\t*************************\n");
        FileUtils.deleteDirectory(new File(deployDirectory, project.getArtifactId()));
        new File(deployDirectory, project.getArtifactId() + ".war").delete();
        FileUtils.copyFileToDirectory(warFile, deployDirectory);
        System.out.println("\n\n*************************\tEnd doExecute()\t*************************\n");
    }
}
