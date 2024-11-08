package de.inovox.pipeline.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.MimetypesFileTypeMap;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component that holds binary content
 * 
 * @author Carsten Burghardt
 * @version $Id: Content.java 408 2008-05-08 19:49:00Z carsten $
 */
public class Content extends Component {

    /**
     * serial id
     */
    private static final long serialVersionUID = -8437583433982464134L;

    private static Log log = LogFactory.getLog(Content.class);

    public static final String TYPE = "content";

    /** Mimetype map from Activation */
    private static MimetypesFileTypeMap mimeTypes = null;

    private static final String DEFAULT_TYPE = "content";

    /** Content as byte[] (only used for writing data) */
    protected transient byte[] content = null;

    /** Content as stream which is read on demand */
    protected transient InputStream contentStream;

    /** Content type aka mimetype */
    protected String contentType = null;

    /** Delete the reference if done */
    protected boolean deletePathReference = true;

    /**
     * Default constructor 
     */
    public Content() {
        componentType = DEFAULT_TYPE;
    }

    /**
     * Write the content to a file
     * @param file
     */
    public void dumpToFile(File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        if (contentStream != null) {
            IOUtils.copy(contentStream, out);
            setPath(file.getAbsolutePath());
        } else {
            IOUtils.write(getContent(), out);
        }
        IOUtils.closeQuietly(out);
    }

    /**
     * Retrieve the document content
     * The content is loaded if necessary
     * @return content as byte[] or null if no content is available
     */
    public byte[] getContent() {
        if (content != null) {
            return content;
        }
        if (contentStream != null) {
            try {
                content = IOUtils.toByteArray(contentStream);
                IOUtils.closeQuietly(contentStream);
                return content;
            } catch (IOException e) {
                log.error("Failed to read from stream", e);
            }
        }
        if (getPath() != null) {
            try {
                File file = new File(getPath());
                content = FileUtils.readFileToByteArray(file);
                return content;
            } catch (IOException e) {
                log.error("Failed to read from file", e);
            }
        }
        return null;
    }

    /**
     * Get content as OutputStream
     * Make sure that you close the stream when you're done
     * @return
     * @throws IOException
     */
    public OutputStream getAsOutputStream() throws IOException {
        OutputStream out;
        if (contentStream != null) {
            File tmp = File.createTempFile(getId(), null);
            out = new FileOutputStream(tmp);
            IOUtils.copy(contentStream, out);
        } else {
            out = new ByteArrayOutputStream();
            out.write(getContent());
        }
        return out;
    }

    /**
     * 
     */
    public InputStream getAsInputStream() throws IOException {
        if (contentStream != null) {
            return contentStream;
        } else {
            InputStream in = new ByteArrayInputStream(getContent());
            return in;
        }
    }

    /**
     * @param content the content to set
     */
    public void setContent(byte[] content) {
        this.content = content;
        setComplete(true);
    }

    /**
     * Get the content type (mimetype)
     * This is calculated automatically if not set
     * @return the contentType
     */
    public String getContentType() {
        if (contentType == null) {
            initContentType();
        }
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Set the contentType from the file extension
     * The filename is retrieved in the following order:
     * path -> name and then the same for the parent
     */
    private void initContentType() {
        if (mimeTypes == null) {
            mimeTypes = new MimetypesFileTypeMap();
        }
        String filename = getPath();
        if (filename == null) {
            filename = getName();
        }
        if (filename == null && getParent() != null) {
            filename = getParent().getPath();
            if (filename == null) {
                filename = getParent().getName();
            }
        }
        if (filename != null) {
            File file = new File(filename);
            String type = null;
            if (file != null && file.canRead()) {
                try {
                    MagicMatch match = Magic.getMagicMatch(file, true);
                    type = match.getMimeType();
                    log.debug("ContentType from magic:" + type);
                } catch (Exception e) {
                    log.debug("Could not get mimetype", e);
                }
            }
            if (type == null) {
                type = mimeTypes.getContentType(filename);
                log.debug("ContentType from map:" + type);
            }
            setContentType(type);
        }
    }

    /**
     * Set content from stream
     * This stream is read on demand so do not close it!
     * @param input
     */
    public void setContent(InputStream input) {
        this.contentStream = input;
        setComplete(true);
    }

    /**
     * @return the deleteFileReference
     */
    public boolean isDeletePathReference() {
        return deletePathReference;
    }

    /**
     * @param deleteFileReference the deleteFileReference to set
     */
    public void setDeletePathReference(boolean cleanupFileReference) {
        this.deletePathReference = cleanupFileReference;
    }

    @Override
    public void setPath(String path) {
        super.setPath(path);
        setComplete(true);
    }

    /**
     * Free content
     * @see de.inovox.pipeline.document.ImportObject#close()
     */
    @Override
    public void close() {
        content = null;
        if (contentStream != null) {
            IOUtils.closeQuietly(contentStream);
        }
    }

    @Override
    public String getFileName() {
        if (getName() != null && getName().length() > 0) {
            return super.getFileName();
        } else {
            StringBuffer name = new StringBuffer();
            name.append(getId());
            if (StringUtils.isNotEmpty(getPath())) {
                if (getPath().indexOf(".") != -1) {
                    name.append(getPath().substring(getPath().lastIndexOf(".")));
                }
            }
            return name.toString();
        }
    }

    public String getType() {
        return TYPE;
    }
}
