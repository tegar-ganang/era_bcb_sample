package de.peathal.util;

import de.peathal.resource.L;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Peter Karich
 */
public class DirHelper extends FireTo {

    private String dir;

    private String appName;

    private List locs;

    /**
     * add a listener to this DirHelper to listen to changes of the directory
     */
    public static final String DIR = "directory";

    /** Creates a new instance of FileManager */
    public DirHelper(String appName) {
        this.appName = appName;
        locs = new ArrayList();
    }

    public void init() {
        if (locs.size() == 0) {
            addLocation(new DirProperties(DirProperties.DEV, true, true));
            addLocation(new DirProperties(DirProperties.ADMIN, true, true));
            addLocation(new DirProperties(DirProperties.USER, true, true));
        }
    }

    /**
     * This method returns an absolut file from specified relative file.
     * It will copy a default file to that location if available and specified
     * in DirProperties.
     * Test with .exists() if a default file was created.
     */
    public File getFile(String file) {
        DirProperties dp;
        List files = new ArrayList();
        for (int i = 0; i < locs.size(); i++) {
            dp = (DirProperties) locs.get(i);
            if (dp.isReadable()) {
                File g = new File(dp.getLocation() + slash() + file);
                if (g.exists()) files.add(g);
            }
        }
        if (files.size() == 0) {
            throw new UnsupportedOperationException("at least one DirProperty should get 'read=true'");
        } else if (files.size() == 1) {
            return (File) files.get(0);
        } else {
            File fromFile = (File) files.get(files.size() - 2);
            File toFile = (File) files.get(files.size() - 1);
            byte reading[] = new byte[2024];
            try {
                FileInputStream stream = new FileInputStream(fromFile);
                FileOutputStream outStr = new FileOutputStream(toFile);
                while (stream.read(reading) != -1) {
                    outStr.write(reading);
                }
            } catch (FileNotFoundException ex) {
                getLogger().severe("FileNotFound: while copying from " + fromFile + " to " + toFile);
            } catch (IOException ex) {
                getLogger().severe("IOException: while copying from " + fromFile + " to " + toFile);
            }
            return toFile;
        }
    }

    public void save(String file, Object obj) {
    }

    /**
     * This method sets the root directory to the default.
     */
    public void setDefaultDirectory() {
        if (!setDirectory(getSystem() + slash() + appName)) {
            if (!setDirectory(getHome() + slash() + "." + appName)) {
                dir = getHome();
            }
        }
    }

    public boolean setDirectory(String name) {
        try {
            _setDirectory(name);
            return true;
        } catch (FileNotFoundException exc) {
            getLogger().log(Level.WARNING, exc.getLocalizedMessage(), exc);
            return false;
        }
    }

    private static Logger logger;

    private synchronized Logger getLogger() {
        if (logger == null) logger = Logger.getLogger("de.peathal.util");
        return logger;
    }

    /**
     * This method changes the default directory to specified dir name.
     * Creates a directory of not already exists.
     *
     * @return true if successfully changed!
     */
    protected void _setDirectory(String name) throws FileNotFoundException {
        File file = new File(name);
        if (!file.exists()) {
            try {
                file.mkdirs();
            } catch (Throwable t) {
                throw new FileNotFoundException(L.tr("Directory_does_not_exist_and_can't_create_it:_") + file.getAbsolutePath());
            }
        }
        if (!file.isDirectory()) {
            throw new FileNotFoundException(L.tr("File_is_not_a_directory:") + file.getAbsolutePath());
        }
        if (!file.canRead()) {
            throw new FileNotFoundException(L.tr("Can't_read_from_dir:") + file.getAbsolutePath());
        }
        if (!file.canWrite()) {
            throw new FileNotFoundException(L.tr("Can't_write_to_dir:") + file.getAbsolutePath());
        }
        if (!name.endsWith("/")) name += '/';
        String old = dir;
        dir = name;
        fireChangeEvent(new PropertyChangeEvent(this, DIR, old, name));
    }

    public static String slash() {
        return System.getProperty("file.separator");
    }

    public String getDir() {
        if (dir == null) setDefaultDirectory();
        return dir;
    }

    public String getHome() {
        return System.getProperty("user.home");
    }

    public String getSystem() {
        return System.getProperty("user.dir");
    }

    public File getDirAsFile() {
        return new File(getDir());
    }

    public void clearLocations() {
        locs.clear();
    }

    public void addLocation(DirProperties dp) {
        locs.add(dp);
    }

    /** This method shows the specified url in the preferred browser.
     */
    public void show(String browser, URL url) throws IOException {
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler \"" + url + "\"");
        } else {
            String cmd = browser + " " + url.toString();
            Runtime.getRuntime().exec(cmd);
        }
    }
}
