package com.dynamide.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zip {

    public static final int ZM_NONE = 0;

    public static final int ZM_READ = 1;

    public static final int ZM_WRITE = 2;

    public static final int ZM_APPEND = 3;

    private int m_mode;

    private ZipOutputStream zo = null;

    public Zip(String zipfilename, int mode) throws java.io.IOException {
        if (mode == ZM_WRITE) {
            FileOutputStream fo = new FileOutputStream(new File(zipfilename));
            zo = new ZipOutputStream(fo);
            m_mode = mode;
        }
    }

    public void close() throws java.io.IOException {
        if (m_mode == ZM_WRITE) {
            zo.close();
            zo = null;
        }
        m_mode = ZM_NONE;
    }

    public boolean addToZip(String filename, String entryName) {
        if (m_mode != ZM_WRITE) return false;
        try {
            FileInputStream fi = new FileInputStream(new File(filename));
            zo.putNextEntry(new ZipEntry(entryName));
            byte[] buff = new byte[10000];
            int read;
            read = fi.read(buff);
            boolean eof = (read == -1);
            while (!eof) {
                zo.write(buff, 0, read);
                read = fi.read(buff);
                eof = (read == -1);
            }
            zo.closeEntry();
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return true;
    }

    public boolean replaceEntry(String filename, String entryName) {
        return false;
    }

    public boolean addDirToZip(String dirname, String entryName, boolean recurse) {
        return false;
    }

    public boolean addDirToZip(String dirname, String[] filters, String entryName, boolean recurse) {
        return false;
    }
}
