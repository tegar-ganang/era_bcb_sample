package util.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;

public class ReadmeGenerator {

    /**
	 * 
	 * @param dir
	 * @param mavenProject
	 * @return
	 * @throws IOException
	 */
    public File createReadmeFile(File dir, MavenProject mavenProject) throws IOException {
        InputStream is = getClass().getResourceAsStream("README.template");
        StringWriter sw = new StringWriter();
        IOUtils.copy(is, sw);
        String content = sw.getBuffer().toString();
        content = StringUtils.replace(content, "{project_name}", mavenProject.getArtifactId());
        File readme = new File(dir, "README.TXT");
        FileUtils.writeStringToFile(readme, content);
        return readme;
    }
}
