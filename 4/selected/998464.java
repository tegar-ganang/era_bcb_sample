package com.jaeksoft.searchlib.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IndexFile {

    private File file;

    private FileOutputStream fos;

    public IndexFile(File parentDir, String indexRef, String fileName) {
        File indexDir = new File(parentDir, indexRef);
        if (!indexDir.exists()) indexDir.mkdir();
        this.file = new File(indexDir, fileName);
    }

    protected void writeBuffer(byte[] buffer, int length) throws IOException {
        fos.write(buffer, 0, length);
    }

    public void put(InputStream is) throws IOException {
        fos = new FileOutputStream(file);
        byte[] buffer = new byte[65536];
        int l;
        while ((l = is.read(buffer)) != -1) writeBuffer(buffer, l);
        fos.close();
    }
}
