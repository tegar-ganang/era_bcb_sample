package com.germinus.xpression.cms.contents.binary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author agonzalez
 *
 */
public class FileBinaryDataHolder implements BinaryDataHolder {

    private File _file;

    /**
     * 
     */
    public FileBinaryDataHolder(File f) {
        super();
        _file = f;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(_file);
    }

    public byte[] getData() throws IOException {
        InputStream stream = getInputStream();
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read;
        while ((read = stream.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    public String getEncoding() {
        return null;
    }
}
