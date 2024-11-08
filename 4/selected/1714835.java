package molmaster.gui;

import molmaster.*;
import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.image.*;

/** Ensures we don't waste *huge* amounts of 
 *  memory by having identical copies of the 
 *  same image in memory.  Also provides convenience
 *  methods for loading images from disk.
 *
 * @author  Russell Power
 */
public class ImageCache {

    protected static Map imageMap = new HashMap();

    protected static int maxSize = 20;

    protected static Component observer = new javax.swing.JPanel();

    public static BufferedImage getImage(String source) {
        Object test = imageMap.get(source);
        if (test != null) {
            Logger.writeLog(Logger.LOG_DEBUG, "Hit in image cache.");
            return (BufferedImage) test;
        }
        java.net.URL u = imageMap.getClass().getResource("/graphics/" + source);
        Image i = new javax.swing.ImageIcon(u).getImage();
        int w = i.getWidth(observer);
        int h = i.getHeight(observer);
        if (w <= 0 || h <= 0) {
            Logger.writeLog("Couldn't read image...");
        }
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        b.getGraphics().drawImage(i, 0, 0, observer);
        imageMap.put(source, b);
        return b;
    }
}
