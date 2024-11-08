package jaxlib.arc.tar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import jaxlib.arc.FileArchiveOutputStream;
import jaxlib.io.channel.OutputByteChannel;
import jaxlib.io.stream.IOStreams;
import jaxlib.util.CheckArg;

/**
 * An outputstream to write a <tt>GNU STar</tt> file archive.
 * Entries are always written in the <tt>GNU STar</tt> format. That's the format used by the Tar
 * implementation commonly used on Linux systems.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: TarOutputStream.java 2543 2008-02-21 13:03:25Z joerg_wassmer $
 */
public class TarOutputStream extends FileArchiveOutputStream<TarEntry> {

    /**
   * The currently opened entry.
   */
    private TarEntry entry;

    /**
   * Bytes remaining in currently opened entry.
   */
    private long remaining = 0;

    private byte[] buf;

    private byte[] padBuf;

    public TarOutputStream(final OutputStream out) {
        super(out);
    }

    private OutputStream checkWrite(final long count) throws IOException {
        if (count > this.remaining) {
            throw new TarException("Number of bytes to write(" + count + ") exceeds remaining bytes to be written for current entry(" + this.remaining + ")");
        }
        final OutputStream out = super.out;
        if (out != null) return out; else throw new ClosedChannelException();
    }

    private OutputStream ensureOpen() throws IOException {
        final OutputStream out = super.out;
        if (out != null) return out; else throw new ClosedChannelException();
    }

    private void written(final long count) throws IOException {
        final long remaining = this.remaining - count;
        this.remaining = remaining;
        if (remaining == 0) closeEntry(); else if (remaining < 0) throw new TarException("too many bytes written");
    }

    @Override
    public void closeInstance() throws IOException {
        if (super.out != null) {
            try {
                closeEntry();
                super.closeInstance();
            } finally {
                this.entry = null;
                this.buf = null;
                this.padBuf = null;
            }
        }
    }

    @Override
    public void closeEntry() throws IOException {
        final OutputStream out = ensureOpen();
        if (this.remaining != 0) {
            throw new TarException("Previous entry was not finished, " + this.remaining + " bytes required to be written.");
        }
        final TarEntry entry = this.entry;
        if (entry != null) {
            this.entry = null;
            final int pad = entry.computePadding();
            if (pad > 0) {
                if (this.padBuf == null) this.padBuf = new byte[TarHeader.RECORDSIZE];
                out.write(this.padBuf, 0, pad);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        ensureOpen().flush();
    }

    @Override
    public final boolean isOpen() {
        return super.out != null;
    }

    @Override
    public void openEntry(final TarEntry entry) throws IOException {
        CheckArg.notNull(entry, "entry");
        closeEntry();
        if (entry.longName != null) {
            openEntry(entry.longName);
            writeBytes0(entry.longName.header.longEntryString, (int) (this.remaining - entry.longName.header.longEntryString.length()));
            closeEntry();
        }
        if (entry.longLinkName != null) {
            openEntry(entry.longLinkName);
            writeBytes0(entry.longName.header.longEntryString, (int) (this.remaining - entry.longName.header.longEntryString.length()));
            closeEntry();
        }
        final TarHeader header = entry.getHeader();
        if (this.buf == null) this.buf = new byte[TarHeader.RECORDSIZE];
        header.write(this.buf);
        ensureOpen().write(this.buf, 0, TarHeader.RECORDSIZE);
        this.remaining = header.size;
        this.entry = entry;
        if (header.size == 0) closeEntry();
    }

    @Override
    public long transferFrom(final InputStream in, long maxCount) throws IOException {
        if (maxCount < 0) maxCount = this.remaining; else maxCount = Math.min(maxCount, this.remaining);
        final OutputStream out = checkWrite(maxCount);
        if (maxCount == 0) return 0;
        final long count = IOStreams.transfer(in, out, maxCount);
        if (count > 0) written(count);
        return count;
    }

    @Override
    public long transferFromByteChannel(final ReadableByteChannel in, long maxCount) throws IOException {
        if (maxCount < 0) maxCount = this.remaining; else maxCount = Math.min(maxCount, this.remaining);
        final OutputStream out = checkWrite(maxCount);
        if (maxCount == 0) return 0;
        final long count;
        if (out instanceof OutputByteChannel) {
            count = ((OutputByteChannel) out).transferFromByteChannel(in, maxCount);
            written(count);
        } else {
            count = super.transferFromByteChannel(in, maxCount);
        }
        return count;
    }

    @Override
    public final void write(final int b) throws IOException {
        checkWrite(1).write(b);
        written(1);
    }

    @Override
    public final void write(final byte[] buf) throws IOException {
        final int len = buf.length;
        checkWrite(len).write(buf, 0, len);
        written(len);
    }

    @Override
    public final void write(final byte[] buf, final int offs, final int len) throws IOException {
        checkWrite(len).write(buf, offs, len);
        written(len);
    }

    @Override
    public final int write(final ByteBuffer src) throws IOException {
        final int count = src.remaining();
        final OutputStream out = checkWrite(count);
        IOStreams.write(out, src);
        written(count);
        return count;
    }

    @Override
    public final void writeBytes(final String s) throws IOException {
        writeBytes0(s, 0);
    }

    private void writeBytes0(final String s, final int pad) throws IOException {
        final int len = s.length() + pad;
        final OutputStream out = checkWrite(len);
        byte[] buf = this.buf;
        if (buf.length < len) {
            buf = null;
            this.buf = null;
            this.buf = buf = new byte[len];
        }
        for (int i = s.length(); --i >= 0; ) buf[i] = (byte) s.charAt(i);
        if (pad != 0) buf[len - 1] = 0;
        out.write(buf, 0, len);
        written(len);
    }
}
