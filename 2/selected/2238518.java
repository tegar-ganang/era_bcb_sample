package au.gov.naa.digipres.xena.kernel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.xml.sax.InputSource;
import au.gov.naa.digipres.xena.kernel.type.Type;

/**
 * The standard XMLReader takes an InputSource as input. We enhance that to provide
 * some extra data, but we still retain compatibility with the XMLReader/InputSource
 * so that people can plug in standard XMLReaders.
 *
 * It's really hard to get the class hierarchy right for all the classes that
 * inherit from XenaInputSource. In theory, XenaInputSource is too specific and
 * has too much stuff dedicated to input sources that derive from Files, when
 * this should go in a sub-class to make sense with the existance of things
 * like ByteArrayInputSource.
 *
 * But in practice, most cases require regular Files, and it has been hard to
 * separate this out. At some time another effort should be made to do so however.
 *
 */
public class XenaInputSource extends InputSource {

    private Type type;

    private String mimeType = "";

    protected File file;

    private String unencodedRelativeFileName = "";

    private XenaInputSource parent;

    private boolean isTmpFile;

    private Date lastModified;

    private String outputFileName;

    protected List<InputStream> openedFiles = new ArrayList<InputStream>();

    /**
	 * Constructor. Create a Xena Input source with the systemId and type if already known.
	 * 
	 * @param systemId - the system id for this XenaInputSource
	 * @param type - the type of this input source.
	 */
    public XenaInputSource(String systemId, Type type) {
        super(systemId);
        this.type = type;
    }

    /**
	 * Constructor.
	 * Create a XenaInputSource using the specified file as the source, and set
	 * it's type to be the supplied type.
	 * 
	 * @param file
	 * @param type
	 * @throws FileNotFoundException
	 */
    public XenaInputSource(File file, Type type) throws FileNotFoundException {
        this(file.toURI().toASCIIString(), type);
        this.file = file;
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString() + " not  found");
        }
        lastModified = new Date(file.lastModified());
    }

    /**
	 * Constructor.
	 * Create a XenaInputSource using the specified file as the source.
	 * 
	 * @param file
	 * @throws FileNotFoundException
	 */
    public XenaInputSource(File file) throws FileNotFoundException {
        this(file.toURI().toASCIIString(), null);
        this.file = file;
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString() + " not  found");
        }
        lastModified = new Date(file.lastModified());
    }

    /**
	 * Constructor.
	 * Create a XenaInputSource using the specified InputStream as the source.
	 * @param is
	 */
    public XenaInputSource(InputStream is) {
        super(is);
    }

    /**
	 * <p>Set the parent of this object.</p>
	 * 
	 * <p>This handles the case of a record consisting of multiple input sources (usually files), one will
	 * be the 'parent' input source, and the others will be it's children.</p>
	 *  
	 * @see getParent  
	 * @param parent
	 */
    public void setParent(XenaInputSource parent) {
        this.parent = parent;
    }

    /**
	 * Get the parent XenaInputSource if it exists.
	 * 
	 * @see setParent()
	 * @return - The XenaInputSource which is the 'parent' of this one.
	 */
    public XenaInputSource getParent() {
        return parent;
    }

    /**
	 * Get the ultimate parent object of this Input Source.
	 * This method is deprecated. Use getParent and interrogate that if required.
	 * @return
	 * @deprecated
	 */
    @Deprecated
    public XenaInputSource getUltimateParent() {
        XenaInputSource rtn = null;
        if (parent == null) {
            rtn = this;
        } else {
            rtn = parent.getUltimateParent();
        }
        assert rtn != null;
        return rtn;
    }

    /**
	 * This method is deprecated. Use getFile and interrogate that if required.
	 * @return
	 * @deprecated
	 * @return
	 */
    @Deprecated
    public File getUltimateFile() {
        File rtn = null;
        if (file != null) {
            rtn = file;
        } else if (parent == null) {
            return null;
        } else {
            rtn = parent.getUltimateFile();
        }
        return rtn;
    }

    /**
	 * Delete the file that this XenaInputSource points to.
	 */
    public void delete() {
        if (file != null) {
            file.delete();
        }
    }

    /**
	 * Get a handle to the file that the Xena input source points to.
	 * @return
	 */
    public File getFile() {
        return file;
    }

    /**
	 * Return the (lower-case) file extension of the file name
	 * @return file extension
	 */
    public String getFileNameExtension() {
        String extension = null;
        if (file != null) {
            String fileName = file.getName();
            if (fileName.indexOf(".") != -1) {
                extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            }
        }
        return extension;
    }

    /**
	 * This the type of the Xena Input Source. This is <b>NOT</b> automatically set.
	 * Normalisers should not rely on this being initialised. It is up to the
	 * application to set this when required. Usually, this should be done during guessing,
	 * however it must be done <b>EXPLICITLY</b>.
	 * @return
	 */
    public Type getType() {
        return type;
    }

    /**
	 * This the type of the Xena Input Source. This is <b>NOT</b> automatically set.
	 * Normalisers should not rely on this being initialised. It is up to the
	 * application to set this when required. Usually, this should be done during guessing,
	 * however it must be done <b>EXPLICITLY</b>.
	 * @return
	 */
    public void setType(Type type) {
        this.type = type;
        mimeType = type.getMimeType();
    }

    @Override
    public Reader getCharacterStream() {
        if (getEncoding() == null) {
            return new InputStreamReader(getByteStream());
        }
        try {
            return new InputStreamReader(getByteStream(), getEncoding());
        } catch (UnsupportedEncodingException x) {
            x.printStackTrace();
            return null;
        }
    }

    @Override
    public InputStream getByteStream() {
        try {
            URL url = new URL(getSystemId());
            URLConnection conn = url.openConnection();
            InputStream rtn = null;
            InputStream is = null;
            try {
                is = conn.getInputStream();
            } catch (IOException x) {
                System.gc();
                is = conn.getInputStream();
            }
            rtn = new BufferedInputStream(is);
            openedFiles.add(rtn);
            return rtn;
        } catch (MalformedURLException x) {
            throw new RuntimeException(x);
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
	 * Close the input stream for this XenaInputSource if is open.
	 * @throws IOException
	 */
    public void close() throws IOException {
        Iterator it = openedFiles.iterator();
        while (it.hasNext()) {
            InputStream is = (InputStream) it.next();
            is.close();
        }
        openedFiles.clear();
        if (isTmpFile && file != null) {
            file.delete();
        }
    }

    public void setTmpFile(boolean v) {
        isTmpFile = v;
        if (file != null) {
            file.deleteOnExit();
        }
    }

    @Override
    public String toString() {
        return "System id: " + getSystemId() + " and type(?): " + type + " and mime type: " + mimeType;
    }

    /**
	 * @return Returns the unencodedRelativeFileName.
	 */
    public String getUnencodedRelativeFileName() {
        return unencodedRelativeFileName;
    }

    /**
	 * @return Returns the lastModified.
	 */
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XenaInputSource) {
            XenaInputSource xis = (XenaInputSource) obj;
            return getSystemId().equals(xis.getSystemId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getSystemId().hashCode();
    }

    /**
	 * @return Returns the outputFileName.
	 */
    public String getOutputFileName() {
        return outputFileName;
    }

    /**
	 * @param outputFileName The new value to set outputFileName to.
	 */
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
}
