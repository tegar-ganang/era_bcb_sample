package base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.jconfig.Configuration;
import org.jconfig.handler.XMLFileHandler;
import util.SuffixFilter;

/**
 * Holds the application preferences. It is accessed through a single instance stored
 * by the App class so as to avoid confusion and errors.
 * 
 * @version 1.5
 * @author Peter Andrews
 */
public class XMLPrefs {

    private Configuration config;

    private XMLFileHandler handler;

    private String prefsPath, currVer;

    BufferedWriter out = null;

    /**
     * Creates a <code>Prefs</code> object and loads the preferences.
     * 
     * @param prefsPathIn   The path that points to the local prefs file. It should probably be .PSE_config in the user's home directory.
     * @see #loadPrefs
     */
    public XMLPrefs(String prefsPathIn, String version) {
        prefsPath = prefsPathIn;
        currVer = version;
        File file = new File(prefsPath);
        if (!file.exists()) {
            try {
                System.err.println("Config file does not exist; making a new one.");
                makeCleanConfigFile(file);
            } catch (IOException e) {
                System.out.println("Error while making new prefs file: " + e);
                e.printStackTrace();
            }
        }
        handler = new XMLFileHandler();
        handler.setFile(file);
        try {
            config = handler.load("PSE");
        } catch (Exception e) {
            System.err.println("Error while loading prefs file: " + e);
            e.printStackTrace();
        }
        if ((currVer != null) && (!get("general.version string").equals(currVer))) {
            try {
                System.err.println("Config file is old (" + get("general.version string") + "); making a clean one (" + currVer + ").");
                makeCleanConfigFile(file);
                config = handler.load("PSE");
            } catch (Throwable e) {
                System.out.println("Error while making clean prefs file: " + e);
                e.printStackTrace();
            }
        }
        File tempFile;
        int ctr;
        String tempList[] = getRecentListsArray();
        for (ctr = tempList.length - 1; ctr >= 0; ctr--) {
            tempFile = new File(tempList[ctr]);
            if (!tempFile.exists()) {
                removeRecentList(ctr);
            }
        }
    }

    /**
     * Saves the prefs to wherever they came from.
     */
    public void savePrefs() {
        try {
            handler.store(config);
        } catch (Exception e) {
            System.err.println("There was an error when writing to the prefs file.");
        }
    }

    /**
     * Returns the value of a property, as given in the syntax "section.property". It's a wrapper
     * method for the more low-level <code>getProperty(String, String, String)</code>
     * method of the <code>Configuration</code> class.
     * 
     * @param property  The name of the property that you want to get, in the form of "section.property".
     * @return A string containing the value of the property.
     * @see #get(String, String)
     */
    public String get(String property) {
        String temp[] = new String[2];
        temp = property.split("\\.");
        return config.getProperty(temp[1], "", temp[0]);
    }

    /**
     * Sets the given property to the specified value. It's another wrapper method.
     * 
     * @param property  The name of the property that you want to set, in the form of "section.property".
     * @param value     A string containing the value of the property to set.
     */
    public void set(String property, String value) {
        String temp[] = property.split("\\.");
        config.setProperty(temp[1], value, temp[0]);
    }

    /**
     * Gets the integer value of the given property.
     * 
     * @param property  The name of the property that you want to get, in the form of "section.property".
     * @return The integer value of the named property. Note that it doesn't check to make sure that the value
     * actually <i>is</i> an int, so be careful.
     */
    public int getAsInt(String property) {
        return Integer.parseInt(get(property));
    }

    /**
     * Gets the boolean value of the given property.
     * 
     * @param property  The name of the property that you want to get, in the form of "section.property".
     * @return The boolean value of the named property. Note that if the property isn't boolean, it will
     * always return false.
     */
    public boolean getAsBoolean(String property) {
        String temp = get(property);
        return temp.equals("true");
    }

    /**
     * Gets the names of the last few recently-used (opened, saved, etc.) playlists.
     * 
     * @return An array of strings holding the filenames of the recently-used playlists.
     */
    public String[] getRecentListsArray() {
        String temp[] = new String[getAsInt("recent playlists.number of recent playlists")];
        int ctr;
        for (ctr = 0; ctr < temp.length; ctr++) {
            temp[ctr] = get("recent playlists.list" + ctr);
        }
        return temp;
    }

    /**
     * Gets the names of the last few recently-used (opened, saved, etc.) playlists.
     * 
     * @return An array of strings holding the names of the recently-used playlists.
     */
    public String[] getRecentNamesArray() {
        String temp[] = new String[getAsInt("recent playlists.number of recent playlists")];
        int ctr;
        for (ctr = 0; ctr < temp.length; ctr++) {
            temp[ctr] = get("recent playlists.listName" + ctr);
        }
        return temp;
    }

