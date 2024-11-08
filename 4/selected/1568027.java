package org.imogene.sync.server.custom;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;

public class OptimizedFileItem implements FileItem {

    private static final long serialVersionUID = -122174345162303746L;

    private File medooFile;

    private OutputStream os;

    private String fieldName;

    private String contentType;

    private boolean isFormField;

    private String fileName;

    private int sizeThreshold;

    private File repository;

    public OptimizedFileItem(String fieldName, String contentType, boolean isFormField, String fileName, int sizeThreshold, File repository) {
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.sizeThreshold = sizeThreshold;
        this.repository = repository;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(medooFile);
    }

    public boolean isInMemory() {
        return false;
    }

    public long getSize() {
        return medooFile.length();
    }

    public byte[] get() {
        byte[] fileData = new byte[(int) getSize()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(medooFile);
            fis.read(fileData);
        } catch (IOException e) {
            fileData = null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
        return fileData;
    }

    public void write(File file) throws Exception {
        if (medooFile != null) {
            if (!medooFile.renameTo(file)) {
                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                try {
                    in = new BufferedInputStream(new FileInputStream(medooFile));
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

    /**
     * When we resume synchro we never delete the file here.
     */
    public void delete() {
    }

    public OutputStream getOutputStream() throws IOException {
        if (os == null) {
            if (repository == null) throw new RuntimeException("repository is null !!!!!!");
            if (fileName == null) throw new RuntimeException("filename is null !!!!!!");
            medooFile = new File(repository, fileName);
            if (medooFile.exists()) return os = new FileOutputStream(medooFile, true);
            return new FileOutputStream(medooFile);
        }
        return os;
    }

    public File getStoreLocation() {
        return medooFile;
    }

    protected void finalize() {
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isFormField() {
        return isFormField;
    }

    public void setFormField(boolean isFormField) {
        this.isFormField = isFormField;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getSizeThreshold() {
        return sizeThreshold;
    }

    public void setSizeThreshold(int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public File getRepository() {
        return repository;
    }

    public void setRepository(File repository) {
        this.repository = repository;
    }

    public String getString(String arg0) throws UnsupportedEncodingException {
        return new String(get(), arg0);
    }

    public String getName() {
        return this.fileName;
    }

    public String getString() {
        return new String(get());
    }
}
