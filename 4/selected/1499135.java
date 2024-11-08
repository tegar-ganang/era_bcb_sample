package de.shandschuh.jaolt.core.auction;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import de.shandschuh.jaolt.core.Language;
import de.shandschuh.jaolt.core.wrapper.FileWrapper;
import de.shandschuh.jaolt.tools.BASE64;
import de.shandschuh.jaolt.tools.log.Logger;

public class Picture implements Cloneable {

    /** URL only used if file is stored online */
    private URL url;

    /** Filename - always used, in case of url not null, the local image will be stored here */
    private File file;

    /** Cache for the resized images (important for performance) */
    private HashMap<Integer, ImageIcon> imageIconCache;

    private int width = 0;

    private int height = 0;

    private File lazyPictureCopyDir;

    private int lazyScaleSize;

    /**
	 * Constructor which sets the image and the file name from a given file.
	 * 
	 * @param file picture file
	 */
    public Picture(File file) {
        this.file = file;
        imageIconCache = new HashMap<Integer, ImageIcon>();
    }

    public Picture(String fileName) {
        this(new File(fileName));
    }

    /**
	 * Constructor that creates an empty picture.
	 */
    public Picture() {
        this((File) null);
    }

    public Picture(URL url) {
        this.url = url;
        imageIconCache = new HashMap<Integer, ImageIcon>();
    }

    /**
	 * Returns if a image exists.
	 * 
	 * @return true if image exists, else false
	 */
    public boolean hasImage() {
        return file != null;
    }

    /**
	 * Returns the file name.
	 * 
	 * @return file name
	 */
    public URL getURL() {
        return url;
    }

    /**
	 * Sets a new file name.
	 * 
	 * @param fileName new file name
	 */
    public void setURL(URL url) {
        this.url = url;
        imageIconCache.clear();
    }

    public void setFile(File file) {
        this.file = file;
        imageIconCache.clear();
    }

    public synchronized ImageIcon getImageIcon(int scale) {
        if (lazyPictureCopyDir != null) {
            copyToDir(lazyPictureCopyDir);
            lazyPictureCopyDir = null;
        }
        if (lazyScaleSize > 0) {
            resize(lazyScaleSize);
            lazyScaleSize = 0;
        }
        ImageIcon cacheImageIcon = imageIconCache.get(new Integer(scale));
        if (cacheImageIcon == null) {
            ImageIcon imageIcon = new ImageIcon(file.toString());
            scale(imageIcon, scale);
            imageIconCache.put(new Integer(scale), imageIcon);
            return imageIcon;
        } else {
            return cacheImageIcon;
        }
    }

    public boolean imageExistsInCache(int scale) {
        return imageIconCache.containsKey(new Integer(scale));
    }

    public static void scale(ImageIcon imageIcon, int scale) {
        int newWidth = scale;
        int newHeight = scale;
        int imageWidth = imageIcon.getImage().getWidth(null);
        int imageHeight = imageIcon.getImage().getHeight(null);
        double scaleRatio = (double) imageWidth / (double) imageHeight;
        if (1 < scaleRatio) {
            newHeight = (int) (scale / scaleRatio);
        } else {
            newWidth = (int) (scale * scaleRatio);
        }
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = scaledImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(imageIcon.getImage(), 0, 0, newWidth, newHeight, null);
        imageIcon.setImage(scaledImage);
    }

    public ImageIcon getImageIcon() {
        return getImageIcon(0);
    }

    public Picture clone() {
        try {
            Picture picture = new Picture(file);
            picture.setURL(url);
            picture.setImageIconCache(imageIconCache);
            picture.setHeight(height);
            picture.setWidth(width);
            return picture;
        } catch (Exception exception) {
            Logger.log(exception);
            return null;
        }
    }

    private void setImageIconCache(HashMap<Integer, ImageIcon> imageIconCache) {
        this.imageIconCache = imageIconCache;
    }

