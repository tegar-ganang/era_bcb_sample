package it.xargon.jvcon;

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;

class Resources {

    private static Toolkit tk = Toolkit.getDefaultToolkit();

    private static HashMap<String, Image> imageCache = new HashMap<String, Image>();

    private static Image createImage(InputStream is) {
        try {
            if (is == null) return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int scan = 0;
            while ((scan = is.read()) != -1) bos.write(scan);
            bos.flush();
            bos.close();
            return tk.createImage(bos.toByteArray());
        } catch (IOException ex) {
            return null;
        }
    }

    public static InputStream getResourceAsStream(String name) {
        return Resources.class.getResourceAsStream(name);
    }

    public static URL getResource(String name) {
        return Resources.class.getResource(name);
    }

    public static Image getImage(String name) {
        Image result = null;
        if (imageCache.containsKey(name)) result = imageCache.get(name); else {
            URL imgurl = getResource(name);
            if (imgurl != null) {
                result = tk.createImage(imgurl);
            } else {
                result = createImage(getResourceAsStream(name));
            }
            if (result != null) imageCache.put(name, result);
        }
        return result;
    }

    public static Icon getIcon(String name) {
        Image img = getImage(name);
        if (img == null) return null;
        return new ImageIcon(img);
    }
}
