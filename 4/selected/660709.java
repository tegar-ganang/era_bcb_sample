package net.sf.webwarp.util.openoffice.odt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Object containing information for one odt-Document. Package private.
 */
class OpenDocumentImpl implements OpenDocument, Cloneable {

    private static final Logger log = Logger.getLogger(OpenDocumentImpl.class);

    private static final int BUFFER_SIZE = 10 * 1024;

    private byte[] fileData;

    /** Has the document been modified relative to fileDate. */
    private boolean dirty;

    /** Content of content.xml */
    private String content;

    /** Content of styles.xml (including headers) */
    private String styles;

    /**
     * Create new OpenDocument from file data. Package private, use OpenDocumentUtil as factory.
     */
    OpenDocumentImpl(byte[] fileData) {
        if (fileData == null) {
            throw new NullPointerException("fileData");
        }
        if (fileData.length == 0) {
            throw new IllegalArgumentException("zero length fileData");
        }
        if (log.isTraceEnabled()) {
            log.trace("File date length = " + fileData.length);
        }
        this.fileData = fileData;
        this.dirty = false;
        this.content = null;
        this.styles = null;
    }

    public String getContent() {
        if (content == null) {
            parseFileData();
        }
        return content;
    }

    public void setContent(String content) {
        if (content == null) {
            throw new NullPointerException("content");
        }
        this.content = content;
        this.dirty = true;
    }

    public String getStyles() {
        if (styles == null) {
            parseFileData();
        }
        return styles;
    }

    public void setStyles(String styles) {
        if (styles == null) {
            throw new NullPointerException("styles");
        }
        this.styles = styles;
        this.dirty = true;
    }

    public byte[] getFileData() {
        if (this.dirty) {
            this.fileData = injectModifiedText();
            this.dirty = false;
        }
        return fileData;
    }

    private void parseFileData() {
        this.content = null;
        this.styles = null;
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData));
        try {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (log.isTraceEnabled()) {
                    log.trace("zipEntry name: " + zipEntry.getName());
                }
                if (zipEntry.getName().equals("content.xml")) {
                    this.content = IOUtils.toString(zipInputStream, "UTF-8");
                    if (log.isTraceEnabled()) {
                        log.trace("Read content.xml from fileData\n" + content);
                    } else if (log.isDebugEnabled()) {
                        log.trace("Length of content.xml = " + content.length());
                    }
                }
                if (zipEntry.getName().equals("styles.xml")) {
                    this.styles = IOUtils.toString(zipInputStream, "UTF-8");
                    if (log.isTraceEnabled()) {
                        log.trace("Read styles.xml from fileData\n" + styles);
                    } else if (log.isDebugEnabled()) {
                        log.trace("Length of styles.xml = " + styles.length());
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException ex) {
            log.error("IO-Exception while parsing odt-file form byte[].", ex);
            throw new IllegalArgumentException("Not a valid odt-file (" + ex + ")");
        } finally {
            IOUtils.closeQuietly(zipInputStream);
        }
        if (this.content == null) {
            log.error("No zipEntry named content.xml found, throw exception");
            throw new IllegalArgumentException("Not a valid odt-file (no content.xml found)");
        }
        if (this.styles == null) {
            log.error("No zipEntry named styles.xml found, throw exception");
            throw new IllegalArgumentException("Not a valid odt-file (no styles.xml found)");
        }
    }

    private byte[] injectModifiedText() {
        byte[] buffer = new byte[BUFFER_SIZE];
        boolean contentFound = false;
        boolean stylesFound = false;
        ByteArrayOutputStream out = new ByteArrayOutputStream(2 * fileData.length);
        ZipOutputStream zipOutputStream = new ZipOutputStream(out);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData));
        try {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
                if (content != null && zipEntry.getName().equals("content.xml")) {
                    contentFound = true;
                    if (log.isTraceEnabled()) {
                        log.trace("Write content.xml to fileData\n" + content);
                    } else if (log.isDebugEnabled()) {
                        log.trace("Write content.xml to fileData, length = " + content.length());
                    }
                    zipOutputStream.write(content.getBytes("UTF-8"));
                } else if (styles != null && zipEntry.getName().equals("styles.xml")) {
                    stylesFound = true;
                    if (log.isTraceEnabled()) {
                        log.trace("Write styles.xml to fileData\n" + styles);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Write styles.xml to fileData, length = " + styles.length());
                    }
                    zipOutputStream.write(styles.getBytes("UTF-8"));
                } else {
                    int read = zipInputStream.read(buffer);
                    while (read > -1) {
                        zipOutputStream.write(buffer, 0, read);
                        read = zipInputStream.read(buffer);
                    }
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException ex) {
            log.error("Exception while injecting content and styles into odt", ex);
            throw new IllegalArgumentException("fileData is probably not an odt file: " + ex);
        } finally {
            IOUtils.closeQuietly(zipInputStream);
            IOUtils.closeQuietly(zipOutputStream);
        }
        if (content != null && !contentFound) {
            log.error("fileData is not an odt file, no content.xml found, throwing exception.");
            throw new IllegalArgumentException("fileData is not an odt file, no content.xml found");
        }
        if (styles != null && !stylesFound) {
            log.error("fileData is not an odt file, no styles.xml found, throwing exception.");
            throw new IllegalArgumentException("fileData is not an odt file, no styles.xml found");
        }
        byte[] result = out.toByteArray();
        if (log.isDebugEnabled()) {
            log.debug("Injected content. File data changed from " + fileData.length + " bytes to " + result.length + " bytes.");
        }
        return result;
    }

    public OpenDocument cloneDocument() {
        try {
            return (OpenDocumentImpl) this.clone();
        } catch (CloneNotSupportedException e) {
            log.fatal("Unexpected " + e, e);
            throw new RuntimeException("Unexpected " + e, e);
        }
    }
}