    public void copyToDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        } else if (this.file.getParentFile() != null && this.file.getParentFile().equals(dir)) {
            return;
        }
        File file = getEstimatedFileName(dir);
        try {
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            FileInputStream fileInputStream = new FileInputStream(this.file);
            int read = 0;
            byte[] buffer = new byte[1024];
            while (read != -1) {
                fileOutputStream.write(buffer, 0, read);
                read = fileInputStream.read(buffer);
            }
            fileInputStream.close();
            fileOutputStream.close();
            this.file = file;
        } catch (IOException e) {
            Logger.log(e);
        }
    }

    public File getFinalFile() {
        if (lazyPictureCopyDir != null) {
            return getEstimatedFileName(lazyPictureCopyDir);
        } else {
            return new File(file.toString());
        }
    }

    private File getEstimatedFileName(File dir) {
        String originalFileName = getFileName();
        String fileName = new String(originalFileName);
        File file = new File(dir + File.separator + fileName);
        int index = originalFileName.lastIndexOf('.');
        if (index == -1) {
            index = originalFileName.length() - 1;
        }
        for (int n = 2; file.exists(); n++, file = new File(dir + File.separator + fileName)) {
            fileName = originalFileName.substring(0, index) + "_" + n + originalFileName.substring(index);
            file = new File(dir + File.separator + fileName);
        }
        return file;
    }

    public void resize(int scaleSize) {
        try {
            ImageIcon image = new ImageIcon(ImageIO.read(file));
            scale(image, scaleSize);
            BufferedImage bufferedImage = new BufferedImage(image.getIconWidth(), image.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            graphics.drawImage(image.getImage(), 0, 0, null);
            graphics.dispose();
            String fileName = file.toString();
            ImageIO.write(bufferedImage, fileName.substring(fileName.lastIndexOf('.') + 1), file);
        } catch (IOException e) {
            Logger.log(e);
        }
    }

    public void resize(int scaleSize, File newFile) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) {
                throw new IllegalArgumentException(Language.translateStatic("ERROR_COULDNOTREADIMAGE"));
            }
            ImageIcon image = new ImageIcon(bufferedImage);
            width = image.getIconWidth();
            height = image.getIconHeight();
            scale(image, scaleSize);
            bufferedImage = new BufferedImage(image.getIconWidth(), image.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = bufferedImage.createGraphics();
            graphics.drawImage(image.getImage(), 0, 0, null);
            graphics.dispose();
            String fileName = newFile.toString();
            synchronized (Picture.class) {
                ImageIO.write(bufferedImage, fileName.substring(fileName.lastIndexOf('.') + 1), newFile);
            }
        } catch (IOException e) {
            Logger.log(e);
        }
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void resizeLazy(int lazyScaleSize) {
        this.lazyScaleSize = lazyScaleSize;
    }

    public void copyToDirLazy(File lazyPictureCopyDir) {
        this.lazyPictureCopyDir = lazyPictureCopyDir;
    }

    public String getFileExtension() {
        return file != null ? file.toString().substring(file.toString().lastIndexOf('.') + 1) : "jpg";
    }

    public File getFile() {
        return file;
    }

    public String getBASE64() {
        return BASE64.encode(FileWrapper.getByteArray(file));
    }

    public void loadSize() {
        if (width == 0 && height == 0) {
            try {
                BufferedImage image = ImageIO.read(file);
                width = image.getWidth();
                height = image.getHeight();
            } catch (Exception e) {
            }
        }
    }

    public String getFileName() {
        if (file != null) {
            String result = file.getName();
            return result.substring(result.lastIndexOf(File.separator) + 1);
        } else {
            return null;
        }
    }

    public String getContentType() {
        String extension = getFileExtension();
        if ("jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension)) {
            return "image/jpeg";
        } else if ("png".equalsIgnoreCase(extension)) {
            return "image/png";
        } else if ("gif".equalsIgnoreCase(extension)) {
            return "image/gif";
        } else {
            return "image";
        }
    }
}
