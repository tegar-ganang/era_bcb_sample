package org.apache.sanselan.common.byteSources;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import org.apache.sanselan.util.Debug;

public class ByteSourceFile extends ByteSource {

    private final File file;

    public ByteSourceFile(File file) {
        super(file.getName());
        this.file = file;
    }

    public InputStream getInputStream() throws IOException {
        FileInputStream is = null;
        BufferedInputStream bis = null;
        is = new FileInputStream(file);
        bis = new BufferedInputStream(is);
        return bis;
    }

    public byte[] getBlock(int start, int length) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            if ((start < 0) || (length < 0) || (start + length < 0) || (start + length > raf.length())) {
                throw new IOException("Could not read block (block start: " + start + ", block length: " + length + ", data length: " + raf.length() + ").");
            }
            return getRAFBytes(raf, start, length, "Could not read value from file");
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (Exception e) {
                Debug.debug(e);
            }
        }
    }

    public long getLength() {
        return file.length();
    }

    public byte[] getAll() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            is = new BufferedInputStream(is);
            byte buffer[] = new byte[1024];
            int read;
            while ((read = is.read(buffer)) > 0) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        } finally {
            try {
                if (null != is) is.close();
            } catch (IOException e) {
            }
        }
    }

    public String getDescription() {
        return "File: '" + file.getAbsolutePath() + "'";
    }
}
