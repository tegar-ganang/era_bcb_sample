package net.sf.statcvs.output;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import net.sf.statcvs.util.FileUtils;

/**
 * CSS handler for a local CSS file which will be copied to the output dir.
 * 
 * @author Richard Cyganiak
 */
public class LocalFileCssHandler implements CssHandler {

    private static Logger logger = Logger.getLogger("net.sf.statcvs.output.CssHandler");

    private final String filename;

    /**
     * Creates a new LocalFileCssHandler for a given CSS file.
     * The filename can be absoulte or relative.
     * @param filename Name of the CSS file
     */
    public LocalFileCssHandler(final String filename) {
        this.filename = filename;
    }

    /**
     * @see net.sf.statcvs.output.CssHandler#getLink()
     */
    public String getLink() {
        return FileUtils.getFilenameWithoutPath(filename);
    }

    /**
     * Checks if the local CSS file exists
     * @see net.sf.statcvs.output.CssHandler#checkForMissingResources()
     * @throws ConfigurationException if the file is not found
     */
    public void checkForMissingResources() throws ConfigurationException {
        logger.finer("Checking if CSS file exists: '" + filename + "'");
        final File f = new File(filename);
        if (!f.exists()) {
            throw new ConfigurationException("CSS file not found: " + filename);
        }
    }

    /**
     * Copies the local CSS file to the output directory
     * @see net.sf.statcvs.output.CssHandler#createOutputFiles()
     */
    public void createOutputFiles() throws IOException {
        final String destination = ConfigurationOptions.getOutputDir() + getLink();
        logger.info("Copying CSS file to '" + destination + "'");
        FileUtils.copyFile(filename, destination);
    }

    /**
     * toString
     * @return string
     */
    public String toString() {
        return "local CSS file (" + filename + ")";
    }
}
