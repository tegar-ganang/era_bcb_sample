package org.magnesia.misc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.KeyEvent;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {

    public static Logger l = Logger.getLogger("Magnesia");

    static {
        l.setLevel(Level.FINEST);
    }

    public static void log(String message) {
        System.out.println(message);
        l.fine(message);
    }

    public static boolean accept(File f) {
        if (f.isDirectory()) return true;
        String name = f.getName().toLowerCase();
        return (name.matches(".+\\.jpe?g$")) && f.canRead();
    }

    /**
	 * Selfpads a key if its length is not a multiple of 8
	 * 
	 * @param key
	 * @return selfpadded key
	 */
    public static byte[] prepareKey(String key) {
        if (key.length() == 0) {
            key = "00000000";
            log("Created key of 0s! This is insecure!");
        } else if (key.length() > 8) {
            key = key.substring(0, 8);
            log("Truncated key to length 8!");
        }
        StringBuilder sb = new StringBuilder(key);
        for (int i = 0; i < 8 - key.length(); i++) {
            sb.append(sb.charAt(i));
        }
        return sb.toString().getBytes();
    }

    public static byte[] decrypt(byte[] challenge, String password) {
        try {
            byte[] keyBytes = prepareKey(password);
            Cipher c = Cipher.getInstance("DES/CBC/NoPadding");
            SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 }));
            byte[] plainText = new byte[challenge.length];
            int ptLength = c.update(challenge, 0, challenge.length, plainText, 0);
            ptLength += c.doFinal(plainText, ptLength);
            return plainText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    public static String keycode2String(int keyCode) {
        int expected_modifiers = (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        Field[] fields = KeyEvent.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                if (fields[i].getModifiers() == expected_modifiers && fields[i].getType() == Integer.TYPE && fields[i].getName().startsWith("VK_") && fields[i].getInt(KeyEvent.class) == keyCode) {
                    String name = fields[i].getName().substring(3);
                    if (name.length() > 1) return "<" + name + ">";
                    return name;
                }
            } catch (IllegalAccessException e) {
                assert (false);
            }
        }
        return "<UNKNOWN>";
    }

    public static Dimension getScale(Image i, int width) {
        if (i.getWidth(null) <= width) return new Dimension(i.getWidth(null), i.getHeight(null));
        return new Dimension(width, (int) (i.getHeight(null) / ((double) i.getWidth(null) / width)));
    }

    public static byte[] getData(BufferedImage bi) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageWriter iw = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
            JPEGImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.89f);
            param.setOptimizeHuffmanTables(true);
            iw.setOutput(ios);
            IIOImage io = new IIOImage(bi, null, null);
            iw.write(null, io, param);
            ios.flush();
            iw.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }

    public static void writeFile(BufferedImage bi, File f, IIOMetadata metadata, IIOMetadata stream) {
        writeFile(new IIOImage(bi, null, metadata), f, stream);
    }

    public static void writeFile(IIOImage iio, File f, IIOMetadata stream) {
        try {
            OutputStream os = new FileOutputStream(f);
            ImageWriter iw = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            JPEGImageWriteParam params = (JPEGImageWriteParam) iw.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.89f);
            params.setOptimizeHuffmanTables(true);
            IIOMetadata defaultMetadata = iw.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(iio.getRenderedImage()), params);
            IIOMetadata metadata = iio.getMetadata();
            try {
                metadata.mergeTree(metadata.getNativeMetadataFormatName(), defaultMetadata.getAsTree(defaultMetadata.getNativeMetadataFormatName()));
            } catch (NullPointerException npe) {
                log("Failed to merge metadata. Looks like bug 4895547 is still there");
            }
            iw.setOutput(ios);
            iw.write(stream, iio, params);
            ios.flush();
            iw.dispose();
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] readFile(File f) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(f);
            byte[] chunk = new byte[org.magnesia.Constants.CHUNK_SIZE];
            int read = 0;
            while ((read = fis.read(chunk)) > 0) {
                baos.write(chunk, 0, read);
            }
            fis.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getFormattedSize(long val) {
        DecimalFormat df = new DecimalFormat("#.##");
        String size = df.format(val) + "B";
        if (val > 1024 * 1024) size = df.format(val / 1024.0 / 1024) + "MB"; else if (val > 1024) size = df.format(val / 1024.0) + "KB";
        return size;
    }

    public static String getFormattedSize(File f) {
        return getFormattedSize(f.length());
    }

    public static BufferedImage drawScaled(Image i, int width) {
        Dimension d = getScale(i, width);
        if (!GraphicsEnvironment.isHeadless()) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
            VolatileImage vi = gc.createCompatibleVolatileImage(d.width, d.height);
            Graphics2D g = vi.createGraphics();
            if (width > 300) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
            g.drawImage(i, 0, 0, d.width, d.height, Color.BLACK, null);
            g.dispose();
            BufferedImage bi = vi.getSnapshot();
            ;
            if (vi.contentsLost()) {
                while (vi.validate(gc) != VolatileImage.IMAGE_RESTORED) ;
                bi = vi.getSnapshot();
            }
            vi.flush();
            return bi;
        } else {
            BufferedImage bi = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();
            if (width > 300) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
            g.drawImage(i, 0, 0, d.width, d.height, Color.BLACK, null);
            g.dispose();
            return bi;
        }
    }
}
