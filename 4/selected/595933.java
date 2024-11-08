package org.ximtec.igesture.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sigtec.util.Constant;

/**
 * Simplifies the access to ZIP files.
 * 
 * Java does not allow the modification of ZIP files. Therefore, a temporary
 * file is used to do write operations. After a close, the original file is
 * replaced by the temporary one.
 * 
 * @author UeliKurmann
 * @version 1.0
 * @since igesture
 */
public class ZipFS {

    public static final String SEPERATOR = "/";

    private ZipFile zipFile;

    private File originalZipFile;

    private File tmpZipFile;

    private ZipOutputStream zipOutput;

    private Map<String, Map<String, ZipEntry>> fileIndex;

    private Set<String> toIgnore;

    public ZipFS(File file) throws IOException {
        this.originalZipFile = file;
        if (this.originalZipFile.exists()) {
            zipFile = new ZipFile(this.originalZipFile);
            fileIndex = indexFile(zipFile);
        } else {
            fileIndex = new HashMap<String, Map<String, ZipEntry>>();
        }
        tmpZipFile = File.createTempFile(Long.toHexString(System.currentTimeMillis()), ".tmp");
        zipOutput = new ZipOutputStream(new FileOutputStream(tmpZipFile));
        toIgnore = new HashSet<String>();
    }

    /**
   * Indexes the ZIP file.
   * 
   * Iterates over each element and populates the fileIndex. The file index
   * allows random access to the elements (files and folders) in the ZIP file.
   * 
   * @throws IOException
   */
    private synchronized Map<String, Map<String, ZipEntry>> indexFile(ZipFile file) throws IOException {
        Map<String, Map<String, ZipEntry>> fileIndex = new HashMap<String, Map<String, ZipEntry>>();
        Enumeration<? extends ZipEntry> enumerator = file.entries();
        fileIndex.put(Constant.EMPTY_STRING, new HashMap<String, ZipEntry>());
        while (enumerator.hasMoreElements()) {
            ZipEntry entry = enumerator.nextElement();
            addEntryToIndex(fileIndex, entry);
        }
        return fileIndex;
    }

    /**
   * Helper Method used by indexFile().
   * 
   * @param zipEntry
   */
    private void addEntryToIndex(Map<String, Map<String, ZipEntry>> index, ZipEntry zipEntry) {
        String parentPath = getParentPath(zipEntry);
        createDirectoryEntry(index, parentPath);
        if (zipEntry.isDirectory()) {
            index.get(parentPath).put(getName(zipEntry), zipEntry);
        } else {
            index.get(parentPath).put(getName(zipEntry), zipEntry);
        }
    }

    /**
   * Creates recursively directory entries.
   * 
   * @param index
   * @param path
   */
    private void createDirectoryEntry(Map<String, Map<String, ZipEntry>> index, String path) {
        path = normalizePath(path);
        if (path.contains(SEPERATOR)) {
            createDirectoryEntry(index, path.substring(0, path.lastIndexOf(SEPERATOR)));
        }
        if (!index.containsKey(path)) {
            index.put(path, new HashMap<String, ZipEntry>());
            String parentPath = getParentPath(path);
            index.get(parentPath).put(getName(path), new ZipEntry(path + SEPERATOR));
        }
    }

    /**
   * Returns the name of a ZIP entry.
   * 
   * @param zipEntry
   * @return
   */
    public static String getName(ZipEntry zipEntry) {
        return getName(zipEntry.getName());
    }

    /**
   * Returns the substring after the last slash. If there is no slash, the
   * entire string is returned.
   * 
   * @param path
   * @return
   */
    public static String getName(String path) {
        path = normalizePath(path);
        int index = path.lastIndexOf(SEPERATOR) + 1;
        if (index == 0) {
            return path;
        } else {
            return path.substring(index);
        }
    }

    /**
   * Normalizes the path.
   * 
   * A path must not start/end with a slash.
   * 
   * @param path
   * @return
   */
    private static String normalizePath(String path) {
        while (path.endsWith(SEPERATOR)) {
            path = path.substring(0, path.length() - 1);
        }
        while (path.startsWith(SEPERATOR)) {
            path = path.substring(1);
        }
        return path;
    }

