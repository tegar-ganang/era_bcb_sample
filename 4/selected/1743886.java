package com.jungleford.util;

import java.io.*;
import java.awt.*;

/**
 * @author
 */
public class Utilities {

    /**
   * <p>
   * Description: Return an Image based on the supplied image identifier. The
   * image is assumed to reside at the defined location within the same
   * repository as this class.
   */
    public static Image getImageFromJar(final String imageId, Class c) {
        Image image = null;
        final InputStream inputStream = c.getResourceAsStream(imageId);
        if (inputStream != null) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                final byte[] bytes = new byte[1024];
                int read = 0;
                while ((read = inputStream.read(bytes)) >= 0) {
                    byteArrayOutputStream.write(bytes, 0, read);
                }
                image = Toolkit.getDefaultToolkit().createImage(byteArrayOutputStream.toByteArray());
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return image;
    }

    public static String getTextFromJar(final String filename, Class c) {
        String text = "";
        final InputStream inputStream = c.getResourceAsStream(filename);
        if (inputStream != null) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String s;
                while ((s = in.readLine()) != null) {
                    text += s + "\n";
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return text;
    }
}
