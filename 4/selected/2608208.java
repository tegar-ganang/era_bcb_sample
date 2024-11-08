package org.tockit.docco;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Handles all persistent configuration information.
 *
 * @TODO document
 * @TODO what do to with exceptions? At the moment they are caught and the default
 *       value is returned.
 * @todo create something for boolean expressions
 */
public class ConfigurationManager {

    /**
     * Stores the data we manage.
     */
    private static Properties properties = new Properties();

    /**
     * Initialization code.
     */
    static {
        try {
            FileInputStream in = new FileInputStream("docco.prop");
            properties.load(in);
            in.close();
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * No public instances allowed.
     */
    private ConfigurationManager() {
    }

    /**
     * Saves the current configuration.
     */
    public static void saveConfiguration() {
        try {
            FileOutputStream out = new FileOutputStream("docco.prop");
            properties.store(out, "---  Docco settings ---");
            out.close();
        } catch (FileNotFoundException e) {
            System.out.println("Could not write session information, most likely" + "since running on a read-only file system. Session management" + "is disabled.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stores the size and position of a window.
     */
    public static void storePlacement(String section, Window window) {
        if (window instanceof Frame) {
            Frame frame = (Frame) window;
            properties.setProperty(section + "-windowstate", String.valueOf(frame.getExtendedState()));
            frame.setExtendedState(Frame.NORMAL);
        }
        properties.setProperty(section + "-x", String.valueOf(window.getX()));
        properties.setProperty(section + "-y", String.valueOf(window.getY()));
        properties.setProperty(section + "-width", String.valueOf(window.getWidth()));
        properties.setProperty(section + "-height", String.valueOf(window.getHeight()));
    }

    /**
	 * Restores the size and position of a window.
	 *
	 * If the configuration could not be found or is broken this will do nothing.
	 */
    public static void restorePlacement(String section, Window window, Rectangle defaultPlacement) {
        try {
            int x = Integer.parseInt(properties.getProperty(section + "-x"));
            int y = Integer.parseInt(properties.getProperty(section + "-y"));
            int w = Integer.parseInt(properties.getProperty(section + "-width"));
            int h = Integer.parseInt(properties.getProperty(section + "-height"));
            window.setBounds(x, y, w, h);
        } catch (NumberFormatException e) {
            window.setBounds(defaultPlacement);
        }
        if (window instanceof Frame) {
            Frame frame = (Frame) window;
            try {
                int state = Integer.parseInt(properties.getProperty(section + "-windowstate"));
                frame.setExtendedState(state);
            } catch (NumberFormatException e) {
                frame.setExtendedState(Frame.NORMAL);
            }
        }
    }

    /**
     * Stores an int value.
     */
    public static void storeInt(String section, String key, int value) {
        properties.setProperty(section + "-" + key, String.valueOf(value));
    }

    /**
     * Retrieves an int value.
     */
    public static int fetchInt(String section, String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(section + "-" + key));
        } catch (NumberFormatException e) {
            return defaultValue;
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Stores a float value.
     */
    public static void storeFloat(String section, String key, float value) {
        properties.setProperty(section + "-" + key, String.valueOf(value));
    }

    /**
     * Retrieves a float value.
     */
    public static float fetchFloat(String section, String key, float defaultValue) {
        try {
            return Float.parseFloat(properties.getProperty(section + "-" + key));
        } catch (NumberFormatException e) {
            return defaultValue;
        } catch (NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Stores a String value.
     */
    public static void storeString(String section, String key, String value) {
        properties.setProperty(section + "-" + key, value);
    }

    /**
     * Retrieves a String value.
     */
    public static String fetchString(String section, String key, String defaultValue) {
        return properties.getProperty(section + "-" + key, defaultValue);
    }

    /**
	 * Stores a list of strings.
	 */
    public static void storeStringList(String section, String key, List list) {
        Iterator it = list.iterator();
        int index = 1;
        while (it.hasNext()) {
            String cur = (String) it.next();
            properties.setProperty(section + "-" + key + "-" + index, cur);
            index++;
        }
    }

    /**
	 * Stores a list of strings, removing all occurances after up to the given index (inclusive).
	 */
    public static void storeStringList(String section, String key, List list, int deleteUpToIndex) {
        Iterator it = list.iterator();
        int index = 1;
        while (it.hasNext()) {
            String cur = (String) it.next();
            properties.setProperty(section + "-" + key + "-" + index, cur);
            index++;
        }
        while (index <= deleteUpToIndex) {
            properties.remove(section + "-" + key + "-" + index);
            index++;
        }
    }

    /**
     * Retrieves a list of strings.
     *
     * The list will at most contains maxItems items, maybe less if less are
     * found.
     */
    public static List fetchStringList(String section, String key, int maxItems) {
        List retVal = new LinkedList();
        for (int i = 1; i <= maxItems; i++) {
            String cur = properties.getProperty(section + "-" + key + "-" + i);
            if (cur != null) {
                retVal.add(cur);
            }
        }
        return retVal;
    }

    /**
     * Stores a Color value.
     */
    public static void storeColor(String section, String key, Color value) {
        storeInt(section, key, value.getRGB());
    }

    /**
     * Retrieves a Color value.
     */
    public static Color fetchColor(String section, String key, Color defaultValue) {
        String propVal = properties.getProperty(section + "-" + key);
        if (propVal == null) {
            return defaultValue;
        } else {
            try {
                return Color.decode(propVal);
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
                return defaultValue;
            }
        }
    }
}