    /**
     * Removes a recent list entry given its number.
     * 
     * @param index The number of the recent list to be removed.
     */
    private void removeRecentList(int index) {
        if (get("recent playlists.list" + index).equals("none")) return;
        int ctr, max = getAsInt("recent playlists.number of recent playlists") - 1;
        String temp;
        for (ctr = index; ctr < max; ctr++) {
            temp = get("recent playlists.list" + (ctr + 1));
            set("recent playlists.list" + ctr, temp);
            temp = get("recent playlists.listName" + (ctr + 1));
            set("recent playlists.listName" + ctr, temp);
        }
        set("recent playlists.list" + max, "none");
        set("recent playlists.list" + max, "");
    }

    /**
     * Gets a list of supported playlist formats.
     * 
     * @return An array of strings holding the names of the supported playlist types. They're given by file extension, like m3u and pls.
     */
    public String[] getPlaylistTypesArray() {
        String temp[] = new String[getAsInt("supported playlists.number of supported types")];
        int ctr;
        for (ctr = 0; ctr < temp.length; ctr++) {
            temp[ctr] = get("supported playlists.type" + ctr).split(";")[0];
        }
        return temp;
    }

    /**
     * Gets suffix filters that select for supported playlist types.
     * 
     * @return An array of filters to select playlist files. 
     */
    public SuffixFilter[] getPlaylistFiltersArray() {
        SuffixFilter temp[] = new SuffixFilter[getAsInt("supported playlists.number of supported types")];
        String temp2[];
        int ctr;
        for (ctr = 0; ctr < temp.length; ctr++) {
            temp2 = get("supported playlists.type" + ctr).split(";");
            temp[ctr] = new SuffixFilter(temp2[0], temp2[1]);
        }
        return temp;
    }

    /**
     * Updates the recent lists so that the given one is on top. This variation lets you
     * specify the name of the list as well as its path. It doesn't quite work yet.
     * 
     * @param name      The name of the new playlist.
     * @param newItem   A String containing the path to the new playlist.
     * @see #updateRecentLists(String)
     */
    public void updateRecentLists(String name, String newItem) {
        updateRecentLists(newItem);
        set("recent playlists.listName0", name);
    }

    /**
     * Updates the recent lists so that the given one is first on the list. If it was already somewhere else in the list,
     * the entries above it are shifted down one and it is moved to the top. If it wasn't, all the entries are shifted down
     * one, the last one is discarded, and it is placed on top.
     * 
     * @param newItem   A String containing the path to the new playlist.
     */
    public void updateRecentLists(String newItem) {
        int ctr, numOfLists = getAsInt("recent playlists.number of recent playlists") - 1;
        boolean shift = false;
        String recentLists[] = getRecentListsArray();
        String recentNames[] = getRecentNamesArray();
        for (ctr = numOfLists; ctr >= 0; ctr--) {
            if (newItem.equals(recentLists[ctr]) && !shift) {
                shift = true;
            }
            if (shift && ctr != 0) {
                recentLists[ctr] = recentLists[ctr - 1];
                recentNames[ctr] = recentNames[ctr - 1];
            }
        }
        if (!shift) {
            for (ctr = numOfLists; ctr > 0; ctr--) {
                recentLists[ctr] = recentLists[ctr - 1];
                recentNames[ctr] = recentNames[ctr - 1];
            }
        }
        recentLists[0] = newItem;
        for (ctr = 0; ctr < numOfLists; ctr++) {
            set("recent playlists.list" + ctr, recentLists[ctr]);
            set("recent playlists.listName" + ctr, recentNames[ctr]);
        }
    }

    /**
     * Returns the value of a property, as given in the syntax "section.property", but lets you
     * specify a default value to use if the property doesn't exist.
     * 
     * @param property  The name of the property that you want to get, in the form of "section.property".
     * @param defValue  A string containing the default value to return if the property doesn't exist.
     * @return A string containing the value of the property, or the default value if that property
     * couldn't be found.
     */
    public String get(String property, String defValue) {
        String temp[] = new String[2];
        if (config == null) return defValue;
        temp = property.split("\\.");
        return config.getProperty(temp[1], defValue, temp[0]);
    }

