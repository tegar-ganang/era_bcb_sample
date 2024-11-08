package com.freegle;

import java.io.*;
import java.io.IOException;
import java.util.*;
import com.lucene.store.*;

public class ResourceDirectory extends Directory {

    ClassLoader classLoader;

    String base;

    String[] fileList;

    public ResourceDirectory(ClassLoader classLoader, String base) {
        this.classLoader = classLoader;
        this.base = base;
    }

    /** Returns an array of strings, one for each file in the directory. */
    public final String[] list() throws IOException {
        loadList();
        return fileList;
    }

    /** Returns true iff a file with the given name exists. */
    public final boolean fileExists(String name) throws IOException {
        loadList();
        for (int i = 0; i < fileList.length; i++) if (fileList[i].equals(name)) return true;
        return false;
    }

    /** Returns the time the named file was last modified. */
    public final long fileModified(String name) throws IOException {
        throw new RuntimeException("Unsupported operation");
    }

    /** Returns the length in bytes of a file in the directory. */
    public final long fileLength(String name) throws IOException {
        throw new RuntimeException("Unsupported operation");
    }

    /** Removes an existing file in the directory. */
    public final void deleteFile(String name) throws IOException {
        throw new RuntimeException("Unsupported operation");
    }

    /** Renames an existing file in the directory. */
    public final void renameFile(String from, String to) throws IOException {
        throw new RuntimeException("Unsupported operation");
    }

    /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
    public final com.lucene.store.OutputStream createFile(String name) throws IOException {
        throw new RuntimeException("Unsupported operation");
    }

    /** Returns a stream reading an existing file. */
    public final com.lucene.store.InputStream openFile(String name) throws IOException {
        if (!fileExists(name)) throw new FileNotFoundException(base + name);
        java.io.InputStream raw = classLoader.getResourceAsStream(base + name + ".t");
        byte[] buf = new byte[128];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int n;
        while ((n = raw.read(buf)) > 0) baos.write(buf, 0, n);
        final byte[] data = baos.toByteArray();
        class ResourceInputStream extends com.lucene.store.InputStream {

            int pos;

            ResourceInputStream() {
                length = data.length;
            }

            public final void readInternal(byte[] b, int offset, int len) throws IOException {
                System.arraycopy(data, pos, b, offset, len);
                pos += len;
            }

            public final void seekInternal(long pos) throws IOException {
                this.pos = (int) pos;
            }

            public void close() {
            }
        }
        return new ResourceInputStream();
    }

    /** Closes the store to future operations. */
    public final void close() throws IOException {
    }

    private synchronized void loadList() throws IOException {
        if (fileList != null) return;
        fileList = new String[0];
        java.io.InputStream in = classLoader.getResourceAsStream(base + "files.txt");
        DataInputStream dis = new DataInputStream(in);
        Vector v = new Vector();
        String line;
        while ((line = dis.readLine()) != null) v.addElement(line);
        dis.close();
        fileList = new String[v.size()];
        v.copyInto(fileList);
    }
}
