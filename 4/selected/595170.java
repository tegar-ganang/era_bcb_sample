package org.lindenb.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.lindenb.util.TimeUtils;

/**
 * @author Pierre Lindenbaum
 *
 */
public class TmpWriter extends Writer {

    private File tmpFile;

    private PrintWriter out = null;

    private long length = 0L;

    public TmpWriter(File directory) throws IOException {
        this.tmpFile = File.createTempFile("_tmp" + TimeUtils.toYYYYMMDD(), ".data", directory);
        this.out = new PrintWriter(new FileWriter(this.tmpFile));
    }

    public TmpWriter() throws IOException {
        this(new File(System.getProperty("java.io.tmpdir", "/tmp")));
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            delete();
        } catch (Exception e) {
        }
        super.finalize();
    }

    @Override
    public void flush() throws IOException {
        if (!isClosed()) out.flush();
    }

    public void close() throws IOException {
        flush();
        if (!isClosed()) out.close();
    }

    public void println(String s) {
        print(s);
        println();
    }

    public void print(String s) {
        if (s == null) s = "null";
        try {
            write(s.toCharArray());
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    public void println() {
        print("\n");
    }

    public void print(int s) {
        print(String.valueOf(s));
    }

    public void println(int s) {
        print(s);
        println();
    }

    public void print(long s) {
        print(String.valueOf(s));
    }

    public void println(long s) {
        print(s);
        println();
    }

    public void print(double s) {
        print(String.valueOf(s));
    }

    public void println(double s) {
        print(s);
        println();
    }

    public void print(float s) {
        print(String.valueOf(s));
    }

    public void println(float s) {
        print(s);
        println();
    }

    public void print(boolean s) {
        print(String.valueOf(s));
    }

    public void println(boolean s) {
        print(s);
        println();
    }

    public void print(Object s) {
        print(String.valueOf(s));
    }

    public void println(Object s) {
        print(s);
        println();
    }

    public void printStackTrace(Throwable err) {
        err.printStackTrace(this.out);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (isClosed()) throw new IOException("stream was closed");
        this.out.write(cbuf, off, len);
        this.length += len;
    }

    public boolean isClosed() {
        return this.out == null;
    }

    @Override
    public void write(char b[]) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(String s) throws IOException {
        write(s.toCharArray());
    }

    public long getSize() {
        return this.length;
    }

    public boolean isEmpty() {
        return getSize() == 0L;
    }

    public void copyToZip(ZipOutputStream zout, String entryName) throws IOException {
        close();
        ZipEntry entry = new ZipEntry(entryName);
        zout.putNextEntry(entry);
        if (!isEmpty() && this.tmpFile.exists()) {
            InputStream in = new FileInputStream(this.tmpFile);
            IOUtils.copyTo(in, zout);
            in.close();
        }
        zout.flush();
        zout.closeEntry();
        delete();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    public void delete() {
        IOUtils.safeClose(out);
        out = null;
        if (this.tmpFile.exists()) this.tmpFile.delete();
    }

    @Override
    public int hashCode() {
        return this.tmpFile == null ? -1 : this.tmpFile.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ":" + this.tmpFile;
    }
}
