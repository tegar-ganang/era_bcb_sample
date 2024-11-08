package org.middleheaven.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import org.middleheaven.io.repository.ManagedFile;
import org.middleheaven.io.repository.ManagedFilePath;
import org.middleheaven.io.repository.ManagedFileRepository;

public final class IOUtils {

    private IOUtils() {
    }

    public static boolean deleteTree(URI path) {
        return deleteTree(new File(path));
    }

    private static boolean deleteTree(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                if (!deleteTree(f)) {
                    return false;
                }
            } else {
                if (!f.delete()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * Closes an I/O resource. Possible IOException is encapsulated in 
	 * an ManagedIOException
	 * @param closeable
	 * @throws ManagedIOException if an IOEXception is throwned by the closeable
	 */
    public static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            throw ManagedIOException.manage(e);
        }
    }

    private static void doStreamCopy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while (-1 != (n = in.read(buffer))) {
            out.write(buffer, 0, n);
        }
        out.flush();
        out.close();
        in.close();
    }

    private static void doCopyFile(FileInputStream in, FileOutputStream out) {
        FileChannel inChannel = null, outChannel = null;
        try {
            inChannel = in.getChannel();
            outChannel = out.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw ManagedIOException.manage(e);
        } finally {
            if (inChannel != null) {
                close(inChannel);
            }
            if (outChannel != null) {
                close(outChannel);
            }
        }
    }

    /**
	 * The copy does not closes the steams.
	 * @param data - the data to write to the output stream
	 * @param out out the stream to write to
	 * 
	 * @see IOUtils#close(Closeable)
	 */
    public static void copy(byte[] data, OutputStream out) {
        if (out == null) {
            throw new IllegalArgumentException("Cannot copy to a non existent stream");
        }
        try {
            out.write(data);
        } catch (IOException e) {
            ManagedIOException.manage(e);
        }
    }

    /**
	 * The copy does not closes the steams.
	 * @param in the stream to read from
	 * @param out the stream to write to
	 * @throws IOException if something goes wrong
	 * @throws IllegalArgumentException if any argument is <code>null</code>
	 */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) {
            throw new IllegalArgumentException("Cannot copy a non existent stream");
        }
        if (in instanceof FileInputStream && out instanceof FileOutputStream) {
            doCopyFile((FileInputStream) in, (FileOutputStream) out);
        } else {
            doStreamCopy(in, out);
        }
    }

    /**
	 * Moves a file, i.e. copies the content to another file and deletes the original
	 * @param in
	 * @param out
	 * @throws IOException
	 */
    public static void move(File in, File out) {
        if (!in.isFile()) {
            throw new IllegalArgumentException("Can only move a file");
        }
        if (out.isDirectory()) {
            out = new File(out.getAbsolutePath() + "/" + in.getName());
            out.mkdirs();
        }
        copy(in, out);
        in.delete();
    }

    /**
	 * Copies the data in one file to another existing file. 
	 * @param in the file to read 
     * @param out the file to write
	 */
    public static void copy(File in, File out) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(in);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundManagedException(in);
        }
        try {
            fos = new FileOutputStream(out);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundManagedException(out);
        }
        doCopyFile(fis, fos);
    }
}
