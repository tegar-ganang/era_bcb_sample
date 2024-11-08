package net.sourceforge.gedapi.fileupload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemHeadersSupport;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.io.IOUtils;

public class GEDCOMFileItem implements FileItem, FileItemHeadersSupport {

    private static final long serialVersionUID = 2237570099615271008L;

    private static final Logger LOG = Logger.getLogger(GEDCOMFileItem.class.getName());

    public static final String DEFAULT_CHARSET = "ISO-8859-1";

    protected OutputStream fos;

    protected File gedcomFile;

    protected File repository;

    protected String fileName;

    protected FileItemHeaders headers;

    protected String fieldName;

    protected String contentType;

    protected boolean isFormField;

    protected long size = -1;

    protected String authenticated;

    public GEDCOMFileItem(String fieldName, String contentType, boolean isFormField, String fileName, File repository, String authenticated) {
        this.repository = repository;
        this.fileName = fileName.replace('\\', '/');
        this.fileName = this.fileName.substring(this.fileName.lastIndexOf('/') + 1);
        this.isFormField = isFormField;
        this.contentType = contentType;
        this.fieldName = fieldName;
        this.authenticated = authenticated;
    }

    public FileItemHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(FileItemHeaders pHeaders) {
        headers = pHeaders;
    }

    public void delete() {
        getGEDCOMFile().delete();
    }

    public byte[] get() {
        byte[] fileData = new byte[(int) getSize()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(getGEDCOMFile());
            fis.read(fileData);
        } catch (IOException e) {
            e.printStackTrace();
            fileData = null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileData;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(getGEDCOMFile()));
    }

    public String getName() {
        return getGEDCOMFile().getName();
    }

    public String getFullPath() {
        return getGEDCOMFile().getAbsolutePath();
    }

    public long getSize() {
        size = getGEDCOMFile().length();
        return size;
    }

    public String getCharSet() {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        Map params = parser.parse(getContentType(), ';');
        return (String) params.get("charset");
    }

    public String getString() {
        byte[] rawdata = get();
        String charset = getCharSet();
        if (charset == null) {
            charset = DEFAULT_CHARSET;
        }
        try {
            return new String(rawdata, charset);
        } catch (UnsupportedEncodingException e) {
            return new String(rawdata);
        }
    }

    public String getString(String charset) throws UnsupportedEncodingException {
        return new String(get(), charset);
    }

    public boolean isFormField() {
        return isFormField;
    }

    public boolean isInMemory() {
        return false;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setFormField(boolean isFormField) {
        this.isFormField = isFormField;
    }

    public void write(File file) throws Exception {
        if (getGEDCOMFile() != null) {
            size = getGEDCOMFile().length();
            if (!getGEDCOMFile().renameTo(file)) {
                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(getGEDCOMFile()));
                    out = new BufferedOutputStream(new FileOutputStream(file));
                    IOUtils.copy(in, out);
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
        } else {
            throw new FileUploadException("Cannot write uploaded file to disk!");
        }
    }

    protected File getGEDCOMFile() {
        if (gedcomFile == null) {
            if (repository == null) {
                throw new IllegalArgumentException("The repository class member is null inside the GEDCOMFileItem class, this is not allowed!!!");
            }
            fileName = authenticated + File.separatorChar + fileName;
            gedcomFile = getGEDCOMFile(repository, fileName);
        }
        return gedcomFile;
    }

    public static final File getGEDCOMFile(File repository, String fileName) {
        String gedcomFilePath = "uploads" + File.separatorChar + fileName;
        File gedcomFile = new File(repository, gedcomFilePath);
        LOG.finest("Mapped the relative path '" + fileName + "' to: " + gedcomFile);
        gedcomFile.getParentFile().mkdirs();
        return gedcomFile;
    }

    public OutputStream getOutputStream() throws IOException {
        if (fos == null) {
            File outputFile = getGEDCOMFile();
            fos = new BufferedOutputStream(new FileOutputStream(outputFile));
        }
        return fos;
    }
}
