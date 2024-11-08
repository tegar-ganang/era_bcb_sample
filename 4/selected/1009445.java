package fileSystem.imageSystem;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

public class Utility {

    private static final Toolkit toolKit = Toolkit.getDefaultToolkit();

    private static final MediaTracker mediaTracker = new MediaTracker(new Container());

    private static final Map cacheImages = new WeakHashMap(100);

    private static final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    static final String newLine = "\r\n";

    private Utility() {
    }

    public static final InputStream getResource(final String fileName) {
        return new BufferedInputStream(classLoader.getResourceAsStream(fileName));
    }

    public static final Image loadImage(final String fileName) {
        String keyName = fileName.trim().toLowerCase();
        Image cacheImage = (Image) cacheImages.get(keyName);
        if (cacheImage == null) {
            InputStream in = new BufferedInputStream(classLoader.getResourceAsStream(fileName));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                byte[] bytes = new byte[8192];
                int read;
                while ((read = in.read(bytes)) >= 0) {
                    byteArrayOutputStream.write(bytes, 0, read);
                }
                byte[] arrayByte = byteArrayOutputStream.toByteArray();
                cacheImages.put(keyName, cacheImage = toolKit.createImage(arrayByte));
                mediaTracker.addImage(cacheImage, 0);
                mediaTracker.waitForID(0);
                waitImage(100, cacheImage);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            } finally {
                try {
                    if (byteArrayOutputStream != null) {
                        byteArrayOutputStream.close();
                        byteArrayOutputStream = null;
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                }
            }
        }
        if (cacheImage == null) {
            throw new RuntimeException(("File not found. ( " + fileName + " )").intern());
        }
        return cacheImage;
    }

    private static final void waitImage(int delay, Image image) {
        try {
            for (int i = 0; i < delay; i++) {
                if (toolKit.prepareImage(image, -1, -1, null)) {
                    return;
                }
                Thread.sleep(delay);
            }
        } catch (Exception e) {
        }
    }

    private static final Image getSmallImage(final Image image, int objectWidth, int objectHeight, int x1, int y1, int x2, int y2) throws Exception {
        BufferedImage buffer = createImage(objectWidth, objectHeight, true);
        Graphics g = buffer.getGraphics();
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.drawImage(image, 0, 0, objectWidth, objectHeight, x1, y1, x2, y2, null);
        graphics2D.dispose();
        graphics2D = null;
        return buffer;
    }

    public static final Image[] getImageRows(Image img, int width) {
        int iWidth = img.getWidth(null);
        int iHeight = img.getHeight(null);
        int size = iWidth / width;
        Image[] imgs = new Image[size];
        for (int i = 1; i <= size; i++) {
            try {
                imgs[i - 1] = transBlackColor(getSmallImage(img, width, iHeight, width * (i - 1), 0, width * i, iHeight));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return imgs;
    }

    public static final Image transBlackColor(final Image img) {
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        PixelGrabber pg = new PixelGrabber(img, 0, 0, width, height, true);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int pixels[] = (int[]) pg.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] <= -11500000) {
                pixels[i] = 16777215;
            }
        }
        return toolKit.createImage(new MemoryImageSource(width, height, pixels, 0, width));
    }

    /**
	 * ���һ��BufferImage
	 * 
	 * @param i
	 * @param j
	 * @param flag
	 * @return
	 */
    public static final BufferedImage createImage(int i, int j, boolean flag) {
        if (flag) {
            return new BufferedImage(i, j, 2);
        } else {
            return new BufferedImage(i, j, 1);
        }
    }

    /**
	 * ���image����
	 * 
	 */
    public static final void destroyImages() {
        cacheImages.clear();
        System.gc();
    }
}
