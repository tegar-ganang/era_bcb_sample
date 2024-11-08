package com.csam.jwebsockets.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/** JavaBean holding information about the feeder
 *
 * @author Nathan Crause <nathan at crause.name>
 * @version 1.0
 */
public class FeederConfig {

    private File file;

    private String language;

    private String source;

    public FeederConfig(File file, String language) {
        this.file = file;
        this.language = language;
    }

    public File getFile() {
        return file;
    }

    public String getLanguage() {
        return language;
    }

    public String getSource() {
        return source;
    }

    public void validate() throws FileNotFoundException, IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        byte[] buffer = new byte[1024];
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        source = out.toString();
    }
}
