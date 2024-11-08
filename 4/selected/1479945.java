package com.yugruk.chooser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.ImageIcon;

/**
 *
 * @author tayfun
 */
public class ImageIconLoader {

    private static ImageIconLoader instance;

    private static final int BUFFER_SIZE = 131072;

    private ByteArrayOutputStream baos;

    private byte[] buffer;

    private static final String PATH = "/com/yugruk/chooser/images/";

    /** Creates a new instance of ImageIconLoader */
    private ImageIconLoader() {
        baos = new ByteArrayOutputStream(BUFFER_SIZE);
    }

    public static ImageIconLoader getInstance() {
        return instance;
    }

    /**
     *  Iconların bulunduğu jar yolundan ikonları getirir.
     *
     */
    public ImageIcon getIcon(String name) {
        baos.reset();
        InputStream is = ImageIconLoader.class.getResourceAsStream(PATH + name);
        byte[] tmpBuf = new byte[4096];
        int readed = 0;
        if (is != null) {
            try {
                while ((readed = is.read(tmpBuf, 0, 4096)) > 0) {
                    baos.write(tmpBuf, 0, readed);
                }
                is.close();
            } catch (IOException ex) {
                return null;
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                }
            }
            buffer = baos.toByteArray();
            ImageIcon icon = new ImageIcon(buffer);
            return icon;
        }
        return null;
    }

    static {
        instance = new ImageIconLoader();
    }
}
