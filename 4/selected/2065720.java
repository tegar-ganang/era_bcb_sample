package org.reddwarfserver.maven.plugin.sgs;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Configures a RedDwarf server installation by overlaying
 * sgs-boot.properties, sgs-server.properties, and sgs-logging.properties
 * files into the conf directory of the installation.
 *
 * @goal configure
 */
public class ConfigureMojo extends AbstractSgsMojo {

    static final String CONF = "conf";

    static final String SGS_BOOT = "sgs-boot.properties";

    static final String SGS_SERVER = "sgs-server.properties";

    static final String SGS_LOGGING = "sgs-logging.properties";

    /**
     * The file used to override the sgs-boot.properties configuration
     * file of the RedDwarf server.
     * 
     * @parameter
     * @since 1.0-alpha-1
     */
    private File sgsBoot;

    /**
     * The file used to override the sgs-server.properties configuration
     * file of the RedDwarf server.
     * 
     * @parameter
     * @since 1.0-alpha-1
     */
    private File sgsServer;

    /**
     * The file used to override the sgs-logging.properties configuration
     * file of the RedDwarf server.
     * 
     * @parameter
     * @since 1.0-alpha-1
     */
    private File sgsLogging;

    public void execute() throws MojoExecutionException {
        this.checkConfig();
        File confDirectory = new File(sgsHome, CONF);
        this.checkDirectory(confDirectory);
        try {
            if (sgsBoot != null) {
                this.checkFile(sgsBoot);
                File targetSgsBoot = new File(confDirectory, SGS_BOOT);
                this.getLog().info("Copying " + sgsBoot + " to " + targetSgsBoot);
                FileUtils.copyFile(sgsBoot, targetSgsBoot);
            }
            if (sgsServer != null) {
                this.checkFile(sgsServer);
                File targetSgsServer = new File(confDirectory, SGS_SERVER);
                this.getLog().info("Copying " + sgsServer + " to " + targetSgsServer);
                FileUtils.copyFile(sgsServer, targetSgsServer);
            }
            if (sgsLogging != null) {
                this.checkFile(sgsLogging);
                File targetSgsLogging = new File(confDirectory, SGS_LOGGING);
                this.getLog().info("Copying " + sgsLogging + " to " + targetSgsLogging);
                FileUtils.copyFile(sgsLogging, new File(confDirectory, SGS_LOGGING));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("File copy failed", e);
        }
    }
}
