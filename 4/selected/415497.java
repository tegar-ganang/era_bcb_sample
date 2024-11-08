package net.infopeers.restrant.kitchen.files.local;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import net.infopeers.restrant.kitchen.files.FilePath;
import net.infopeers.restrant.kitchen.files.FileStorageMetadata;

public class LocalFilePath implements FilePath {

    private String backet;

    private String path;

    private File file;

    public LocalFilePath(String path) {
        file = new File(path);
        this.path = path;
        this.backet = null;
    }

    public LocalFilePath(String parent, String path) {
        file = new File(new File(parent), path);
        this.path = path;
        this.backet = parent;
    }

    @Override
    public FileStorageMetadata getMetadata() {
        return new FileStorageMetadata();
    }

    @Override
    public InputStream readAsStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public boolean setReadable(boolean readable) {
        return file.setReadable(readable);
    }

    @Override
    public void write(byte[] data, FileStorageMetadata metadata) throws IOException {
        FileOutputStream os = new FileOutputStream(file);
        try {
            InputStream is = new ByteArrayInputStream(data);
            int length = 4096;
            byte[] buf = new byte[length];
            int readed = 0;
            while ((readed = is.read(buf, 0, buf.length)) != -1) {
                os.write(buf, 0, readed);
            }
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public URL toURL() throws MalformedURLException {
        return file.toURI().toURL();
    }

    @Override
    public String getBacket() {
        return this.backet;
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
