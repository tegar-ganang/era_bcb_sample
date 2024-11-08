package net.rptools.lib.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.rptools.lib.CodeTimer;
import net.rptools.lib.FileUtil;
import net.rptools.lib.GUID;
import net.rptools.lib.ModelVersionManager;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import com.thoughtworks.xstream.XStream;

/**
 * Represents a container of content/files within a single actual file.
 * <p>
 * A packed file contains three parts:
 * <ul>
 * <li>Contents - A single object that represents the core content of the packed file, as a convenience method
 * <li>Properties - A String to Object map of arbitrary properties that can be used to describe the packed file
 * <li>Files - Any arbitrary files/data that are packed within the packed file
 * </ul>
 * <p>
 * The implementation uses two {@link Set}s, <b>addedFileSet</b> and <b>removedFileSet</b>,
 * to keep track of paths for content that has been added or removed from the packed
 * file, respectively.  This is because the actual file itself isn't written until the {@link #save()}
 * method is called, yet the application may want to dynamically add and remove paths to
 * the packed file and query the state of which files are currently included or excluded
 * from the packed file.
 * <p>
 * In addition, the API allows for storing multiple types of objects into the packed file.  The
 * easiest to understand are byte streams as they are binary values that are not modified
 * by character set encoding during output.  They are represented by an array of bytes or
 * an {@link InputStream}.
 * <p>
 * The second type of data is the file, represented here as a URL as it is more universally
 * applicable (although currently it is unused outside this class).  URLs have their content
 * retrieved from the source and are currently written into the packed file without any
 * character encoding (it is possible that the <code>Content-Type</code> of the data
 * stream could provide information on how the data should be written so this may
 * change in the future).
 * <p>
 * The last and most important type of data is the POJO (plain old Java object).  These
 * are converted into XML using the XStream library from codehaus.org.  As the data
 * is written to the output file it is character set encoded to UTF-8.  It is hoped that
 * this solves the localization issues with saved macros and other data not being
 * restored properly.
 * Because of this, data loaded from a packed file is always retrieved without
 * character set encoding <b>unless</b> it is XML data.  This should preserve
 * binary data such as JPEG and PNG images properly.  A side effect of this is that
 * all character data should be written to the packed file as POJOs in order to obtain
 * the automatic character set encoding.  (Otherwise, strings can be converted to
 * UTF-8 using the {@link String#getBytes(String)} method.
 */
public class PackedFile {

    private static final String PROPERTY_FILE = "properties.xml";

    private static final String CONTENT_FILE = "content.xml";

    private static final Logger log = Logger.getLogger(PackedFile.class);

    private static File tmpDir = new File(System.getProperty("java.io.tmpdir"));

    private final XStream xstream = new XStream();

    private final File file;

    private final File tmpFile;

    private boolean dirty;

    private boolean propsLoaded;

    private Map<String, Object> propertyMap = new HashMap<String, Object>();

    private final Set<String> addedFileSet = new HashSet<String>();

    private final Set<String> removedFileSet = new HashSet<String>();

    private ModelVersionManager versionManager;

    /**
	 * By default all temporary files are handled in /tmp.  Use this method
	 * to globally set the location of the temporary directory
	 */
    public static void init(File tmpDir) {
        PackedFile.tmpDir = tmpDir;
    }

    public void setModelVersionManager(ModelVersionManager versionManager) {
        this.versionManager = versionManager;
    }

    /**
	 * Useful for configuring the xstream for object serialization
	 */
    public XStream getXStream() {
        return xstream;
    }

    public PackedFile(File file) {
        this.file = file;
        dirty = !file.exists();
        tmpFile = new File(tmpDir.getAbsolutePath() + "/" + new GUID() + ".tmp");
    }

    /**
	 * Retrieves the property map from the campaign file and accesses the given key,
	 * returning an Object that the key holds.  The Object is constructed from the
	 * XML content and could be anything.
	 * 
	 * @param key key for accessing the property map
	 * @return the value (typically a String)
	 * @throws IOException
	 */
    public Object getProperty(String key) throws IOException {
        return getPropertyMap().get(key);
    }

