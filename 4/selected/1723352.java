package com.google.code.javastorage;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;

public abstract class AbstractStorageFile extends StorageFile {

    private static final long serialVersionUID = 1L;

    private long size;

    private boolean file = true;

    private Date modificationDate;

    protected Visibility visibility = Visibility.publicEntry;

    public AbstractStorageFile(String pathname) {
        super(pathname);
    }

    public AbstractStorageFile(StorageFile parent, String child) {
        super(parent, child);
    }

    /**
	 * Returns the path of the temporary file. The call triggers the download of
	 * the file into the temp directory.
	 * 
	 * This method gets called by FileInputStream(File). Users should use
	 * WualaFile.openStream() instead to prevent the caching in the temp
	 * directory.
	 * 
	 */
    @Override
    public String getPath() {
        InputStream in = null;
        OutputStream out = null;
        File file = null;
        try {
            file = File.createTempFile("java-storage_" + RandomStringUtils.randomAlphanumeric(32), ".tmp");
            file.deleteOnExit();
            out = new FileOutputStream(file);
            in = openStream();
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
        if (file != null && file.exists()) {
            return file.getPath();
        }
        return null;
    }

    @Override
    public String[] list(FilenameFilter filter) {
        String[] files = list();
        List<String> filteredFiles = new ArrayList<String>();
        for (String file : files) {
            if (filter.accept(this, file)) {
                filteredFiles.add(file);
            }
        }
        return filteredFiles.toArray(new String[0]);
    }

    @Override
    public StorageFile[] listFiles(FileFilter filter) {
        StorageFile[] files = listFiles();
        List<StorageFile> filteredFiles = new ArrayList<StorageFile>();
        for (StorageFile file : files) {
            if (filter.accept(file)) {
                filteredFiles.add(file);
            }
        }
        return filteredFiles.toArray(new StorageFile[0]);
    }

    @Override
    public StorageFile[] listFiles(FilenameFilter filter) {
        StorageFile[] files = listFiles();
        List<StorageFile> filteredFiles = new ArrayList<StorageFile>();
        for (StorageFile file : files) {
            if (filter.accept(this, file.getName())) {
                filteredFiles.add(file);
            }
        }
        return filteredFiles.toArray(new StorageFile[0]);
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    protected void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public boolean isDirectory() {
        return !isFile();
    }

    @Override
    public boolean isFile() {
        return file;
    }

    public void setFile(boolean isFile) {
        this.file = isFile;
    }
}