    /**
     * Returns the given property's value as an integer, but allows you
     * to specify a default value to use if the property doesn't exist.
     * 
     * @param property  The name of the property that you want to get, in the form of "section.property".
     * @param defValue  A string containing the default value to return if the property doesn't exist. And yes,
     * it <i>has</i> to be a string, because it's getting parsed into an int.
     * 
     * @return The integer represented by this property. It doesn't check beforehand to be sure that the property
     * actually contains an int, so be careful.
     */
    public int getAsInt(String property, String defValue) {
        return Integer.parseInt(get(property, defValue));
    }

    /**
     * Sets the number of recent lists that should be saved.
     * 
     * @param num   The new number of recent lists to save.
     */
    public void setNumRecentLists(int num) {
        int ctr;
        set("recent playlists.number of recent playlists", num + "");
        for (ctr = 0; ctr < num; ctr++) {
            set("recent playlists.list" + ctr, get("recent playlists.list" + ctr, "none"));
            set("recent playlists.listName" + ctr, get("recent playlists.listName" + ctr, "No Name"));
        }
    }

    /**
     * Writes some text to the xml output file, automatically entering &amp; for "&" to avoid loading errors.
     * @param input The text to write.
     */
    private void write(String input) {
        input = input.replaceAll("&", "&amp;");
        try {
            out.write(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new config file to match the program version. All prefs values are
     * preserved if they exist and given default values if they don't.
     * 
     * @param file  The file that will store the prefs.
     */
    private void makeCleanConfigFile(File file) throws IOException {
        int ctr;
        out = new BufferedWriter(new FileWriter(file));
        write("<?xml version=\"1.0\" encoding=\"iso-8859-1\" ?>\n");
        write("<properties>\n");
        write("  <category name=\"booleans\">\n");
        write("    <property name=\"usePaths\" value=\"" + get("booleans.usePaths", "true") + "\"/>\n");
        write("    <property name=\"useListsMenu\" value=\"" + get("booleans.useListstMenu", "true") + "\"/>\n");
        write("    <property name=\"useLastList\" value=\"" + get("booleans.useLastList", "true") + "\"/>\n");
        write("    <property name=\"useDestPath\" value=\"" + get("booleans.useDestPath", "true") + "\"/>\n");
        write("    <property name=\"useLoadPath\" value=\"" + get("booleans.useLoadPath", "true") + "\"/>\n");
        write("    <property name=\"useSearchPath\" value=\"" + get("booleans.useSearchPath", "true") + "\"/>\n");
        write("    <property name=\"autoload\" value=\"" + get("booleans.autoload", "false") + "\"/>\n");
        write("    <property name=\"save main window position\" value=\"" + get("booleans.save main window position", "true") + "\"/>\n");
        write("    <property name=\"add recursively\" value=\"" + get("booleans.add recursively", "true") + "\"/>\n");
        write("    <property name=\"use log\" value=\"" + get("booleans.use log", "false") + "\"/>\n");
        write("    <property name=\"use rel paths\" value=\"" + get("booleans.use rel paths", "false") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"supported playlists\">\n");
        write("    <property name=\"number of supported types\" value=\"7\"/>\n");
        write("    <property name=\"preferred type\" value=\"0\"/>\n");
        write("    <property name=\"type0\" value=\"m3u;M3U Playlists\"/>\n");
        write("    <property name=\"type1\" value=\"pls;PLS Playlists\"/>\n");
        write("    <property name=\"type2\" value=\"b4s;B4S Playlists\"/>\n");
        write("    <property name=\"type3\" value=\"wpl;WPL Playlists\"/>\n");
        write("    <property name=\"type4\" value=\"asx;ASX Playlists\"/>\n");
        write("    <property name=\"type5\" value=\"wax;WAX Playlists\"/>\n");
        write("    <property name=\"type6\" value=\"wvx;WVX Playlists\"/>\n");
        write("  </category>\n");
        write("  <category name=\"recent playlists\">\n");
        write("    <property name=\"number of recent playlists\" value=\"" + get("recent playlists.number of recent playlists", "4") + "\"/>\n");
        for (ctr = 0; ctr < getAsInt("recent playlists.number of recent playlists", "4"); ctr++) {
            write("    <property name=\"list" + ctr + "\" value=\"" + get("recent playlists.list" + ctr, "none") + "\"/>\n");
            write("    <property name=\"listName" + ctr + "\" value=\"" + get("recent playlists.listName" + ctr, "No Name") + "\"/>\n");
        }
        write("  </category>\n");
        write("  <category name=\"paths\">\n");
        write("    <property name=\"song loading path\" value=\"" + get("paths.song loading path", "none") + "\"/>\n");
        write("    <property name=\"sending path\" value=\"" + get("paths.sending path", "none") + "\"/>\n");
        write("    <property name=\"playlist loading path\" value=\"" + get("paths.playlist loading path", "none") + "\"/>\n");
        write("    <property name=\"name of last playlist\" value=\"" + get("paths.name of last playlist", "none") + "\"/>\n");
        write("    <property name=\"missing song search path\" value=\"" + get("paths.missing song search path", System.getProperty("user.home")) + "\"/>\n");
        write("    <property name=\"autosearch path\" value=\"" + get("paths.autosearch path", System.getProperty("user.home")) + "\"/>\n");
        write("    <property name=\"logfile path\" value=\"" + get("paths.logfile path", System.getProperty("user.home") + File.separator + "PSE.log") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"general\">\n");
        write("    <property name=\"version string\" value=\"" + currVer + "\"/>\n");
        write("    <property name=\"kill dups preserve which\" value=\"" + get("general.kill dups preserve which", "first") + "\"/>\n");
        write("    <property name=\"max sending errors\" value=\"" + get("general.max sending errors", "5") + "\"/>\n");
        write("    <property name=\"scroll offset\" value=\"" + get("general.scroll offset", "3") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"main window\">\n");
        write("    <property name=\"x\" value=\"" + get("main window.x", "10") + "\"/>\n");
        write("    <property name=\"y\" value=\"" + get("main window.y", "10") + "\"/>\n");
        write("    <property name=\"width\" value=\"" + get("main window.width", "500") + "\"/>\n");
        write("    <property name=\"height\" value=\"" + get("main window.height", "550") + "\"/>\n");
        write("    <property name=\"title column width\" value=\"" + get("main window.title column width", "300") + "\"/>\n");
        write("    <property name=\"size column width\" value=\"" + get("main window.size column width", "70") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"log options\">\n");
        write("    <property name=\"log errors\" value=\"" + get("log options.log errors", "true") + "\"/>\n");
        write("    <property name=\"log list actions\" value=\"" + get("log options.log list actions", "false") + "\"/>\n");
        write("    <property name=\"log sending\" value=\"" + get("log options.log sending", "false") + "\"/>\n");
        write("    <property name=\"log adding\" value=\"" + get("log options.log adding", "false") + "\"/>\n");
        write("    <property name=\"log searching\" value=\"" + get("log options.log searching", "false") + "\"/>\n");
        write("    <property name=\"log size\" value=\"" + get("log options.log size", "15") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"search options\">\n");
        write("    <property name=\"autosearch recursively\" value=\"" + get("search options.autosearch recursively", "true") + "\"/>\n");
        write("    <property name=\"autosearch breadthwise\" value=\"" + get("search options.autosearch breadthwise", "true") + "\"/>\n");
        write("    <property name=\"autosearch metadata\" value=\"" + get("search options.autosearch metadata", "true") + "\"/>\n");
        write("    <property name=\"search recursively\" value=\"" + get("search options.search recursively", "true") + "\"/>\n");
        write("    <property name=\"search breadthwise\" value=\"" + get("search options.search breadthwise", "true") + "\"/>\n");
        write("    <property name=\"search metadata\" value=\"" + get("search options.search metadata", "true") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"upon loading\">\n");
        write("    <property name=\"autokill duplicates\" value=\"" + get("upon loading.autokill duplicates", "true") + "\"/>\n");
        write("    <property name=\"autokill preserve which\" value=\"" + get("upon loading.autokill preserve which", "first") + "\"/>\n");
        write("    <property name=\"autosearch\" value=\"" + get("upon loading.autosearch", "true") + "\"/>\n");
        write("  </category>\n");
        write("  <category name=\"data patterns\">\n");
        write("    <property name=\"num of patterns\" value=\"" + get("data patterns.num of patterns", "4") + "\"/>\n");
        write("    <property name=\"selected pattern\" value=\"" + get("data patterns.selected pattern", "0") + "\"/>\n");
        write("    <property name=\"pattern0\" value=\"" + get("data patterns.pattern0", "%a - %t") + "\"/>\n");
        write("    <property name=\"pattern1\" value=\"" + get("data patterns.pattern1", "%a - %n %t") + "\"/>\n");
        write("    <property name=\"pattern2\" value=\"" + get("data patterns.pattern2", "%a - %m: %t") + "\"/>\n");
        write("    <property name=\"pattern3\" value=\"" + get("data patterns.pattern3", "%a - %m: %n %t") + "\"/>\n");
        write("  </category>\n");
        write("</properties>\n");
        out.close();
    }
}
