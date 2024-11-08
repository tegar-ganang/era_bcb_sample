package au.com.lastweekend.openjaws.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.com.lastweekend.openjaws.api.Readings;
import au.com.lastweekend.openjaws.api.WeatherPluginException;

/**
 * Write the readings to a file
 * 
 * @author ggardner
 * 
 */
public class TextFilePlugin extends AbstractWeatherPlugin {

    public static final Logger LOG = LoggerFactory.getLogger(TextFilePlugin.class);

    private File outputFile;

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            LOG.info("Enabled, outputFile=" + outputFile.getAbsolutePath());
        } else {
            LOG.info("Disabled");
        }
    }

    public void processReadings(Readings readings) throws WeatherPluginException {
        PrintWriter writer = null;
        try {
            boolean writeHeadings = !outputFile.exists();
            writer = new PrintWriter(new FileOutputStream(this.outputFile, true));
            writer.write(readings.format(writeHeadings, true, true, false));
            writer.flush();
        } catch (IOException e) {
            throw new WeatherPluginException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && outputFile != null;
    }

    public File getOutputFile() {
        return outputFile;
    }

    /**
     * 
     * @param outputFile
     *            the file to write to
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }
}