    /**
	 * Returns a list of all keys in the campaign's property map.
	 * See also {@link #getProperty(String)}.
	 * 
	 * @return list of all keys
	 * @throws IOException
	 */
    public Iterator<String> getPropertyNames() throws IOException {
        return getPropertyMap().keySet().iterator();
    }

    /**
	 * Stores a new key/value pair into the property map.  Existing keys are
	 * overwritten.
	 * 
	 * @param key
	 * @param value any POJO; will be serialized into XML upon writing
	 * @return the previous value for the given key
	 * @throws IOException
	 */
    public Object setProperty(String key, Object value) throws IOException {
        dirty = true;
        return getPropertyMap().put(key, value);
    }

    /**
	 * Remove the property with the associated key from the property map.
	 * 
	 * @param key
	 * @return the previous value for the given key
	 * @throws IOException
	 */
    public Object removeProperty(String key) throws IOException {
        dirty = true;
        return getPropertyMap().remove(key);
    }

    /**
	 * Retrieves the contents of the <code>CONTENT_FILE</code> as a POJO.
	 * This object is the top-level data structure for all information regarding the
	 * content of the PackedFile.
	 * 
	 * @return the results of the deserialization
	 * @throws IOException
	 */
    public Object getContent() throws IOException {
        return getContent(versionManager, (String) getProperty("version"));
    }

    /**
	 * Same as {@link #getContent()} except that the version can be specified.  This
	 * allows a newer release of an application to provide automatic transformation
	 * information that will be applied to the XML as the object is deserialized.
	 * The default trasnformation manager is used.
	 * (Think of the transformation as a simplifed XSTL process.)
	 * 
	 * @param fileVersion such as "1.3.70"
	 * @return the results of the deserialization
	 * @throws IOException
	 */
    public Object getContent(String fileVersion) throws IOException {
        return getContent(versionManager, fileVersion);
    }

    /**
	 * Same as {@link #getContent(String)} except that the transformation manager,
	 * <code>versionManager</code>, is specified as a parameter.
	 * 
	 * @param versionManager which set of transforms to apply to older file versions
	 * @param fileVersion such as "1.3.70"
	 * @return the results of the deserialization
	 * @throws IOException
	 */
    public Object getContent(ModelVersionManager versionManager, String fileVersion) throws IOException {
        try {
            if (versionManager != null) {
                Reader r = getFileAsReader(CONTENT_FILE);
                String xml = IOUtils.toString(r);
                xml = versionManager.transform(xml, fileVersion);
                return xstream.fromXML(xml);
            } else {
                return getFileObject(CONTENT_FILE);
            }
        } catch (NullPointerException npe) {
            log.error("Problem finding/converting content file", npe);
            return null;
        }
    }

