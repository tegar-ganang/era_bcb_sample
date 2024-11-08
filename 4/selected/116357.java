package ebgeo.maprequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Some data that could be cached on disk.
 */
public class CachedFile {

    private String filename = null;

    private File file = null;

    private long modified = 0;

    private Log log = LogFactory.getLog(CachedFile.class);

    /** Creates a new instance of CachedFile */
    public CachedFile(String filename) {
        this.filename = filename;
        file = new File(MapRequest.getInstance().getCacheDirectory() + File.separator + filename);
        modified = file.lastModified();
    }

    public boolean setContent(String content) {
        boolean result = true;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content, 0, content.length());
            writer.close();
            modified = System.currentTimeMillis();
        } catch (IOException ex) {
            log.error("Failed to read file " + MapRequest.getInstance().getCacheDirectory() + File.separator + filename, ex);
            result = false;
        }
        return result;
    }

    public String getContent() {
        String result = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringWriter writer = new StringWriter();
            char[] data = new char[1024];
            int length;
            int position = 0;
            while ((length = reader.read(data)) >= 0) writer.write(data, 0, length);
            reader.close();
            writer.close();
            result = writer.toString();
        } catch (FileNotFoundException ex) {
            log.error("Failed to read file " + filename, ex);
        } catch (IOException ex) {
            log.error("Failed to read file " + filename, ex);
        }
        return result;
    }

    public boolean isStale() {
        boolean result = false;
        if (!file.exists()) result = true; else {
            long age = System.currentTimeMillis() - modified;
            if (age > (1000 * 60 * 60 * 24 * 14)) result = true;
        }
        return result;
    }
}
