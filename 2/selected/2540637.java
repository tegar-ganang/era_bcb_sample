package org.compiere.plaf;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;
import javax.swing.*;
import com.sun.image.codec.jpeg.*;

/**
 *  UI utilities
 *
 *  @author     Jorg Janke
 *  @version    $Id: AdempiereUtils.java,v 1.2 2006/07/30 00:52:23 jjanke Exp $
 */
public class CompiereUtils {

    /**	Logger			*/
    private static Logger log = Logger.getLogger(CompiereUtils.class.getName());

    /**
	 *  Fill Background with Color.
	 *  (Ususlly called from update methods)
	 *
	 *  @param g2D  Graphics
	 *  @param c    Component
	 *  @param round paint round corners
	 */
    public static void fillRectange(Graphics2D g2D, JComponent c, boolean round) {
        CompiereColor cc = null;
        boolean stdCC = c.getClientProperty(CompiereLookAndFeel.BACKGROUND_FILL) != null;
        try {
            cc = (CompiereColor) c.getClientProperty(CompiereLookAndFeel.BACKGROUND);
        } catch (Exception e) {
            stdCC = true;
        }
        if (stdCC) cc = CompiereColor.getDefaultBackground();
        if (cc != null) {
            cc.paint(g2D, c);
        } else {
            Paint paint = c.getBackground();
            g2D.setPaint(paint);
            RectangularShape rec = null;
            if (round) rec = new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), 15, 15); else rec = new Rectangle(0, 0, c.getWidth(), c.getHeight());
            g2D.fill(rec);
        }
    }

    /** Top Top Color - white 128       the higher the ligher   */
    public static final Color COL_1TOP = new Color(255, 255, 255, 128);

    /** End Top Color - white 0         */
    public static final Color COL_1END = new Color(255, 255, 255, 0);

    /** Top End Color - black 0         */
    public static final Color COL_2TOP = new Color(0, 0, 0, 0);

    /** End End Color - black 64        the higher the darker   */
    public static final Color COL_2END = new Color(0, 0, 0, 64);

    /**
	 *  Paint 3D effect in (lighten in upper half, darken in lowerhalf)
	 *  (called from paint methods)
	 *
	 *  @param g2D Graphics
	 *  @param r Ractangle
	 *  @param round paint round corners
	 *  @param out paint sticking out (not pressed)
	 */
    public static void paint3Deffect(Graphics2D g2D, Rectangle r, boolean round, boolean out) {
        GradientPaint topPaint = null;
        if (out) topPaint = new GradientPaint(r.x, r.y, COL_1TOP, r.x, r.y + r.height / 2, COL_1END); else topPaint = new GradientPaint(r.x, r.y, COL_2END, r.x, r.y + r.height / 2, COL_2TOP);
        g2D.setPaint(topPaint);
        RectangularShape topRec = null;
        if (round) topRec = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height / 2, 15, 15); else topRec = new Rectangle(r.x, r.y, r.width, r.height / 2);
        g2D.fill(topRec);
        GradientPaint endPaint = null;
        if (out) endPaint = new GradientPaint(r.x, r.y + r.height / 2, COL_2TOP, r.x, r.y + r.height, COL_2END); else endPaint = new GradientPaint(r.x, r.y + r.height / 2, COL_1END, r.x, r.y + r.height, COL_1TOP);
        g2D.setPaint(endPaint);
        RectangularShape endRec = null;
        if (round) endRec = new RoundRectangle2D.Float(r.x, r.y + r.height / 2, r.width, r.height / 2, 15, 15); else endRec = new Rectangle(r.x, r.y + r.height / 2, r.width, r.height / 2);
        g2D.fill(endRec);
    }

    /**
	 *  Paint 3D effect in (lighten in upper half, darken in lowerhalf)
	 *  (called from paint methods)
	 *
	 *  @param g2D Graphics
	 *  @param c Component
	 *  @param round paint round corners
	 *  @param out paint sticking out (not pressed)
	 */
    public static void paint3Deffect(Graphics2D g2D, JComponent c, boolean round, boolean out) {
        GradientPaint topPaint = null;
        if (out) topPaint = new GradientPaint(0, 0, COL_1TOP, 0, c.getHeight() / 2, COL_1END); else topPaint = new GradientPaint(0, 0, COL_2END, 0, c.getHeight() / 2, COL_2TOP);
        g2D.setPaint(topPaint);
        RectangularShape topRec = null;
        if (round) topRec = new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight() / 2, 15, 15); else topRec = new Rectangle(0, 0, c.getWidth(), c.getHeight() / 2);
        g2D.fill(topRec);
        GradientPaint endPaint = null;
        if (out) endPaint = new GradientPaint(0, c.getHeight() / 2, COL_2TOP, 0, c.getHeight(), COL_2END); else endPaint = new GradientPaint(0, c.getHeight() / 2, COL_1END, 0, c.getHeight(), COL_1TOP);
        g2D.setPaint(endPaint);
        RectangularShape endRec = null;
        if (round) endRec = new RoundRectangle2D.Float(0, c.getHeight() / 2, c.getWidth(), c.getHeight() / 2, 15, 15); else endRec = new Rectangle(0, c.getHeight() / 2, c.getWidth(), c.getHeight() / 2);
        g2D.fill(endRec);
    }

    /*************************************************************************
	 *  Helper to simplify creation of translucent colors
	 *  @param c Color
	 *  @param alpha alpha
	 *  @return Translucent Color
	 */
    public static Color getTranslucentColor(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /**
	 *  Set Not Buffered. Recursive for all contained components
	 *  @param c
	 */
    public static void setNotBuffered(Component c) {
        if (c instanceof JComponent) ((JComponent) c).setDoubleBuffered(false);
        if (c instanceof Container) {
            Component[] cc = ((Container) c).getComponents();
            for (int i = 0; i < cc.length; i++) setNotBuffered(cc[i]);
        }
    }

    /** Component for media tracker         */
    protected static final Component s_component = new Component() {
    };

    /** Media tracker                       */
    protected static final MediaTracker s_tracker = new MediaTracker(s_component);

    /**
	 *  load the image located at path.
	 *
	 *  @param path location of image file in local file system
	 *  - otherwise relative to class
	 *  @return loaded image at path or url
	 *  @see java.io.File#toURL()
	 */
    public static synchronized Image loadImage(String path) {
        Image image = null;
        try {
            File file = new File(path);
            URL url = file.toURL();
            image = loadImage(url);
        } catch (MalformedURLException e) {
            log.severe("Path= " + path + " - " + e.getMessage());
        }
        return image;
    }

    /**
	 *  Load the image located at URL.
	 *
	 *  @param url URL where the image file is located.
	 *  @return loaded image at path or url
	 *  @see java.io.File#toURL()
	 */
    public static synchronized Image loadImage(URL url) {
        Image image = null;
        image = Toolkit.getDefaultToolkit().getImage(url);
        if (image != null) {
            s_tracker.addImage(image, 0);
            try {
                s_tracker.waitForAll();
            } catch (InterruptedException e) {
                log.severe("Url= " + url + " - " + e.getMessage());
                s_tracker.removeImage(image);
                image = null;
            } finally {
                if (image != null) s_tracker.removeImage(image);
                if (s_tracker.isErrorAny()) {
                    log.severe("Tracker: " + s_tracker.getErrorsAny()[0]);
                    image = null;
                }
                if (image != null) {
                    if (image.getWidth(null) < 0 || image.getHeight(null) < 0) {
                        log.severe("Image=0");
                        image = null;
                    }
                }
            }
        }
        return image;
    }

    /**
	 *  Load an image from a given file into a BufferedImage.
	 *  The image is returned in the format defined by the imageType parameter.
	 *  Note that this is special cased for JPEG images where loading is performed
	 *  outside the standard media tracker, for efficiency reasons.
	 *
	 *  @param file File where the image file is located.
	 *  @param imageType one of the image type defined in the BufferedImage class.
	 *  @return loaded image at path or url
	 *  @see java.awt.image.BufferedImage
	 */
    public static synchronized BufferedImage loadBufferedImage(File file, int imageType) {
        BufferedImage image = null;
        try {
            URL url = file.toURL();
            image = loadBufferedImage(url, imageType);
        } catch (MalformedURLException e) {
            log.severe("File: " + file + " - " + e.getMessage());
        }
        return image;
    }

    /**
	 *  Load an image from a given path into a BufferedImage.
	 *  The image is returned in the format defined by the imageType parameter.
	 *  Note that this is special cased for JPEG images where loading is performed
	 *  outside the standard media tracker, for efficiency reasons.
	 *
	 *  @param path Name of file where the image file is located.
	 *  @param imageType one of the image type defined in the BufferedImage class.
	 *  @return loaded image at path or url
	 *  @see java.awt.image.BufferedImage
	 */
    public static synchronized BufferedImage loadBufferedImage(String path, int imageType) {
        File file = new File(path);
        BufferedImage image = null;
        try {
            URL url = file.toURL();
            image = loadBufferedImage(url, imageType);
        } catch (MalformedURLException e) {
            log.severe("Path: " + path + " - " + e.getMessage());
        }
        return image;
    }

    /**
	 *  Loads an image from a given URL into a BufferedImage.
	 *  The image is returned in the format defined by the imageType parameter.
	 *  Note that this is special cased for JPEG images where loading is performed
	 *  outside the standard media tracker, for efficiency reasons.
	 *
	 *  @param url URL where the image file is located.
	 *  @param imageType one of the image type defined in the BufferedImage class.
	 *  @return loaded image at path or url
	 *  @see java.awt.image.BufferedImage
	 */
    public static synchronized BufferedImage loadBufferedImage(URL url, int imageType) {
        BufferedImage image = null;
        if (url == null || !url.toString().toLowerCase().endsWith(".jpg")) {
            Image tmpImage = loadImage(url);
            if (tmpImage != null) {
                image = new BufferedImage(tmpImage.getWidth(null), tmpImage.getHeight(null), imageType);
                Graphics2D g = image.createGraphics();
                g.drawImage(tmpImage, 0, 0, null);
                g.dispose();
            }
        } else {
            BufferedImage tmpImage = loadBufferedJPEGImage(url);
            if (tmpImage != null) {
                if (tmpImage.getType() != imageType) {
                    log.config("Incompatible JPEG image type: creating new buffer image");
                    image = new BufferedImage(tmpImage.getWidth(null), tmpImage.getHeight(null), imageType);
                    Graphics2D g = image.createGraphics();
                    g.drawImage(tmpImage, 0, 0, null);
                    g.dispose();
                } else image = tmpImage;
            }
        }
        return image;
    }

    /**
	 *  Load a JPEG image from a given location.
	 *
	 *  @param url URL where the image file is located.
	 *  @return loaded image at path or url
	 */
    public static synchronized BufferedImage loadBufferedJPEGImage(URL url) {
        BufferedImage image = null;
        if (url != null) {
            InputStream in = null;
            try {
                in = url.openStream();
                JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(in);
                image = decoder.decodeAsBufferedImage();
            } catch (Exception e) {
                log.severe("URL: " + url + " - " + e.getMessage());
                image = null;
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException ioe) {
                    log.severe("URL: " + url + " - " + ioe.getMessage());
                }
            }
            if (image != null) {
                log.config("Image type : " + image.getType());
                if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                    log.severe("URL: " + url + " =0");
                    image = null;
                }
            }
        }
        return image;
    }

    /*************************************************************************
	 *  Convenience function for determining ComponentOrientation.
	 *  Copied from MetalUtils
	 *  @param c Component
	 *  @return true, if left to right
	 */
    public static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    /** Debug SequenceNo        */
    private static int s_no = 0;

    /**
	 *  Print Oarent of Component
	 *  @param c component
	 */
    static void printParents(JComponent c) {
        if (c.getName() == null) c.setName("C" + String.valueOf(s_no++));
        System.out.print(c.getName());
        System.out.print(" - " + c.getClass().getName());
        System.out.println(" ** " + c.isOpaque() + " bg=" + (c.getClientProperty(CompiereLookAndFeel.BACKGROUND) != null));
        Container container = c.getParent();
        while (container != null) {
            System.out.print(" - " + container.getName() + " " + container.getClass().getName() + " ** " + container.isOpaque());
            if (container instanceof JComponent) System.out.print(" bg=" + (((JComponent) container).getClientProperty(CompiereLookAndFeel.BACKGROUND) != null));
            System.out.println();
            container = container.getParent();
        }
    }
}
