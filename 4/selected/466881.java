package org.dmonix.timex.properties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import org.dmonix.io.IOUtil;
import org.dmonix.xml.XMLPropertyHandler;

/**
 * The Timex properties
 * 
 * @author Peter Nerg
 * @version 1.0
 */
public abstract class TmxProperties extends XMLPropertyHandler {

    private static final Logger log = Logger.getLogger(TmxProperties.class.getName());

    public static final String PATH = getHome() + File.separator;

    public static final String DATA_DIR = PATH + File.separator + "data" + File.separator;

    public static final String PROP_LOOKNFEEL = "looknfeel";

    public static final String PROP_TOOLBAR_TMX = "toolbartmx";

    public static final String PROP_TOOLBAR_READER = "toolbarreader";

    public static final String PROP_SUMLABEL = "sumlabel";

    public static final String PROP_BACKUP = "backup";

    private static File PROPERTY_FILE = new File(PATH + "timex.properties");

    static {
        XMLPropertyHandler.setTransformerProperty(OutputKeys.INDENT, "yes");
        XMLPropertyHandler.setTransformerProperty("encoding", "UTF-8");
    }

    public static void parseTimexPropertyFile() {
        if (!PROPERTY_FILE.exists()) copyPropertyFile();
        XMLPropertyHandler.parsePropertyFile(PROPERTY_FILE);
    }

    public static void saveTimexPropertyFile() {
        XMLPropertyHandler.savePropertyFile(PROPERTY_FILE);
    }

    public static String getHome() {
        String home = System.getProperty("dmonix.home", System.getProperty("user.home"));
        home += File.separator + "dmonix" + File.separator + "timex";
        log.log(Level.CONFIG, "Using user.home = " + home);
        return home;
    }

    private static void copyPropertyFile() {
        InputStream istream = null;
        OutputStream ostream = null;
        try {
            PROPERTY_FILE.getParentFile().mkdirs();
            PROPERTY_FILE.createNewFile();
            istream = TmxProperties.class.getClassLoader().getResourceAsStream("org/dmonix/timex/properties/timex.properties");
            ostream = new FileOutputStream(PROPERTY_FILE);
            byte[] data = new byte[256];
            int read = 1;
            while (read > 0) {
                read = istream.read(data);
                ostream.write(data, 0, read);
                ostream.flush();
            }
        } catch (Exception ex) {
            log.log(Level.CONFIG, "Could not copy property file", ex);
        } finally {
            IOUtil.closeNoException(istream);
            IOUtil.closeException(ostream);
        }
    }
}
