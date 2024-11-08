package tufts.vue.ibisimage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import tufts.vue.*;
import tufts.vue.LWComponent.Flag;
import tufts.vue.ibisicon.*;
import org.apache.batik.dom.svg.*;
import org.apache.batik.*;

public abstract class IBISImage extends LWImage {

    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(IBISImage.class);

    protected static int IBISIconMaxSide = 32;

    protected static int IBISDefaultWidth = 32;

    protected static int IBISDefaultHeight = 32;

    public static int getIBISIconMaxSide() {
        return IBISIconMaxSide;
    }

    public static int getIBISDefaultWidth() {
        return IBISDefaultWidth;
    }

    public static int getIBISDefaultHeight() {
        return IBISDefaultHeight;
    }

    private IBISImageIcon mIcon = null;

    public IBISImage() {
    }

    public IBISImage(Resource r) {
        super(r, IBISIconMaxSide, IBISDefaultWidth, IBISDefaultHeight, false);
    }

    public IBISImage(Resource r, int iconMaxSide, int width, int height, boolean unsized) {
        super(r, iconMaxSide, width, height, unsized);
        IBISIconMaxSide = iconMaxSide;
        IBISDefaultWidth = width;
        IBISDefaultHeight = height;
    }

    public abstract void setIcon();

    public abstract IBISImageIcon getIcon();

    private static File CacheDir;

    private static File createCacheDirectory() {
        if (CacheDir == null) {
            File dir = VueUtil.getDefaultUserFolder();
            CacheDir = new File(dir, "ibiscache");
            if (!CacheDir.exists()) {
                Log.debug("creating cache directory: " + CacheDir);
                if (!CacheDir.mkdir()) Log.warn("couldn't create cache directory " + CacheDir);
            } else if (!CacheDir.isDirectory()) {
                Log.warn("couldn't create cache directory (is a file) " + CacheDir);
                return CacheDir = null;
            }
            Log.debug("Got cache directory: " + CacheDir);
        }
        return CacheDir;
    }

    public void setCacheDir(File theDir) {
        CacheDir = theDir;
    }

    public File getCacheDir() {
        return createCacheDirectory();
    }

    public static boolean writeImageToJPG(File file, BufferedImage bufferedImage) throws IOException {
        if (readImageFromFile(file) == null) return ImageIO.write(bufferedImage, "jpg", file); else return true;
    }

    private static String getFileExtension(File file) {
        if (file == null) return "";
        String fileName = file.getName();
        int mid = fileName.lastIndexOf(".");
        String ext = fileName.substring(mid + 1, fileName.length());
        return ext;
    }

    public static boolean writeImageToFile(File file, BufferedImage bufferedImage) throws IOException {
        if (file == null) return false;
        String strExtn = getFileExtension(file);
        if (readImageFromFile(file) == null) return ImageIO.write(bufferedImage, strExtn, file); else return true;
    }

    public static BufferedImage readImageFromImageIO(File file) {
        BufferedImage theImage = null;
        try {
            theImage = ImageIO.read(file);
        } catch (Exception e) {
        } finally {
            return theImage;
        }
    }

    public static BufferedImage readImageFromFile(File file) throws IOException {
        BufferedImage theImage = null;
        theImage = readImageFromImageIO(file);
        return theImage;
    }

    public static File createImageFile(String theFile, BufferedImage theImage) {
        File imgFile = new File(createCacheDirectory(), theFile);
        try {
            if (writeImageToFile(imgFile, theImage)) return imgFile;
        } catch (IOException e) {
            e.printStackTrace();
            imgFile = null;
        } finally {
            return imgFile;
        }
    }
}
