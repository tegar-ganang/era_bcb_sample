package com.goodcodeisbeautiful.archtea.io.lfs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import com.goodcodeisbeautiful.archtea.io.ArchteaEntryException;
import com.goodcodeisbeautiful.archtea.io.DefaultLeafEntry;
import com.goodcodeisbeautiful.archtea.io.EntryManager;
import com.goodcodeisbeautiful.archtea.io.data.DataContainer;
import com.goodcodeisbeautiful.archtea.io.data.DataContainerReaderType;
import com.goodcodeisbeautiful.archtea.io.data.FileDataContainerAdapter;

/**
 * @author hata
 *
 */
public class FileSystemLeafEntry extends DefaultLeafEntry implements DataContainer {

    /** default buffer size */
    private static final int DEFAULT_BUFF_SIZE = 8192;

    /** local file path for this class. */
    private String m_path;

    /**
     *  a flag which is used to check this instance's attributes are
     *  initialized or not.
     */
    private volatile boolean m_initAttributes;

    /**
     * Delegated class for DataContainer.
     */
    private volatile DataContainer m_container;

    /**
     * Constructor.
     * @param filepath for this instance.
     * @exception ArchteaEntryException is thrown if some IO error happened.
     */
    public FileSystemLeafEntry(EntryManager mgr, String entryPath, String filepath) throws ArchteaEntryException {
        this(mgr, entryPath, new File(filepath));
    }

    public FileSystemLeafEntry(EntryManager mgr, String entryPath, File file) throws ArchteaEntryException {
        super(mgr, entryPath);
        m_initAttributes = false;
        try {
            m_path = file.getCanonicalPath();
            initAttributes(file);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ArchteaEntryException(e);
        }
    }

    /**
     * Write contents to a file.
     * @param in is a new contents.
     * @param overwrite is an flag to overwrite it.
     * @exception ArchteaEntryException is thrown if some exception happened.
     */
    public void writeContents(InputStream in, boolean overwrite) throws ArchteaEntryException {
        File f = new File(m_path);
        if ((!overwrite) && f.exists()) {
            throw new ArchteaEntryException("Cannot overwrite it because a file already exists.");
        } else if (!f.canWrite()) {
            throw new ArchteaEntryException("Cannot write a file.");
        } else if (in == null) {
            throw new ArchteaEntryException("Input stream is null.");
        }
        m_initAttributes = false;
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(f), getBuffSize());
            in = new BufferedInputStream(in, getBuffSize());
            byte[] buff = new byte[getBuffSize()];
            int len = in.read(buff);
            while (len != -1) {
                out.write(buff, 0, len);
                len = in.read(buff);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ArchteaEntryException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Get InputStream to read contents of a file.
     * @return InputStream instance of a file for this instance.
     * @exception IOException is thrown if some IO error happened.
     */
    public InputStream getInputStream() throws IOException {
        return getDataContainer().getInputStream();
    }

    /**
     * Get Reader.
     * @return Reader instance.
     * @exception IOException is thrown if some io error happened.
     */
    public Reader getReader(DataContainerReaderType type) throws IOException {
        return getDataContainer().getReader(type);
    }

    /**
     * Get content-type.
     * @return content-type if it found.
     */
    public String getContentType() {
        return getDataContainer().getContentType();
    }

    /**
     * Get charset if it can find.
     * @return charset if it can find, otherwise null.
     */
    public String getCharset() {
        return getDataContainer().getCharset();
    }

    /**
     * Overwrite this method to support some attributes.
     * @param name is an attribute name.
     * @return an attribute object.
     */
    public Object getAttribute(String name) {
        if (!m_initAttributes) {
            initAttributes(new File(m_path));
        }
        return super.getAttribute(name);
    }

    /**
     * Get buffer size.
     * @return buff size.
     */
    private int getBuffSize() {
        return DEFAULT_BUFF_SIZE;
    }

    /**
     * Get DataContainer instance.
     * @return DataContainer instance.
     */
    private DataContainer getDataContainer() {
        if (m_container == null) {
            synchronized (this) {
                if (m_container == null) {
                    m_container = getManager().getDataContainer(new FileDataContainerAdapter(m_path));
                }
            }
        }
        return m_container;
    }

    private void initAttributes(File f) {
        if (!m_initAttributes) {
            synchronized (this) {
                if (!m_initAttributes) {
                    super.setAttribute(ATTR_SIZE, "" + f.length());
                    super.setAttribute(ATTR_LAST_MODIFIED, "" + f.lastModified());
                    m_initAttributes = true;
                }
            }
        }
    }
}
