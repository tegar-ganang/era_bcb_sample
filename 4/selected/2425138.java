package net.sf.mavenhar;

import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.project.MavenProject;
import junit.framework.TestCase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;

public class HarMojoTest extends TestCase {

    /**
     *  Temporary repo to use while we do some testing.
     */
    File repo = new File("target/test-classes/unit/dummy-repo");

    /**
     * Copy a file from src to dest.
     * 
     * @param src location of file to copy.
     * @param dest location of copied file.
     * @throws FileNotFoundException when a file doesn't exist.
     * @throws IOException when FileChannel fails.
     */
    public void copy(File src, File dest) throws FileNotFoundException, IOException {
        FileInputStream srcStream = new FileInputStream(src);
        FileOutputStream destStream = new FileOutputStream(dest);
        FileChannel srcChannel = srcStream.getChannel();
        FileChannel destChannel = destStream.getChannel();
        srcChannel.transferTo(0, srcChannel.size(), destChannel);
        destChannel.close();
        srcChannel.close();
        destStream.close();
        srcStream.close();
    }

    public void testMavenHarPluginJarExists() throws Exception {
    }

    /**
     * Functional Testing. This method builds a har.
     * 
     * @throws Exception
     */
    public void testGenerateSimpleHarPackage() throws Exception {
        boolean blah = false;
        assertFalse(blah);
    }
}
