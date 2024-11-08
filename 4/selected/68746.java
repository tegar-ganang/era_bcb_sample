package net.sourceforge.hourglass;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sourceforge.hourglass.framework.HourglassPreferences;
import net.sourceforge.hourglass.framework.Prefs;
import net.sourceforge.hourglass.BaseUtilities;
import org.apache.log4j.Logger;

/**
 * BaseUtilities for the Swing UI
 * 
 * @author Mike Grant
 */
public class BaseUtilities {

    private static final String MNEMONIC_RESOURCE_SUFFIX = ".mnemonic";

    protected BaseUtilities() {
        initializeResourceBundle();
    }

    /**
	* Initializes the static variable m_resources with the resource bundle necessary for
	* translation strings.
	*/
    private static void initializeResourceBundle() {
        if (m_resources == null) {
            synchronized (BaseUtilities.class) {
                if (m_resources == null) {
                    m_resources = ResourceBundle.getBundle(gp().getString(Prefs.TRANSLATION_RESOURCE_BUNDLE));
                }
            }
        }
    }

    /**
	 * Returns the sole instance of BaseUtilities.
	 */
    public static BaseUtilities getInstance() {
        if (b_instance == null) {
            b_instance = new BaseUtilities();
        }
        return b_instance;
    }

    /**
	 * Returns the localized string with the given key.
	 */
    public String getString(String resourceKey) {
        String result = null;
        try {
            result = m_resources.getString(resourceKey);
        } catch (MissingResourceException e) {
            getLogger().debug(e);
        }
        return result == null ? "[MISSING: " + resourceKey + "]" : result;
    }

    public String getString(String resourceKey, String[] args) {
        String unformattedMsg = getString(resourceKey);
        return MessageFormat.format(unformattedMsg, (Object[]) args);
    }

    public char getChar(String resourceKey) {
        String result = null;
        try {
            result = m_resources.getString(resourceKey);
        } catch (MissingResourceException e) {
            getLogger().debug(e);
        }
        return result == null ? 0 : result.charAt(0);
    }

    /**
	* Copies a file.
	*/
    public static void copy(File source, File target) throws IOException {
        FileInputStream fin = new FileInputStream(source);
        FileOutputStream fout = new FileOutputStream(target);
        copy(fin, fout);
        fin.close();
        fout.close();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[2048];
        int read = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
    }

    private Logger getLogger() {
        if (m_logger == null) {
            m_logger = Logger.getLogger(getClass());
        }
        return m_logger;
    }

    private static HourglassPreferences gp() {
        return HourglassPreferences.getInstance();
    }

    private static ResourceBundle m_resources;

    private Logger m_logger;

    private static BaseUtilities b_instance;
}
