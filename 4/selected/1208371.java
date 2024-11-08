package sample.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which download and copy the Java to csharp eclipse plugin into eclipse "plugins"
 * directory
 * 
 * @goal install
 * @requiresProject false
 * 
 * @phase process-sources
 */
public class InstallEclipsePlugin extends AbstractMojo {

    /**
	 * Artifact resolver, needed to download source jars for inclusion in
	 * classpath.
	 * 
	 * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
	 * @required
	 * @readonly
	 */
    protected ArtifactResolver artifactResolver;

    /**
	 * Location of the local repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @readonly
	 * @required
	 */
    protected ArtifactRepository localRepo;

    /**
	 * List of Remote Repositories used by the resolver
	 * 
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 * @readonly
	 * @required
	 */
    protected java.util.List remoteRepos;

    /**
	 * An ArtifactFactory instance
	 * 
	 * @component
	 */
    private ArtifactFactory artifactFactory;

    /**
	 * The home directory if eclipse 3.2
	 * 
	 * @parameter expression="${eclipse.home}" default-value="${env.ECLIPSE_HOME}"
	 * @required
	 */
    private String eclipseHome;

    /**
	 * @component
	 */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
	 * 
	 */
    public void execute() throws MojoExecutionException {
        String groupeId = "com.ilog.rules";
        String artifactId = "Java2CSharpTranslator";
        String version = "1.0.0";
        Artifact artifact = (Artifact) this.artifactFactory.createArtifact(groupeId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar");
        if (eclipseHome == null || eclipseHome.equals("null")) {
            eclipseHome = System.getenv().get("ECLIPSE_HOME");
        }
        try {
            this.artifactResolver.resolve(artifact, remoteRepos, localRepo);
            String sourceDir = localRepo.getBasedir() + File.separator + groupeId.replace(".", File.separator) + File.separator + artifactId + File.separator + version + File.separator;
            String sourceName = artifactId + "-" + version + ".jar";
            getLog().info("Translator is  : " + sourceDir + sourceName);
            File file = new File(sourceDir + sourceName);
            if (file.exists()) {
                String destName = groupeId + "." + artifactId + "_" + version + ".jar";
                String destDir = eclipseHome + File.separator + "plugins" + File.separator;
                copy(sourceDir + sourceName, destDir + destName);
            } else {
                getLog().info("File does not exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copy(String fromFileName, String toFileName) throws IOException {
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
            System.out.print("Overwrite existing file " + toFile.getName() + "? (Y/N): ");
            System.out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String response = in.readLine();
            if (!response.equals("Y") && !response.equals("y")) throw new IOException("FileCopy: " + "existing file was not overwritten.");
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }
}