    protected Map<String, Object> getPropertyMap() throws IOException {
        if (hasFile(PROPERTY_FILE) && !propsLoaded) {
            propertyMap = null;
            try {
                Object obj = getFileObject(PROPERTY_FILE);
                if (obj instanceof Map<?, ?>) {
                    propertyMap = (Map<String, Object>) obj;
                    propsLoaded = true;
                } else log.error("Unexpected class type for property object: " + obj.getClass().getName());
            } catch (NullPointerException npe) {
                log.error("Problem finding/converting property file", npe);
            }
        }
        return propertyMap;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void save() throws IOException {
        CodeTimer saveTimer;
        if (!dirty) {
            return;
        }
        saveTimer = new CodeTimer("PackedFile.save");
        saveTimer.setEnabled(log.isDebugEnabled());
        File newFile = new File(tmpDir.getAbsolutePath() + "/" + new GUID() + ".pak");
        ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(newFile)));
        zout.setLevel(1);
        try {
            saveTimer.start("contentFile");
            if (hasFile(CONTENT_FILE)) {
                zout.putNextEntry(new ZipEntry(CONTENT_FILE));
                InputStream is = getFileAsInputStream(CONTENT_FILE);
                IOUtils.copy(is, zout);
                zout.closeEntry();
            }
            saveTimer.stop("contentFile");
            saveTimer.start("propertyFile");
            if (getPropertyMap().isEmpty()) {
                removeFile(PROPERTY_FILE);
            } else {
                zout.putNextEntry(new ZipEntry(PROPERTY_FILE));
                xstream.toXML(getPropertyMap(), zout);
                zout.closeEntry();
            }
            saveTimer.stop("propertyFile");
            saveTimer.start("addFiles");
            addedFileSet.remove(CONTENT_FILE);
            for (String path : addedFileSet) {
                zout.putNextEntry(new ZipEntry(path));
                InputStream is = getFileAsInputStream(path);
                IOUtils.copy(is, zout);
                zout.closeEntry();
            }
            saveTimer.stop("addFiles");
            saveTimer.start("copyFiles");
            if (file.exists()) {
                Enumeration<? extends ZipEntry> entries = zFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && !addedFileSet.contains(entry.getName()) && !removedFileSet.contains(entry.getName()) && !CONTENT_FILE.equals(entry.getName()) && !PROPERTY_FILE.equals(entry.getName())) {
                        zout.putNextEntry(entry);
                        InputStream is = getFileAsInputStream(entry.getName());
                        IOUtils.copy(is, zout);
                        zout.closeEntry();
                    } else if (entry.isDirectory()) {
                        zout.putNextEntry(entry);
                        zout.closeEntry();
                    }
                }
            }
            try {
                if (zFile != null) zFile.close();
            } catch (IOException e) {
            }
            zFile = null;
            saveTimer.stop("copyFiles");
            saveTimer.start("close");
            zout.close();
            zout = null;
            saveTimer.stop("close");
            saveTimer.start("backup");
            File backupFile = new File(tmpDir.getAbsolutePath() + "/" + new GUID() + ".mv");
            if (file.exists()) {
                backupFile.delete();
                if (!file.renameTo(backupFile)) {
                    FileUtil.copyFile(file, backupFile);
                    file.delete();
                }
            }
            saveTimer.stop("backup");
            saveTimer.start("finalize");
            if (!newFile.renameTo(file)) FileUtil.copyFile(newFile, file);
            if (backupFile.exists()) backupFile.delete();
            saveTimer.stop("finalize");
            dirty = false;
        } finally {
            saveTimer.start("cleanup");
            try {
                if (zFile != null) zFile.close();
            } catch (IOException e) {
            }
            if (newFile.exists()) newFile.delete();
            try {
                if (zout != null) zout.close();
            } catch (IOException e) {
            }
            saveTimer.stop("cleanup");
            if (log.isDebugEnabled()) log.debug(saveTimer);
            saveTimer = null;
        }
    }

    /**
	 * Set the given object as the information to write to the 'content.xml' file in the archive.
	 * 
	 * @param content
	 * @throws IOException
	 */
    public void setContent(Object content) throws IOException {
        putFile(CONTENT_FILE, content);
    }

    /**
	 * Does the work of preparing for output to a temporary file, returning the {@link File}
	 * object associated with the temporary location.  The caller is then expected to
	 * open and write their data to the file which will later be added to the ZIP file.
	 *
	 * @param path path within the ZIP to write to
	 * @return the <code>File</code> object for the temporary location
	 * @throws IOException
	 */
    private File putFileImpl(String path) throws IOException {
        if (!tmpFile.exists()) tmpFile.getParentFile().mkdirs();
        File explodedFile = getExplodedFile(path);
        if (explodedFile.exists()) {
            explodedFile.delete();
        } else {
            explodedFile.getParentFile().mkdirs();
        }
        addedFileSet.add(path);
        removedFileSet.remove(path);
        dirty = true;
        return explodedFile;
    }

    /**
	 * Write the <code>byte</code> data to the given path in the ZIP file; as the data
	 * is binary there is no {@link Charset} conversion.
	 *
	 * @param path location within the ZIP file
	 * @param data the binary data to be written
	 * @throws IOException
	 */
    public void putFile(String path, byte[] data) throws IOException {
        putFile(path, new ByteArrayInputStream(data));
    }

    /**
	 * Write the <b>binary</b> data to the given path in the ZIP file; as the data is
	 * presumed to be binary there is no charset conversion.
	 *
	 * @param path location within the ZIP file
	 * @param data the binary data to be written in the form of an InputStream
	 * @throws IOException
	 */
    public void putFile(String path, InputStream is) throws IOException {
        File explodedFile = putFileImpl(path);
        FileOutputStream fos = new FileOutputStream(explodedFile);
        IOUtils.copy(is, fos);
        fos.close();
    }

    /**
	 * Write the serialized object to the given path in the ZIP file; as the data is an
	 * object it is first converted to XML and character set encoding will take place
	 * as the data is written to the (temporary) file.
	 *
	 * @param path location within the ZIP file
	 * @param obj the object to be written
	 * @throws IOException
	 */
    public void putFile(String path, Object obj) throws IOException {
        File explodedFile = putFileImpl(path);
        FileOutputStream fos = new FileOutputStream(explodedFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bw = new BufferedWriter(osw);
        xstream.toXML(obj, bw);
        bw.newLine();
        bw.close();
    }

    /**
	 * Write the data from the given URL to the path in the ZIP file; as the data
	 * is presumed binary there is no {@link Charset} conversion.
	 *<p>
	 * FIXME Should the MIME type of the InputStream be checked??
	 *
	 * @param path location within the ZIP file
	 * @param data the binary data to be written
	 * @throws IOException
	 */
    public void putFile(String path, URL url) throws IOException {
        InputStream is = url.openStream();
        putFile(path, is);
    }

    public boolean hasFile(String path) throws IOException {
        if (removedFileSet.contains(path)) return false;
        File explodedFile = getExplodedFile(path);
        if (explodedFile.exists()) return true;
        boolean ret = false;
        if (file.exists()) {
            ZipFile zipFile = getZipFile();
            ZipEntry ze = zipFile.getEntry(path);
            ret = (ze != null);
        }
        return ret;
    }

    private ZipFile zFile = null;

    private ZipFile getZipFile() throws IOException {
        if (zFile == null) zFile = new ZipFile(file);
        return zFile;
    }

    /**
	 * Returns a POJO by reading the contents of the zip archive path specified and
	 * converting the XML via the associated XStream object.
	 * (Because the XML is character data, this routine calls
	 * {@link #getFileAsReader(String)} to handle character encoding.)
	 * <p>
	 * <b>TODO:</b> add {@link ModelVersionManager} support
	 * 
	 * @param path zip file archive path entry
	 * @return Object created by translating the XML
	 * @throws IOException
	 */
    public Object getFileObject(String path) throws IOException {
        Reader r = getFileAsReader(path);
        return xstream.fromXML(r);
    }

    /**
	 * Returns an InputStreamReader that corresponds to the zip file path specified.
	 * This method should be called only for character-based file contents such as
	 * the <b>CONTENT_FILE</b> and <b>PROPERTY_FILE</b>.  For binary
	 * data, such as images (assets and thumbnails) use {@link #getFileAsInputStream(String)}
	 * instead.
	 * 
	 * @param path zip file archive path entry
	 * @return Reader representing the data stream
	 * @throws IOException
	 */
    public Reader getFileAsReader(String path) throws IOException {
        File explodedFile = getExplodedFile(path);
        if ((!file.exists() && !tmpFile.exists() && !explodedFile.exists()) || removedFileSet.contains(path)) throw new FileNotFoundException(path);
        if (explodedFile.exists()) return FileUtil.getFileAsReader(explodedFile);
        ZipEntry entry = new ZipEntry(path);
        ZipFile zipFile = getZipFile();
        InputStream in = null;
        try {
            in = new BufferedInputStream(zipFile.getInputStream(entry));
            if (in != null) {
                if (log.isDebugEnabled()) {
                    String type;
                    type = FileUtil.getContentType(in);
                    if (type == null) type = FileUtil.getContentType(explodedFile);
                    log.debug("FileUtil.getContentType() returned " + (type != null ? type : "(null)"));
                }
                return new InputStreamReader(in);
            }
        } catch (Exception ex) {
        }
        throw new FileNotFoundException(path);
    }

    /**
	 * Returns an InputStream that corresponds to the zip file path specified.
	 * This method should be called only for binary file contents such as
	 * images (assets and thumbnails).
	 * For character-based data, use {@link #getFileAsReader(String)}
	 * instead.
	 * 
	 * @param path zip file archive path entry
	 * @return InputStream representing the data stream
	 * @throws IOException
	 */
    public InputStream getFileAsInputStream(String path) throws IOException {
        File explodedFile = getExplodedFile(path);
        if ((!file.exists() && !tmpFile.exists() && !explodedFile.exists()) || removedFileSet.contains(path)) throw new FileNotFoundException(path);
        if (explodedFile.exists()) return FileUtil.getFileAsInputStream(explodedFile);
        ZipEntry entry = new ZipEntry(path);
        ZipFile zipFile = getZipFile();
        InputStream in = null;
        try {
            in = zipFile.getInputStream(entry);
            if (in != null) {
                String type = FileUtil.getContentType(in);
                if (log.isDebugEnabled() && type != null) log.debug("FileUtil.getContentType() returned " + type);
                return in;
            }
        } catch (Exception ex) {
        }
        throw new FileNotFoundException(path);
    }

    public void close() {
        if (zFile != null) {
            try {
                zFile.close();
            } catch (IOException e) {
            }
            zFile = null;
        }
        if (tmpFile.exists()) FileUtil.delete(tmpFile);
        propertyMap.clear();
        addedFileSet.clear();
        removedFileSet.clear();
        propsLoaded = false;
        dirty = !file.exists();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    protected File getExplodedFile(String path) {
        return new File(tmpFile.getAbsolutePath() + "/" + path);
    }

    /**
	 * Get all of the path names for this packed file.
	 *
	 * @return All the path names. Changing this set does not affect the packed file. Changes to the
	 * file made after this method is called are not reflected in the path and do not cause a
	 * ConcurrentModificationException. Directories in the packed file are also included in the set.
	 * @throws IOException Problem with the zip file.
	 */
    public Set<String> getPaths() throws IOException {
        Set<String> paths = new HashSet<String>(addedFileSet);
        paths.add(CONTENT_FILE);
        paths.add(PROPERTY_FILE);
        if (file.exists()) {
            ZipFile zf = getZipFile();
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                paths.add(e.nextElement().getName());
            }
        }
        paths.removeAll(removedFileSet);
        return paths;
    }

    /** @return Getter for file */
    public File getPackedFile() {
        return file;
    }

    /**
	 * Return a URL for a path in this file.
	 *
	 * @param path Get the url for this path
	 * @return URL that can be used to access the file.
	 * @throws IOException invalid zip file.
	 */
    public URL getURL(String path) throws IOException {
        if (!hasFile(path)) throw new FileNotFoundException("The path '" + path + "' is not in this packed file.");
        try {
            File explodedFile = getExplodedFile(path);
            if (explodedFile.exists()) return explodedFile.toURI().toURL();
            if (!path.startsWith("/")) path = "/" + path;
            String url = "jar:" + file.toURI().toURL().toExternalForm() + "!" + path;
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Couldn't create a url for path: '" + path + "'");
        }
    }

    /**
	 * Remove a path from the packed file.
	 *
	 * @param path Remove this path
	 */
    public void removeFile(String path) {
        removedFileSet.add(path);
        addedFileSet.remove(path);
        File explodedFile = getExplodedFile(path);
        if (explodedFile.exists()) {
            explodedFile.delete();
        }
        dirty = true;
    }

    /**
	 * Remove all files and directories from the zip file.
	 *
	 * @throws IOException Problem reading the zip file.
	 */
    public void removeAll() throws IOException {
        Set<String> paths = getPaths();
        for (String path : paths) {
            removeFile(path);
        }
    }

    /**
	 * Create an output stream that will be written to the packed file. Caller is
	 * responsible for closing the stream.
	 *
	 * @param path Path of the file being saved.
	 * @return Stream that can be used to write the data.
	 * @throws IOException Error opening the stream.
	 */
    public OutputStream getOutputStream(String path) throws IOException {
        if (!tmpFile.exists()) {
            tmpFile.mkdirs();
        }
        File explodedFile = getExplodedFile(path);
        dirty = true;
        if (explodedFile.exists()) {
            return new FileOutputStream(explodedFile);
        } else {
            explodedFile.getParentFile().mkdirs();
        }
        addedFileSet.add(path);
        removedFileSet.remove(path);
        dirty = true;
        return new FileOutputStream(explodedFile);
    }
}
