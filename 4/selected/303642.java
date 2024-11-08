package net.sf.statcvs.output;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import net.sf.statcvs.Main;
import net.sf.statcvs.pages.ReportSuiteMaker;
import net.sf.statcvs.util.FileUtils;

/**
 * CSS handler for a CSS file included in the distribution JAR file.
 * 
 * @author Richard Cyganiak
 */
public class DefaultCssHandler implements CssHandler {

    private static Logger logger = Logger.getLogger("net.sf.statcvs.output.CssHandler");

    private final String filename;

    /**
     * Creates a new DefaultCssHandler for a CSS file in the
     * <code>/src/net/sf/statcvs/web-files/</code> folder of the distribution JAR.
     * This must be a filename only, without a directory.
     * @param filename Name of the css file
     */
    public DefaultCssHandler(final String filename) {
        this.filename = filename;
    }

    /**
     * @see net.sf.statcvs.output.CssHandler#getLink()
     */
    public String getLink() {
        return filename;
    }

    /**
     * No external resources are necessary for default CSS files, so
     * nothing is done here
     * @see net.sf.statcvs.output.CssHandler#checkForMissingResources()
     */
    public void checkForMissingResources() throws ConfigurationException {
    }

    /**
     * Extracts the CSS file from the distribution JAR and saves it
     * into the output directory
     * @see net.sf.statcvs.output.CssHandler#createOutputFiles()
     */
    public void createOutputFiles() throws IOException {
        final String destination = ConfigurationOptions.getOutputDir() + filename;
        logger.info("Creating CSS file at '" + destination + "'");
        FileUtils.copyFile(Main.class.getResourceAsStream(ReportSuiteMaker.WEB_FILE_PATH + filename), new File(destination));
    }

    /**
     * toString
     * @return string
     */
    public String toString() {
        return "default CSS file (" + filename + ")";
    }
}
