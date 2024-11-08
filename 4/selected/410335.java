package it.dangelo.dojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Goal which touches a timestamp file.
 *
 * @goal dojo
 * 
 * @phase process-resources
 */
public class DojoMojo extends AbstractMojo {

    /**
     * Directory where insert dojo 
     *
     * @parameter default-value="dojo"
     */
    private String dojoDir;

    /**
     * Location of the file.
     * @ parameter expression="${project.build.directory}"
     * @ required
     */
    private File outputDirectory;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /** @parameter expression="${project}" */
    private org.apache.maven.project.MavenProject project;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {
        List<Dependency> dependencies = project.getDependencies();
        File file = new File(this.localRepository.getBasedir());
        File dojoArtifact = null;
        for (Dependency dependency : dependencies) {
            if (!dependency.getGroupId().equals("org.dojotoolkit") && !dependency.getArtifactId().equals("dojo")) continue;
            File file2 = new File(file, dependency.getGroupId().replace('.', File.separatorChar));
            file2 = new File(file2, dependency.getArtifactId());
            file2 = new File(file2, dependency.getVersion());
            file2 = new File(file2, dependency.getArtifactId() + '-' + dependency.getVersion() + '.' + dependency.getType());
            if (!file2.exists()) throw new MojoExecutionException("No artifact found in base repository " + file2);
            dojoArtifact = file2;
        }
        if (dojoArtifact == null) throw new MojoExecutionException("No dojo dependencies found");
        Plugin plugin = (Plugin) project.getBuild().getPluginsAsMap().get("org.apache.maven.plugins:maven-war-plugin");
        if (plugin == null) return;
        String webappDirectory = null;
        Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
        if (dom != null) webappDirectory = dom.getChild("webappDirectory").getValue();
        if (webappDirectory == null) webappDirectory = project.getBuild().getDirectory() + File.separatorChar + project.getArtifactId() + '-' + project.getVersion();
        try {
            File webAppDojoDir = new File(webappDirectory, this.dojoDir);
            if (!webAppDojoDir.exists()) webAppDojoDir.mkdirs();
            ZipInputStream inputStream = new ZipInputStream(new FileInputStream(dojoArtifact));
            ZipEntry zipentry = inputStream.getNextEntry();
            byte[] buf = new byte[1024];
            while (zipentry != null) {
                File entry = new File(webAppDojoDir, zipentry.getName());
                if (zipentry.isDirectory()) {
                    entry.mkdirs();
                } else {
                    int n;
                    FileOutputStream fileoutputstream;
                    if (entry.getParent() != null) {
                        entry.getParentFile().mkdirs();
                    }
                    fileoutputstream = new FileOutputStream(entry);
                    while ((n = inputStream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
                    fileoutputstream.close();
                    inputStream.closeEntry();
                }
                zipentry = inputStream.getNextEntry();
            }
            inputStream.close();
        } catch (Exception e) {
            throw new MojoExecutionException("Error to extract the dojo artifact ", e);
        }
    }
}