    /**
   * Returns the parent path of an element. If no parent path is available, an
   * EMPTY string is returned.
   * 
   * @see getParentPath(String path)
   * @param zipEntry
   * @return
   */
    public static String getParentPath(ZipEntry zipEntry) {
        return getParentPath(zipEntry.getName());
    }

    /**
   * Returns the parent path. If no path is available, an EMPTY string is
   * returned.
   * 
   * @param path
   * @return
   */
    public static String getParentPath(String path) {
        if (path == null) {
            return null;
        }
        path = normalizePath(path);
        int lastIndex = path.lastIndexOf(SEPERATOR);
        if (lastIndex == -1) {
            return Constant.EMPTY_STRING;
        } else {
            return path.substring(0, lastIndex);
        }
    }

    /**
   * Lists all elements in a path (files and folders)
   * 
   * @param path
   * @return
   */
    public List<ZipEntry> listFiles(String path) {
        path = normalizePath(path);
        if (fileIndex.containsKey(path)) {
            return new ArrayList<ZipEntry>(fileIndex.get(path).values());
        }
        return new ArrayList<ZipEntry>();
    }

    /**
   * Returns the input stream of a path.
   * 
   * @param path
   * @return
   * @throws IOException
   */
    public InputStream getInputStream(String path) throws IOException {
        return zipFile.getInputStream(getEntry(path));
    }

    /**
   * Returns the input stream of a path.
   * 
   * @param entry
   * @return
   * @throws IOException
   */
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return getInputStream(entry.getName());
    }

    /**
   * Returns the ZipEntry with the given path.
   * 
   * @param path
   * @return
   * @throws IOException
   */
    public ZipEntry getEntry(String path) throws IOException {
        String parentPath = getParentPath(path);
        String name = getName(path);
        Map<String, ZipEntry> entries = fileIndex.get(parentPath);
        ZipEntry entry = null;
        if (entries != null) {
            entry = entries.get(name);
        }
        if (entry == null || entry.isDirectory()) {
            throw new FileNotFoundException(path);
        }
        return entry;
    }

    public ZipFS(String fileName) throws IOException {
        this(new File(fileName));
    }

    /**
   * Marks an element to be ignored.
   * 
   * @param path
   */
    public void delete(String path) {
        toIgnore.add(normalizePath(path));
    }

    /**
   * Stores a new element in the ZIP file with the given path.
   * 
   * @param path
   * @param stream
   * @throws IOException
   */
    public void store(String path, InputStream stream) throws IOException {
        toIgnore.add(normalizePath(path));
        ZipEntry entry = new ZipEntry(path);
        zipOutput.putNextEntry(entry);
        IOUtils.copy(stream, zipOutput);
        zipOutput.closeEntry();
    }

    /**
   * Copies the elements to the output file.
   */
    private void copyEntries() {
        if (zipFile != null) {
            Enumeration<? extends ZipEntry> enumerator = zipFile.entries();
            while (enumerator.hasMoreElements()) {
                ZipEntry entry = enumerator.nextElement();
                if (!entry.isDirectory() && !toIgnore.contains(normalizePath(entry.getName()))) {
                    ZipEntry originalEntry = new ZipEntry(entry.getName());
                    try {
                        zipOutput.putNextEntry(originalEntry);
                        IOUtils.copy(getInputStream(entry.getName()), zipOutput);
                        zipOutput.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
   * Closes ZipFS
   * 
   * @throws IOException
   */
    public void close() throws IOException {
        if (zipFile != null) {
            copyEntries();
            zipOutput.close();
            zipFile.close();
            originalZipFile.delete();
            if (!originalZipFile.exists()) {
                FileUtils.moveFile(tmpZipFile, originalZipFile);
            }
        } else {
            store("readme", IOUtils.toInputStream("see http://www.igesture.org"));
            zipOutput.flush();
            zipOutput.close();
            if (!originalZipFile.exists()) {
                FileUtils.moveFile(tmpZipFile, originalZipFile);
            }
        }
    }
}
