package com.incendiaryblue.file;

import java.io.*;
import com.oreilly.servlet.multipart.*;

/**
 * This class represents a file that has been uploaded using the
 * FileUploadManager class.
 *
 */
public class UploadedFile {

    /** Buffer size to use for writing files. */
    public static final int BUFFER_SIZE = 8192;

    public static final int STATE_OK = 0;

    public static final int STATE_BAD_EXTENSION = 1;

    public static final int STATE_EMPTY_FILE = 2;

    private int state = STATE_OK;

    private File tempLocation = null;

    private String filename;

    private String contentType;

    UploadedFile(FilePart filePart) {
        filename = filePart.getFileName();
        contentType = filePart.getContentType();
    }

    void setFile(File f) {
        tempLocation = f;
    }

    void setState(int s) {
        state = s;
    }

    public int getState() {
        return state;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    /**
	 * Write the uploaded file to a permananent location.
	 *
	 * <p>If the upload failed, this throws an IllegalStateException.</p>
	 *
	 * <p>This method only works once, after which it deletes the temporary upload file.
	 * If it is called again, it throws an IllegalStateException.</p>
	 *
	 * @param f The file to write to.
	 */
    public void writeTo(File f) throws IOException {
        if (state != STATE_OK) throw new IllegalStateException("Upload failed");
        if (tempLocation == null) throw new IllegalStateException("File already saved");
        if (f.isDirectory()) f = new File(f, filename);
        FileInputStream fis = new FileInputStream(tempLocation);
        FileOutputStream fos = new FileOutputStream(f);
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            int i = 0;
            while ((i = fis.read(buf)) != -1) fos.write(buf, 0, i);
        } finally {
            deleteTemporaryFile();
            fis.close();
            fos.close();
        }
    }

    /**
	 * Gets the temporary location the file was uploaded to.
	 *
	 * <p>This should only be used if the file is not needed beyond the
	 * lifetime of the request.</p>
	 */
    public File getTemporaryFile() {
        if (state != STATE_OK) throw new IllegalStateException("Upload failed");
        if (tempLocation == null) throw new IllegalStateException("File already saved");
        return tempLocation;
    }

    /**
	 * This method, when called by the garbage collector, deletes this object's
	 * backing file from the temporary directory.
	 */
    public void finalize() {
        deleteTemporaryFile();
    }

    /**
	 * Delete the temporary file used for the upload, if it exists.
	 */
    public void deleteTemporaryFile() {
        if (tempLocation != null) {
            tempLocation.delete();
            tempLocation = null;
        }
    }
}
