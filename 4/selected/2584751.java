package cloudspace.vm.filesystem;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WebApp;
import cloudspace.config.CloudSpaceConfiguration;
import cloudspace.security.CloudController;

/**
 * Encapsulates the notion of a path to a resource in the CloudSpace storage
 * area, similar to how {@link java.io.File} abstracts the notion of a path on
 * the native file system. In fact, many of the methods provided by this class
 * mimic those provided by the File class. A CSPath can be a simple path (which
 * is rooted at the storage location configured by the administrator), or a
 * zone-path pair, where the zone defines a different root somewhere else in the
 * storage hierarchy.
 * 
 * @author Tony Allevato
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class CSPath implements Comparable<CSPath> {

    private static final String SNAPSHOT_ZONE = "snapshot";

    private String zone;

    private String path;

    private CSPath cachedParent;

    private File cachedPhysicalFile;

    public static final String ZONE_SEPARATOR = ":";

    public static final String SEPARATOR = "/";

    public static final char ZONE_SEPARATOR_CHAR = ':';

    public static final char SEPARATOR_CHAR = '/';

    private static final String WORK_ZONE = "work";

    private static final String SAMPLES_ZONE = "sample";

    private static Logger logger = Logger.getLogger(CSPath.class);

    /**
     * Creates a CSPath instance with the specified path. This constructor
     * supports paths of the form "/some/path" as well as zone-prefixed paths of
     * the form "zone:/some/path".
     * 
     * @param path
     *            the path
     */
    public CSPath(String path) {
        String[] parts = path.split(ZONE_SEPARATOR);
        if (parts.length == 1) {
            this.zone = null;
            this.path = normalizePath(parts[0]);
        } else {
            this.zone = parts[0];
            this.path = normalizePath(parts[1]);
        }
    }

    /**
     * Creates a CSPath instance with the specified zone and path.
     * 
     * @param zone
     *            the zone, which may be null
     * @param path
     *            the path
     */
    public CSPath(String zone, String path) {
        this.zone = zone;
        this.path = normalizePath(path);
    }

    /**
     * Creates a CSPath instance that represents a file or directory inside the
     * specified CSPath.
     * 
     * @param parent
     *            the parent CSPath
     * @param child
     *            the name of the child file or directory
     */
    public CSPath(CSPath parent, String child) {
        this(parent.getZone(), appendPathComponent(parent.getPath(), child));
    }

    /**
     * Gets the physical file system path for the file or folder that this
     * object represents.
     * 
     * @return a File object representing the physical file system location
     */
    public File getPhysicalFile() {
        if (cachedPhysicalFile == null) {
            cachedPhysicalFile = new File(getZoneLocation(), path);
        }
        return cachedPhysicalFile;
    }

    /**
     * Gets the zone for this file or directory.
     * 
     * @return the zone, which may be null
     */
    public String getZone() {
        return zone;
    }

    /**
     * Gets the path for this file or directory.
     * 
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the zone-qualified path for this file or directory. The return value
     * will be in the format "zone:/some/path/portion".
     * 
     * @return the zone-qualified path
     */
    public String getQualifiedPath() {
        if (zone != null) {
            return zone + ZONE_SEPARATOR + path;
        } else {
            return path;
        }
    }

    /**
     * Gets the parent of this file or directory.
     * 
     * @return the parent of this file or directory, or null if it is the root
     *         of its zone
     */
    public CSPath getParent() {
        if (isRoot()) {
            return null;
        } else {
            if (cachedParent == null) {
                int lastSlash = path.lastIndexOf(SEPARATOR_CHAR);
                String parentPath = path.substring(0, lastSlash);
                cachedParent = new CSPath(zone, parentPath);
            }
            return cachedParent;
        }
    }

    /**
     * Gets the name portion of this path; that is, everything after the final
     * separator.
     * 
     * @return the name portion of this path
     */
    public String getName() {
        int lastSlash = path.lastIndexOf(SEPARATOR_CHAR);
        return (lastSlash == -1) ? "" : path.substring(lastSlash + 1);
    }

    /**
     * Gets the extension of this path; that is, the portion after the final dot
     * in the name.
     * 
     * @return the extension of the path, or the empty string if there is none
     */
    public String getFileExtension() {
        String name = getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? "" : name.substring(lastDot + 1);
    }

    /**
     * Gets a list of children of this path, if it represents a directory. Only
     * those entries that the calling user has at least read access to are
     * returned.
     * 
     * @return a list of CSPaths representing the accessible children of this
     *         path, or null if it represents a file
     */
    public List<CSPath> getChildren() {
        if (isDirectory()) {
            List<CSPath> children = new ArrayList<CSPath>();
            String[] childNames = getPhysicalFile().list();
            for (String childName : childNames) {
                CSPath child = new CSPath(this, childName);
                try {
                    CloudController.checkRead(child);
                    children.add(child);
                } catch (AccessControlException e) {
                }
            }
            Collections.sort(children);
            return children;
        } else {
            return null;
        }
    }

    /**
     * Gets a list of child directories of this path, if it represents a
     * directory. Only those entries that the calling user has at least read
     * access to are returned.
     * 
     * @return a list of CSPaths representing the accessible children of this
     *         path, or null if it represents a file
     */
    public List<CSPath> getChildDirectories() {
        if (isDirectory()) {
            List<CSPath> children = new ArrayList<CSPath>();
            String[] childNames = getPhysicalFile().list();
            if (childNames == null) {
                return children;
            }
            for (String childName : childNames) {
                CSPath child = new CSPath(this, childName);
                if (child.isDirectory()) {
                    try {
                        CloudController.checkRead(child);
                        children.add(child);
                    } catch (AccessControlException e) {
                    }
                }
            }
            Collections.sort(children);
            return children;
        } else {
            return null;
        }
    }

    /**
     * Gets a value indicating whether this path represents a file that
     * currently exists on the file system.
     * 
     * @return true if the file exists; otherwise, false
     */
    public boolean exists() {
        return getPhysicalFile().exists();
    }

    /**
     * Gets a value indicating whether this path is the root of its zone.
     * 
     * @return true if the path is the root of its zone, otherwise false
     */
    public boolean isRoot() {
        return path.equals(SEPARATOR);
    }

    /**
     * Gets a value indicating whether this path represents a directory.
     * 
     * @return true if the path is a directory, otherwise false
     */
    public boolean isDirectory() {
        return getPhysicalFile().isDirectory();
    }

    /**
     * Gets a value indicating whether this path represents a file.
     * 
     * @return true if the path is a file, otherwise false
     */
    public boolean isFile() {
        return getPhysicalFile().isFile();
    }

    /**
     * Gets a value indicating whether this path represents a hidden file or
     * directory.
     * 
     * @return true if the file or directory is hidden, otherwise false
     */
    public boolean isHidden() {
        return getPhysicalFile().isHidden();
    }

    /**
     * Creates the directory that this path represents.
     * 
     * @return true if and only if the directory was created, otherwise false
     * @throws AccessControlException
     *             if the calling user does not have write access to this path
     */
    public boolean mkdir() throws AccessControlException {
        CloudController.checkWrite(this);
        return getPhysicalFile().mkdir();
    }

    /**
     * Creates a new empty file at the path that this object represents.
     * 
     * @return true if and only if the file was created, otherwise false
     * @throws AccessControlException
     *             if the calling user does not have write access to this path
     * @throws IOException
     *             if another I/O error occurs
     */
    public boolean createNewFile() throws AccessControlException, IOException {
        CloudController.checkWrite(this);
        return getPhysicalFile().createNewFile();
    }

    /**
     * Copies the file or directory represented by this path to the specified
     * new path.
     * 
     * @param newPath
     *            the new path
     * @throws AccessControlException
     *             if the calling user does not have read access to this path or
     *             write access the new path
     */
    public boolean copyTo(CSPath newPath) throws AccessControlException {
        CloudController.checkRead(this);
        CloudController.checkWrite(newPath);
        try {
            if (isDirectory()) {
                FileUtils.copyDirectoryToDirectory(getPhysicalFile(), newPath.getPhysicalFile());
            } else {
                FileUtils.copyFileToDirectory(getPhysicalFile(), newPath.getPhysicalFile());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Moves the file or directory represented by this path to the specified new
     * path.
     * 
     * @param newPath
     *            the new path
     * @throws AccessControlException
     *             if the calling user does not have write access to this path
     *             or the new path
     */
    public boolean moveTo(CSPath newPath) throws AccessControlException {
        CloudController.checkWrite(this);
        CloudController.checkWrite(newPath);
        try {
            if (isDirectory()) {
                FileUtils.copyDirectoryToDirectory(getPhysicalFile(), newPath.getPhysicalFile());
                FileUtils.deleteDirectory(getPhysicalFile());
            } else {
                FileUtils.copyFileToDirectory(getPhysicalFile(), newPath.getPhysicalFile());
                FileUtils.forceDelete(getPhysicalFile());
            }
            return true;
        } catch (IOException e) {
            logger.error("Could not move file", e);
            return false;
        }
    }

    /**
     * Deletes the file or directory at the specified path. If it is a directory
     * and it is not empty, this method will attempt to delete its contents
     * recursively; the directory itself will then only be deleted if its
     * contents were successfully deleted (in other words, the calling user had
     * write access to the entire hierarchy underneath this directory).
     * 
     * @return true if and only if the file or directory was successfully
     *         deleted, otherwise false
     * @throws AccessControlException
     *             if the calling user does not have write access to this path
     */
    public boolean delete() throws AccessControlException {
        CloudController.checkWrite(this);
        if (isDirectory()) {
            for (CSPath child : getChildren()) {
                child.delete();
            }
        }
        return getPhysicalFile().delete();
    }

    /**
     * Returns the zone-qualified path.
     * 
     * @return the zone-qualified path
     */
    public String toString() {
        return getQualifiedPath();
    }

    public int compareTo(CSPath other) {
        return toString().compareTo(other.toString());
    }

    public boolean equals(Object object) {
        if (object instanceof CSPath) {
            CSPath other = (CSPath) object;
            if (getZone() != null) {
                return getZone().equals(other.getZone()) && getPath().equals(other.getPath());
            } else {
                return getPath().equals(other.getPath());
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (zone != null ? (zone.hashCode() >> 16) : 0) | path.hashCode();
    }

    public static CSPath pathForUrlBaseDir(String baseDir) {
        CSPath path;
        if (baseDir.startsWith("/work")) {
            path = new CSPath(WORK_ZONE, baseDir.substring("/work".length()));
        } else {
            path = new CSPath(baseDir);
        }
        return path;
    }

    private File getZoneLocation() {
        CloudSpaceConfiguration config = CloudSpaceConfiguration.getInstance();
        if (zone == null) {
            WebApp webApp = Executions.getCurrent().getDesktop().getWebApp();
            return new File(webApp.getRealPath("/"));
        } else if (WORK_ZONE.equals(zone)) {
            return new File(config.getStorageLocation(), "work");
        } else if (SAMPLES_ZONE.equals(zone)) {
            WebApp webApp = Executions.getCurrent().getDesktop().getWebApp();
            return new File(webApp.getRealPath("sample"));
        } else if (SNAPSHOT_ZONE.equals(zone)) {
            return new File(config.getStorageLocation(), SNAPSHOT_ZONE);
        } else {
            throw new IllegalArgumentException("Unsupported zone: " + zone);
        }
    }

    /**
     * Appends the child name to the specified path, taking into account the
     * presence or absence of the path separator if necessary.
     * 
     * @param path
     *            the parent path
     * @param child
     *            the child name
     * @return the full path
     */
    private static String appendPathComponent(String path, String child) {
        if (path.endsWith(SEPARATOR)) {
            return path + child;
        } else {
            return path + SEPARATOR + child;
        }
    }

    private static String normalizePath(String path) {
        if (path.endsWith(SEPARATOR)) {
            path = path.substring(0, path.length() - SEPARATOR.length());
        }
        if (!path.startsWith(SEPARATOR)) {
            path = SEPARATOR + path;
        }
        return path;
    }

    public boolean isProectDirectory() {
        return ProjectSpec.isProjectDirectory(this);
    }
}
