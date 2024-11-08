package gov.sns.apps.pta.rscmgt;

import gov.sns.apps.pta.MainApplication;
import gov.sns.tools.apputils.iconlib.IconLib;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;

/**
/**
 * <h4>ResourceManager</h4>
 * <p>
 * Utility class for managing the resources of the application.
 * </p>
 *
 * @since  Jun 23, 2010
 * @author Christopher K. Allen
 */
public class ResourceManager {

    /** The singleton preferences object */
    private static final Preferences PREFS_CONFIG;

    /** Name of the resources directory relative to the location of <code>MainApplication</code> */
    public static String STR_DIR_RESOURCES = "resources/";

    /**
     * Load the singleton class objects.
     */
    static {
        PREFS_CONFIG = Preferences.userNodeForPackage(ResourceManager.class);
    }

    /**
     * <p>
     * Returns the <code>Preferences</code> object associated with 
     * the application class.  These are user-specific preferences
     * for the application.
     * </p>
     *
     * @return  user preferences for the application
     * 
     * @since  Jun 11, 2009
     * @author Christopher K. Allen
     */
    public static Preferences getPreferences() {
        return PREFS_CONFIG;
    }

    /**
     * <p>
     * Return the URL for the local resource name.
     * <p>
     * Provides the URL for the named resource.  The  
     * name is given with respect to the application 
     * resource folder location.  Specifically, if the
     * given resource has name "SomeText.txt" then the 
     * returned value is
     * <br/>
     * <br/>
     * &emsp;  <code>file://../../../resources/SomeText.txt</code>
     * </p>
     *
     * @param strLocalName      local name of the resource
     * 
     * @return  <code>URL</code> of the local resource, or null if not present
     * 
     * @since  Jun 15, 2009
     * @author Christopher K. Allen
     */
    public static URL getResourceUrl(String strLocalName) {
        String strPathRel = ResourceManager.STR_DIR_RESOURCES + strLocalName;
        URL urlResrc = MainApplication.class.getResource(strPathRel);
        return urlResrc;
    }

    /**
     * Opens the named resource and connects an input stream 
     * to it. 
     *
     * @param strRscName  the name of the file with respect 
     *                    to the resources directory
     * 
     * @return            and input stream connected to the given resource
     * 
     * @throws IOException      unable to find the resource
     * 
     * @since  Nov 12, 2009
     * @author Christopher K. Allen
     */
    public static InputStream openResource(String strRscName) throws IOException {
        URL urlSrc = ResourceManager.getResourceUrl(strRscName);
        InputStream isPath = urlSrc.openStream();
        return isPath;
    }

    /**
     * <p>
     * Return a <code>Properties</code> object initialized
     * according to the property file name.  That is, we
     * assume that the argument is the file name of a
     * property map.  Such files have line-by-line formats
     * given by
     * <br/>
     * <br/>
     * &emsp;  <tt>key</tt> = <tt>value</tt>
     * <br/>
     * <br/>
     * where both <tt>key</tt> and <tt>value</tt> are ASCII
     * character strings.
     * </p>
     *
     * @param strMapFile        file name of property map
     * 
     * @return  a <code>Properties</code> object representing property file
     * 
     * @throws IOException      file is missing or corrupt (i.e., not a property map)
     * 
     * @since  Jun 16, 2009
     * @author Christopher K. Allen
     */
    public static Properties getProperties(String strMapFile) throws IOException {
        InputStream is = openResource(strMapFile);
        Properties map = new Properties();
        map.load(is);
        is.close();
        return map;
    }

    /**
     * <p>
     * Returns the icon created from the image with the given
     * resource name.  
     * </p> 
     * <p>
     * We first check if the indicated icon is part of the
     * <code>IconLib</code> suite of icons.  If so, the 
     * argument is formatted as follows:
     * <br/>
     * <br/>
     * &nbsp;  <code>strRscName</code> = 
     *         "<tt>IconLib:<i>group</i>:<i>image_file</i></tt>"
     * <br/>
     * <br/>
     * where <code><i>group</i></code> is the group name
     * (i.e., the sub-directory) in the icon library suite 
     * and <code><i>image_file</i></code> is the specific file
     * name of the image.  
     * </p>
     * <p>
     * If the icon is not in the <code>IconLib</code> suite,
     * then it should be the file name within the application's
     * resource directory.
     * </p>
     *
     * @param strRscName      name of the icon or image file name
     * 
     * @return  icon created from given image file, 
     *          or <code>null</code> if failure
     * 
     * @since  Jun 16, 2009
     * @author Christopher K. Allen
     */
    public static ImageIcon getImageIcon(String strRscName) {
        if (strRscName == null) return null;
        if (strRscName.startsWith("IconLib")) {
            String[] arrTokens = strRscName.split(":");
            ImageIcon icon = IconLib.getImageIcon(arrTokens[1], arrTokens[2]);
            return icon;
        }
        URL urlIcon = ResourceManager.getResourceUrl(strRscName);
        ImageIcon icnImage = new ImageIcon(urlIcon);
        return icnImage;
    }

    /**
     * Utility class - prevent any instances of
     * <code>ResourceManager</code> objects.
     *
     *
     * @since     Jun 16, 2009
     * @author    Christopher K. Allen
     */
    private ResourceManager() {
    }

    ;
}
