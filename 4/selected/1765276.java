package com.stroke.util;

import org.apache.log4j.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Author: Igor Bubelov
 * Date: 3/8/11 6:33 PM
 */
public class IOHelper {

    public static final void copyInputStream(InputStream in, OutputStream out) {
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(IOHelper.class).error("Failed to copy stream", ex);
        }
    }

    public static final void copyToOutput(InputStream in, OutputStream out) {
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
        } catch (IOException ex) {
            Logger.getLogger(IOHelper.class).error("Failed to copy stream", ex);
        }
    }

    public static BufferedImage loadImage(File file) {
        BufferedImage image = null;
        if (!file.exists()) {
            Logger.getLogger(IOHelper.class).error("Failed to load image. File: " + file.getName() + " doesn't exists");
            return null;
        }
        try {
            image = ImageIO.read(file);
        } catch (IOException ex) {
            Logger.getLogger(IOHelper.class).error("Failed to load image", ex);
        }
        return image;
    }

    public static void addZipEntry(ZipOutputStream out, InputStream in, String name) {
        try {
            out.putNextEntry(new ZipEntry(name));
            copyToOutput(in, out);
            in.close();
            out.closeEntry();
        } catch (Exception ex) {
            Logger.getLogger(IOHelper.class).error("Failed to add ZIP entry", ex);
        }
    }

    public static String getFormatName(Object o) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(o);
            Iterator iterator = ImageIO.getImageReaders(iis);
            if (!iterator.hasNext()) {
                return null;
            }
            ImageReader reader = (ImageReader) iterator.next();
            return reader.getFormatName();
        } catch (Exception ex) {
            Logger.getLogger(IOHelper.class).error("Failed to get a format name", ex);
            return null;
        }
    }
}
