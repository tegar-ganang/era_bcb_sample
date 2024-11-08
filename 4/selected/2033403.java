package com.panopset.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileLock;
import com.panopset.RezIO;
import com.panopset.Strings;
import com.panopset.Util;
import com.panopset.UtilIO;

/**
 * Use of this class to write to a file will ensure that nobody else locks it
 * while you are writing.
 *
 * @author Karl Dinwiddie
 *
 */
public final class LockedWriter {

    /**
     * File.
     */
    private final File file;

    /**
     * Append to end of file flag.
     */
    private final boolean append;

    /**
     * Buffered writer.
     */
    private BufferedWriter writer;

    /**
     * File lock.
     */
    private FileLock lock;

    /**
     * LockedWriter constructor.
     *
     * @param outFile
     *            File to be written out to.
     * @param appendFile
     *            Set to true if you want to append text.
     */
    public LockedWriter(final File outFile, final boolean appendFile) {
        this.file = outFile;
        this.append = appendFile;
        init();
    }

    /**
     * LockedWriter will not append, if the file exists it will be overwritten.
     *
     * @param outFile
     *            File to be written out to.
     */
    public LockedWriter(final File outFile) {
        this(outFile, false);
    }

    /**
     * LockedWriter will not append, if the file exists it will be overwritten.
     *
     * @param path
     *            Full path of file you want to write out to.
     */
    public LockedWriter(final String path) {
        this(new File(path), false);
    }

    /**
     *
     * @return File associated with this LockedWriter
     */
    public File getFile() {
        return file;
    }

    /**
     * Delete the file associated with this LockedWriter.
     */
    public void deleteFile() {
        UtilIO.delete(this.file);
    }

    /**
     * Close the file, release any locks.
     */
    public void close() {
        if (this.lock != null) {
            try {
                this.lock.release();
            } catch (IOException e) {
                Util.log(e);
            }
        }
        try {
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a String, then append a standard line feed/carriage return.
     *
     * @param s
     *            String to write.
     */
    public void writeln(final String s) {
        try {
            this.writer.write(s);
            this.writer.write(Strings.getEol());
        } catch (IOException e) {
            Util.log(e);
        }
    }

    /**
     * Write a character.
     *
     * @param c
     *            Character to write.
     */
    public void write(final int c) {
        try {
            this.writer.write(c);
        } catch (IOException e) {
            Util.log(e);
        }
    }

    /**
     * Write a character buffer.
     *
     * @param cbuf
     *            Character buffer to write.
     */
    public void write(final char[] cbuf) {
        try {
            this.writer.write(cbuf);
        } catch (IOException e) {
            Util.log(e);
        }
    }

    /**
     * Write a character buffer.
     *
     * @param cbuf
     *            Character buffer to write.
     * @param off
     *            Offset.
     * @param len
     *            Length.
     */
    public void write(final char[] cbuf, final int off, final int len) {
        try {
            this.writer.write(cbuf, off, len);
        } catch (IOException e) {
            Util.log(e);
        }
    }

    /**
     * Write a String.
     *
     * @param str
     *            String to write.
     */
    public void write(final String str) {
        try {
            this.writer.write(str);
        } catch (IOException e) {
            Util.log(e);
        }
    }

    /**
     * Write a String.
     *
     * @param s
     *            String to write.
     * @param off
     *            Offset.
     * @param len
     *            Length.
     */
    public void write(final String s, final int off, final int len) {
        try {
            this.writer.write(s, off, len);
        } catch (IOException e) {
            Util.log(e);
        }
    }

    /**
     *
     * @return Writer associated with this LockedWriter
     */
    public BufferedWriter getWriter() {
        return this.writer;
    }

    /**
     * Set writer.
     *
     * @param newWriter
     *            Writer to set this locked writer to.
     */
    private void setWriter(final Writer newWriter) {
        if (newWriter instanceof BufferedWriter) {
            writer = (BufferedWriter) newWriter;
        } else {
            writer = new BufferedWriter(newWriter);
        }
    }

    /**
     * Perform all initialization.
     */
    private void init() {
        try {
            File d = this.file.getParentFile();
            UtilIO.mkdirs(d);
            FileOutputStream stream = new FileOutputStream(this.file, append);
            this.lock = stream.getChannel().tryLock();
            setWriter(new OutputStreamWriter(stream));
        } catch (IOException e) {
            throw new RuntimeException(RezIO.getCanonicalPath(this.file), e);
        }
    }
}
