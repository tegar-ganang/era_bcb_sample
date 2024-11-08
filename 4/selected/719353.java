package x.java.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Virtual File System RandomAccessFile backed by a local file. Supports read
 * only or write only mode; mixed read/write not supported.
 * 
 * @author qiangli
 * 
 */
public class RandomAccessFile extends java.io.RandomAccessFile {

    private x.java.io.File file = null;

    private String mode = null;

    /**
	 * @param file
	 * @param mode
	 * @throws FileNotFoundException
	 */
    public RandomAccessFile(java.io.File file, String mode) throws FileNotFoundException {
        super(create(file, mode), mode);
        this.file = (x.java.io.File) file;
        this.mode = mode;
    }

    /**
	 * @param name
	 * @param mode
	 * @throws FileNotFoundException
	 */
    public RandomAccessFile(String name, String mode) throws FileNotFoundException {
        this(new x.java.io.File(name), mode);
    }

    public void close() throws IOException {
        super.close();
        if (mode.equals("r")) {
            return;
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        InputStream is = new java.io.FileInputStream(super.getFD());
        OutputStream os = file.getOutputStream();
        copy(is, os);
    }

    public java.io.File getFile() {
        return this.file;
    }

    public String getMode() {
        return this.mode;
    }

    private static java.io.File create(java.io.File f, String mode) throws FileNotFoundException {
        x.java.io.File file = (x.java.io.File) f;
        if (mode.equals("r")) {
            file.checkReadable();
        } else if (mode.indexOf("w") != -1) {
            file.checkWritable();
        }
        java.io.File cached = null;
        try {
            cached = java.io.File.createTempFile("vfs", ".cache");
            cached.deleteOnExit();
            if (mode.equals("r")) {
                InputStream is = file.getInputStream();
                java.io.FileOutputStream os = new java.io.FileOutputStream(cached);
                copy(is, os);
            }
        } catch (IOException e) {
            throw new FileNotFoundException("can't map to local cache: " + e);
        }
        return cached;
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        InputStream bis = null;
        OutputStream bos = null;
        try {
            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(os);
            byte[] buf = new byte[1024];
            int nread = -1;
            while ((nread = bis.read(buf)) != -1) {
                bos.write(buf, 0, nread);
            }
        } finally {
            bos.close();
            bis.close();
        }
        is.close();
        os.close();
    }
}
