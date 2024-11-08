package com.birosoft.liquid.skin;

import java.awt.Component;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SecretLoader {

    static Component component = new Label();

    static byte buffer[] = new byte[4096];

    static Image loadImage(String fileName) {
        byte[] byteArray = null;
        try {
            InputStream fis = SecretLoader.class.getResourceAsStream("/com/birosoft/liquid/icons/" + fileName);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read = fis.read(buffer);
            while (read != -1) {
                bos.write(buffer, 0, read);
                read = fis.read(buffer);
            }
            byteArray = bos.toByteArray();
            read = fis.read(byteArray);
            Image img = java.awt.Toolkit.getDefaultToolkit().createImage(byteArray, 0, byteArray.length);
            MediaTracker tracker = new MediaTracker(component);
            tracker.addImage(img, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException ignore) {
            }
            return img;
        } catch (Throwable t) {
            throw new IllegalArgumentException("File " + fileName + " could not be loaded.");
        }
    }
}
