package it.activemind.xtools.development;

import it.activemind.xtools.Messages;
import it.activemind.xtools.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Update dependencies libs.
 * 
 * @author france
 * @version $Id: UpdateLibsMojo.java 20 2006-08-18 16:43:34Z france $
 * @goal updatelibs
 * @requiresDependencyResolution
 * @execute phase="compile"
 */
public class UpdateLibsMojo extends AbstractMojo {

    private static final String WEBAPP_LIB_PATH = "WEB-INF" + File.separator + "lib";

    /**
	 * It's the destination directory
	 * 
	 * <pre>
	 *      &lt;destination&gt;.deployables/webappname/WEB-INF/libs&lt;/destination&gt;
	 * </pre>
	 * 
	 * @parameter expression="${deployablesDir}"
	 */
    private String deployablesDir;

    /**
	 * The current Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
    private MavenProject project;

    /**
	 * @parameter expression="${localRepository}"
	 * @required
	 * @readonly
	 */
    private ArtifactRepository localRepository;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (this.deployablesDir == null) {
            throw new MojoExecutionException(Messages.getString("xtools.defineDestinationDir"));
        }
        Set<Artifact> artifacts = project.getArtifacts();
        String m2repo = localRepository.getBasedir();
        for (Artifact artifact : artifacts) {
            String artifactScope = artifact.getScope();
            if (artifactScope.equals("compile") || (artifactScope.equals("test"))) {
                String artifactId = artifact.getArtifactId();
                String groupId = artifact.getGroupId();
                String versionId = artifact.getVersion();
                String artifactName = artifactId + "-" + versionId + ".jar";
                File srcFile = new File(m2repo + File.separator + StringUtils.replace(groupId, ".", File.separator) + File.separator + artifactId + File.separator + versionId + File.separator + artifactName);
                String srcFileName = Utils.getCanonicalPath(srcFile);
                File destFile = new File(deployablesDir + File.separator + WEBAPP_LIB_PATH + File.separator + artifactName);
                String destFileName = Utils.getCanonicalPath(destFile);
                if (new File(destFileName).exists() == true) {
                    getLog().info(Messages.getString("updateLibs.skippingExistentArtifact", artifactName));
                } else {
                    try {
                        getLog().info(Messages.getString("updateLibs.copying", artifactName));
                        FileUtils.copyFile(new File(srcFileName), new File(destFileName));
                    } catch (IOException e) {
                        getLog().info(Messages.getString("xtools.mojoException", e.getMessage()));
                    }
                }
            }
        }
    }

    public void setDeployablesDir(String deployablesDir) {
        this.deployablesDir = deployablesDir;
    }

    /**
	 * Returns current Maven project.
	 * 
	 * @return current <code>MavenProject</code>
	 */
    public MavenProject getProject() {
        return this.project;
    }

    /**
	 * Returns current Maven repository
	 * 
	 * @return current <code>ArtifactRepository</code>
	 */
    public ArtifactRepository getLocalRepository() {
        return this.localRepository;
    }
}
