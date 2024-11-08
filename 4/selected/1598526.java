package za.co.data.plugins;

import za.co.data.framework.modler.ModlerUI;
import za.co.data.framework.ui.components.MenuTreeNode;
import za.co.data.util.IOUtils;
import javax.swing.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by Darryl Culverwell
 * On Jun 22, 2008 9:33:02 AM
 * Implementing classes can be added as menu generating plugins to the menu
 * configuration screen.
 *
 *
 *
 */
public abstract class MenuPlugin {

    public static final short NORTH = 0;

    public static final short SOUTH = 1;

    public static final short EAST = 2;

    public static final short WEST = 3;

    public static final short NORTH_EAST = 4;

    public static final short SOUTH_EAST = 5;

    public static final short NORTH_WEST = 6;

    public static final short SOUTH_WEST = 7;

    private static final String MENU_CONFIG_DIR_NAME = "/etc/menu";

    protected static File MENU_CONFIG_DIR;

    protected static File MENU_OUTPUT_FILE = new File(ModlerUI.properties.getProperty("silo.absoloute.path") + "/web/menu/menu.html");

    protected static final String PROJECT_ROOT = ModlerUI.properties.getProperty("silo.absoloute.path");

    public void checkOutputDirExists() {
        if (!MENU_OUTPUT_FILE.getParentFile().exists()) MENU_OUTPUT_FILE.getParentFile().mkdirs();
    }

    protected Map<File, File> dependentFiles = new HashMap<File, File>();

    /**
     * Get the possible locations for this menu.
     * This will appear as options in the menu setup
     * screen.
     * @return
     */
    public abstract short[] getAvailableMenuLocations();

    /**
     * Get the user seleced location for this menu
     * @return
     */
    public abstract short getSelectedMenuLocation();

    /**
     * Adds a menu item
     */
    public abstract void addMenuItem(MenuTreeNode parent, MenuTreeNode node);

    /**
     * Insert a menu item at specified index
     * @param parent
     * @param node
     * @param index
     */
    public abstract void insertMenuItem(MenuTreeNode parent, MenuTreeNode node, int index);

    /**
     * Deletes a menu item
     */
    public abstract void removeMenuItem(MenuTreeNode parent, MenuTreeNode node);

    /**
     * Generates the menu code
     */
    public abstract void createMenu();

    /**
     * sets the location for a menu to appear at
     * @param menuLocation
     */
    public abstract void setMenuLocation(short menuLocation);

    /**
     * Load this menu configuration from disk
     * @return Root node of menu
     */
    public abstract MenuTreeNode load();

    /**
     * Get the root node (Entry point) for this menu
     * @return
     */
    public abstract MenuTreeNode getRootNode();

    /**
     * Name that appears in the menu type selection box
     * @return
     */
    public abstract String getMenuPluginName();

    /**
     * This will appear as a tooltip
     * @return
     */
    public abstract String getMenuPluginDescription();

    /**
     * Saves this menu configuration to disk
     */
    public abstract void save();

    public void init() {
        MENU_CONFIG_DIR = new File(ModlerUI.properties.getProperty("silo.absoloute.path") + MENU_CONFIG_DIR_NAME);
        checkOutputDirExists();
        if (!MENU_CONFIG_DIR.exists()) MENU_CONFIG_DIR.mkdirs();
        if (!MENU_OUTPUT_FILE.exists()) MENU_OUTPUT_FILE.mkdirs();
    }

    protected void copyDependents() {
        for (File source : dependentFiles.keySet()) {
            try {
                if (!dependentFiles.get(source).exists()) {
                    if (dependentFiles.get(source).isDirectory()) dependentFiles.get(source).mkdirs(); else dependentFiles.get(source).getParentFile().mkdirs();
                }
                IOUtils.copyEverything(source, dependentFiles.get(source));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get URL to menu
     *
     * @return Menu URL
     */
    public URL getMenuURL() throws MalformedURLException {
        return MENU_OUTPUT_FILE.toURI().toURL();
    }

    /**
     * Allows for menu preview
     * This will be interesting
     * @return Something that shows menu preveiw
     */
    public JComponent getPreview() {
        return new JPanel();
    }

    /**
     * Name that appears in menu selection box.     
     * @return
     */
    public abstract String toString();

    /**
     * Used to construct the menu root if it is a new menu
     * @param rootName
     */
    public abstract void initializeMenu(String rootName);
}
