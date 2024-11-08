package org.sac.browse.datastore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.sac.browse.util.Logger;

public class FileSystemPersister implements AppDataPersister {

    private static final Logger logger = new Logger("FileSystemPersister");

    private static final int MAX_CHUNK_BYTES = 5000000;

    private static final int MAX_BUFFER_BYTES = 1000000;

    @Deprecated
    public boolean deleteFile(TmpFile tmpFile) {
        return false;
    }

    public boolean deleteFile(String fileName, int part, int batchSize) {
        File file = new File(fileName + "_" + part);
        return file.delete();
    }

    @Deprecated
    public TmpFile getFile(String fileName) throws IOException {
        return null;
    }

    @Deprecated
    public void writeFile(InputStream in, String fileName, String fileKey) throws IOException {
        int maxlength = 1000000;
        File uploadedFile = new File(fileKey);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(uploadedFile, uploadedFile.exists());
            byte[] buffer = new byte[maxlength];
            int readBytes = in.read(buffer);
            if (readBytes < 0) {
                fos.write(new byte[0]);
            } else {
                while (readBytes > -1) {
                    fos.write(buffer, 0, readBytes);
                    buffer = new byte[maxlength];
                    readBytes = in.read(buffer);
                }
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        logger.logIt("Wrote " + uploadedFile.getAbsolutePath() + " to server");
    }

    public void saveFile(InputStream in, String name, String fileKey, ProgressListener listener, int batchNo, long maxBatchSize, boolean isFinalBatch) throws IOException {
        if (listener == null) {
            listener = new ProgressListener(null);
        }
        int index = 0;
        File uploadedFile = new File(fileKey + "_" + index);
        while (uploadedFile.length() >= MAX_CHUNK_BYTES) {
            uploadedFile = new File(fileKey + "_" + ++index);
        }
        listener.resetCurrentSize((index * MAX_CHUNK_BYTES) + uploadedFile.length());
        int current_size = (int) uploadedFile.length();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(uploadedFile, uploadedFile.exists());
            byte[] buffer;
            if (MAX_CHUNK_BYTES - current_size < MAX_BUFFER_BYTES) {
                buffer = new byte[MAX_CHUNK_BYTES - current_size];
            } else {
                buffer = new byte[MAX_BUFFER_BYTES];
            }
            int readBytes = in.read(buffer);
            while (readBytes > -1) {
                fos.write(buffer, 0, readBytes);
                current_size = current_size + readBytes;
                listener.updateStatus(readBytes);
                if (current_size == MAX_CHUNK_BYTES) {
                    uploadedFile = new File(fileKey + "_" + ++index);
                    fos = new FileOutputStream(uploadedFile, uploadedFile.exists());
                    buffer = new byte[MAX_BUFFER_BYTES];
                } else if (current_size + MAX_BUFFER_BYTES > MAX_CHUNK_BYTES) {
                    buffer = new byte[MAX_CHUNK_BYTES - current_size];
                } else {
                    buffer = new byte[MAX_BUFFER_BYTES];
                }
                readBytes = in.read(buffer);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        logger.logIt("Wrote " + uploadedFile.getAbsolutePath() + " to server");
    }

    public String isBatchFinal(String fileNameKey, int batchNo, int batchSize) {
        throw new RuntimeException("UNIMPLEMENTED METHOD");
    }

    public void fetchIntoStream(String fileNameKey, int part, int batchSize, OutputStream out) throws IOException {
        File doc = new File(fileNameKey + "_" + part);
        if (doc.exists() && doc.isFile()) {
            FileInputStream input = new FileInputStream(doc);
            BufferedInputStream buf = new BufferedInputStream(input);
            int readBytes = 0;
            while ((readBytes = buf.read()) != -1) {
                out.write(readBytes);
            }
            if (buf != null) {
                buf.close();
            }
        }
    }

    public String getFileName(String fileNameKey) {
        return fileNameKey.split("_")[0];
    }
}
