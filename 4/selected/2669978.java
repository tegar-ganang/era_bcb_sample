package com.gorillalogic.faces.util;

import java.io.*;
import java.util.*;
import com.gorillalogic.dal.Table;
import com.gorillalogic.dal.AccessException;
import com.gorillalogic.config.ConfigurationException;
import com.gorillalogic.config.Module;
import com.gorillalogic.config.Preferences;
import com.gorillalogic.faces.beans.GlSession;
import com.gorillalogic.gosh.Script;
import com.gorillalogic.gosh.GoshOptions;
import com.gorillalogic.util.IOUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * manages the import/cache/refresh of non-jsp web resources
 *  
 */
public class ResourceMgr {

    static Logger logger = Logger.getLogger(ResourceMgr.class);

    public static final String WEB_RESOURCES_LOCATION = "resources";

    public static void refreshGXEWebFiles() throws ConfigurationException {
        String gHome = Preferences.getGorillaHome(null);
        if (gHome != null) {
            copyWebFiles(new File(gHome));
        }
    }

    public static void refreshModuleWebFiles() throws ConfigurationException {
        Module mod = Preferences.getCurrentModule();
        if (mod != null) {
            refreshModuleWebFiles(mod);
        }
    }

    public static void refreshModuleWebFiles(Module mod) throws ConfigurationException {
        File moduleDirectory = mod.getModuleDirectory();
        if (moduleDirectory != null) {
            copyWebFiles(moduleDirectory);
        }
    }

    private static void copyWebFiles(File sourceDir) throws ConfigurationException {
        File webdir = new File(sourceDir, WEB_RESOURCES_LOCATION);
        if (!webdir.exists()) {
            return;
        }
        Collection c = FileUtils.listFiles(webdir, null, true);
        Iterator i = c.iterator();
        while (i.hasNext()) {
            try {
                FileUtils.copyFileToDirectory(((File) i.next()), getWebResourcesDir());
            } catch (IOException e) {
                throw new ConfigurationException(logger, "Error copy user web files", e);
            }
        }
    }

    private static File _webResourcesDir = null;

    public static File getWebResourcesDir() throws ConfigurationException {
        if (_webResourcesDir == null) {
            File rootDir = new File(Preferences.getWCIWebappDir("."));
            _webResourcesDir = new File(rootDir, "pages/" + WEB_RESOURCES_LOCATION);
            _webResourcesDir.mkdir();
        }
        return _webResourcesDir;
    }

    /**
	 * take properties values which are WCI resources and make sure they're 
	 * in the resources directory
	 * <code>val</code> should be a file reference which is one of<br/> 
	 * (a) relative to moduleDir/resource<br/>
	 * (b) relative to GORILLA_HOME<br/>
	 * (c) absolute<br/>
	 * if (a) it will be left alone; if (b) or (c) it will be copied to 
	 * moduleDir/resources
	 * @param key the property we are talking about 
	 * @param val the value, a string file path
	 * @return a string file path with the name of the copied file, if any, or null if the source fuile could not be found 
	 */
    public static String assureResource(String key, String val, File moduleDir) throws IOException {
        File candidate = null;
        File source = null;
        File targetDir = new File(moduleDir, WEB_RESOURCES_LOCATION);
        boolean copySource = true;
        candidate = new File(val);
        if (isResourceCandidate(candidate)) {
            source = candidate;
        }
        if (source == null) {
            candidate = new File(targetDir, val);
            if (isResourceCandidate(candidate)) {
                source = candidate;
                copySource = false;
            }
        }
        if (source == null) {
            candidate = new File(Preferences.getGorillaHome(""), val);
            if (isResourceCandidate(candidate)) {
                source = candidate;
            }
        }
        if (source == null) {
            return null;
        }
        if (source.getParentFile().equals(targetDir)) {
            copySource = false;
        }
        if (copySource) {
            FileUtils.copyFileToDirectory(source, targetDir);
        }
        val = source.getName();
        return val;
    }

    private static boolean isResourceCandidate(File f) {
        return f.exists() && f.isFile() && f.canRead();
    }

    private static Hashtable resourceProp = new Hashtable();

    static {
        resourceProp.put(Preferences.WCI_LOGO_IMAGE, "");
        resourceProp.put(Preferences.WCI_STYLE_SHEET, "");
        resourceProp.put(Preferences.WCI_PAGE_HEADER, "");
        resourceProp.put(Preferences.WCI_PAGE_FOOTER, "");
    }

    public static boolean isResourceProp(String key) {
        return resourceProp.get(key) != null;
    }
}
