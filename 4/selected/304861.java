package alto.sys;

import java.nio.channels.FileChannel;

/**
 * Class used by {@link File#openInputStream()}.  
 * 
 * <h3>File locking</h3>
 * 
 * <p> The file locking channel (and this input stream) must remain
 * open until the file lock is released.  To accomplish this, and for
 * rational server side file writing, the {@link File} class permits
 * only one open instance of this class at a time. </p>
 * 
 * @see File
 * @see Flock
 * @author jdp
 * @since 1.6
 */
public class FileInputStream extends java.io.FileInputStream implements java.nio.channels.ReadableByteChannel, alto.io.Input {

    public static final int COPY = 0x200;

    private final File file;

    private alto.io.u.Bbuf linebuf;

    public FileInputStream(File file) throws java.io.FileNotFoundException, java.lang.SecurityException {
        super(file);
        this.file = file;
    }

    public File getFile() {
        return this.file;
    }

    public boolean isOpen() {
        return this.getChannel().isOpen();
    }

    public int read(java.nio.ByteBuffer dst) throws java.io.IOException {
        return this.getChannel().read(dst);
    }

    private alto.io.u.Bbuf linebuf() {
        alto.io.u.Bbuf linebuf = this.linebuf;
        if (null == linebuf) {
            linebuf = new alto.io.u.Bbuf();
            this.linebuf = linebuf;
        }
        return linebuf;
    }

    public final java.lang.String readLine() throws java.io.IOException {
        int ch, drop = 0;
        alto.io.u.Bbuf linebuf = this.linebuf();
        readl: while (true) {
            switch(ch = this.read()) {
                case -1:
                case '\n':
                    break readl;
                case '\r':
                    switch(ch = this.read()) {
                        case -1:
                            break readl;
                        case '\n':
                            break readl;
                        default:
                            throw new java.io.IOException("Character carriage-return not followed by line-feed.");
                    }
                default:
                    linebuf.write(ch);
                    break;
            }
        }
        byte[] buf = linebuf.toByteArray();
        if (null == buf) return null; else {
            java.lang.String re = linebuf.toString();
            linebuf.resetall();
            char[] cary = alto.io.u.Utf8.decode(buf);
            return new java.lang.String(cary);
        }
    }

    public byte[] readMany(int length) throws java.io.IOException {
        byte[] re = new byte[length];
        int r = this.read(re, 0, length);
        if (1 > r) return null; else {
            int count = (length - r);
            int index = r;
            while (0 < count && (0 < (r = this.read(re, index, count)))) {
                if (1 > r) break; else {
                    count -= r;
                    index += r;
                }
            }
            if (r < length) {
                byte[] copier = new byte[r];
                System.arraycopy(re, 0, copier, 0, r);
                return copier;
            }
            return re;
        }
    }

    public int copyTo(alto.io.Output out) throws java.io.IOException {
        int total = 0;
        byte[] iobuf = new byte[COPY];
        int count;
        while (0 < (count = this.read(iobuf, 0, COPY))) {
            out.write(iobuf, 0, count);
            total += count;
        }
        return total;
    }
}
