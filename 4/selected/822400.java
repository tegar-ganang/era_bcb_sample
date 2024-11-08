package ru.ipo.dces.client.resources;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Ilya
 * Date: 22.05.2010
 * Time: 19:17:44
 */
public class Resources {

    private static Resources ourInstance = new Resources();

    public static Resources getInstance() {
        return ourInstance;
    }

    private Resources() {
    }

    public BufferedInputStream getResource(String resourceName) {
        final InputStream stream = Resources.class.getResourceAsStream(resourceName);
        return new BufferedInputStream(stream);
    }

    public byte[] getResourceAsByteArray(String resourceName) {
        try {
            return stream2byteArray(getResource(resourceName));
        } catch (IOException e) {
            dieWithError(null);
            return null;
        }
    }

    private void dieWithError(String message) {
        if (message == null) message = "Program is corrupted, reinstall";
        JOptionPane.showMessageDialog(null, message, "Internal error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private byte[] stream2byteArray(InputStream resource) throws IOException {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read;
        while ((read = resource.read(buffer)) > 0) baos.write(buffer, 0, read);
        return baos.toByteArray();
    }
}
