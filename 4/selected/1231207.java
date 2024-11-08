package com.servengine.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;

public class File extends java.io.File {

    private static final long serialVersionUID = 9123867004353614938L;

    public File(java.lang.String p1) {
        super(p1);
    }

    public File(java.lang.String p1, java.lang.String p2) {
        super(p1, p2);
    }

    public File(java.io.File p1, java.lang.String p2) {
        super(p1, p2);
    }

    public File(java.net.URI p1) {
        super(p1);
    }

    public File(java.io.File file) {
        super(file.getAbsolutePath());
    }

    public void store(String content) throws IOException {
        store(new StringReader(content));
    }

    public void store(InputStream source) throws IOException {
        FileOutputStream fos = new FileOutputStream(this);
        byte[] buf = new byte[1024];
        int a = 0;
        while ((a = source.read(buf)) != -1) fos.write(buf, 0, a);
        source.close();
        fos.close();
        return;
    }

    public void store(Reader reader) throws IOException {
        FileWriter writer = new FileWriter(this);
        char[] buf = new char[1024];
        int a = 0;
        while ((a = reader.read(buf)) != -1) writer.write(buf, 0, a);
        reader.close();
        writer.close();
        return;
    }

    public void write(OutputStream fos) throws IOException {
        FileInputStream fis = new FileInputStream(this);
        byte[] buf = new byte[1024];
        int a = 0;
        while ((a = fis.read(buf)) != -1) fos.write(buf, 0, a);
        fis.close();
        fos.close();
        return;
    }

    public void write(Writer writer) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(this));
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        String line = null;
        while ((line = reader.readLine()) != null) {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
        writer.close();
        reader.close();
        return;
    }

    public String getContent() throws IOException {
        StringWriter writer = new StringWriter();
        write(writer);
        return writer.toString();
    }

    public Collection<File> listAllFiles() {
        Collection<File> files = new ArrayList<File>();
        for (java.io.File file : listFiles()) if (file.isDirectory()) files.addAll(new File(file.getAbsoluteFile()).listAllFiles()); else files.add(new File(file.getAbsoluteFile()));
        return files;
    }

    public void deleteAll(String filename) {
        File file = new File(this, filename);
        if (!file.exists()) return;
        for (String subfilename : file.list()) file.deleteAll(subfilename);
        file.delete();
    }

    public void delete(boolean force) {
        if (!force || isFile()) {
            super.delete();
            return;
        }
        for (String subfilename : list()) deleteAll(subfilename);
        super.delete();
    }

    public java.io.File findFileWithUnknownExtension(String filename) {
        FilenameFilter filter = new FilesWithNameAndAnyExtensionFilter(filename);
        java.io.File[] files = listFiles(filter);
        if (files == null || files.length == 0) return null;
        return files[0];
    }

    private class FilesWithNameAndAnyExtensionFilter implements FilenameFilter {

        String filename;

        FilesWithNameAndAnyExtensionFilter(String filename) {
            this.filename = filename;
        }

        public boolean accept(java.io.File dir, String name) {
            return name.startsWith(filename);
        }
    }

    public long dirSize() {
        return dirSize(this);
    }

    private long dirSize(java.io.File file) {
        String[] files = file.list();
        long size = 0;
        if (files != null) for (int n = 0; n < files.length; n++) {
            java.io.File f = new java.io.File(getAbsolutePath(), files[n]);
            if (f.isDirectory()) size += dirSize(f); else size += f.length();
        }
        return size;
    }

    public byte[] getData() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        write(bytes);
        return bytes.toByteArray();
    }
}
