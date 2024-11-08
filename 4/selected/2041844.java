package net.sf.fileexchange.util.http;

import static net.sf.fileexchange.util.http.Method.GET;
import static net.sf.fileexchange.util.http.Method.HEAD;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.EnumSet;
import java.util.Set;

public class FileResource implements Resource {

    private final RandomAccessFile randomAccessFile;

    private final File file;

    private final long lastModified;

    private final long length;

    private final String id;

    private final String contentType;

    private static final Set<Method> GET_AND_HEAD = EnumSet.of(HEAD, GET);

    public FileResource(File file, String contentType) throws InterruptedException, FileChangedException, FileNotFoundException {
        this.file = file;
        this.contentType = contentType;
        this.lastModified = file.lastModified();
        Thread.sleep(1);
        if (lastModified != file.lastModified()) throw new FileChangedException();
        this.length = file.length();
        this.id = Long.toHexString(lastModified);
        this.randomAccessFile = new RandomAccessFile(file, "r");
    }

    public final class FileChangedException extends IOException {

        private static final long serialVersionUID = 1L;
    }

    @Override
    public String getID() {
        return id;
    }

    public long getLength() {
        return length;
    }

    @Override
    public void writeTo(OutputStream out, long offset, long length) throws IOException {
        randomAccessFile.seek(offset);
        byte[] buffer = new byte[32 * 1024];
        long remaining = length;
        while (remaining > 0) {
            int readed = randomAccessFile.read(buffer, 0, (int) Math.min(remaining, buffer.length));
            if (file.lastModified() != lastModified) throw new FileChangedException();
            if (readed == -1) {
                throw new IllegalArgumentException("randomAccessFile to short");
            }
            out.write(buffer, 0, readed);
            remaining -= readed;
        }
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Set<Method> getAllowedMethods() {
        return GET_AND_HEAD;
    }

    /**
	 * 
	 * @param file
	 *            to get the resource for.
	 * @return the requested resource or null if the file could not be found.
	 * @throws InterruptedException
	 *             if the thread gets interrupted before the method could
	 *             complete.
	 */
    public static Resource create(File file) throws InterruptedException {
        try {
            while (true) {
                try {
                    return new FileResource(file, null);
                } catch (FileChangedException e) {
                    Thread.sleep(100);
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}
