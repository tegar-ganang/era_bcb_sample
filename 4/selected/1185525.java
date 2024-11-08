package render;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.applet.*;
import java.net.*;

/**
 * is this really a factory ?
 */
public class TextureFactory {

    int _x;

    int _y;

    private Component _c = new Label("small component");

    public TextureFactory() {
    }

    private Image _image(String name) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image img = null;
        try {
            MediaTracker mt = new MediaTracker(_c);
            InputStream in = io.RootDir.getResourceAsStream(name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read = in.read();
            while (read != -1) {
                out.write(read);
                read = in.read();
            }
            in.close();
            byte[] buffer = out.toByteArray();
            img = Toolkit.getDefaultToolkit().createImage(buffer);
            mt.addImage(img, 0);
            mt.waitForID(0);
        } catch (Exception e) {
            System.err.println("Unable to read image.");
            e.printStackTrace();
        }
        _x = img.getWidth(null);
        _y = img.getHeight(null);
        return img;
    }

    public Texture loadTexture(String name) throws Exception {
        Texture result = null;
        Image img = _image(name);
        int[] pixels = new int[_x * _y];
        PixelGrabber gb = new PixelGrabber(img, 0, 0, _x, _y, pixels, 0, _x);
        if (gb.grabPixels() && (gb.status() & ImageObserver.ALLBITS) != 0) {
            result = new TextureImpl(pixels, _x, _y);
        } else {
            int status = gb.status();
            System.out.println(status);
            System.out.println(status & ImageObserver.SOMEBITS);
            System.out.println(status & ImageObserver.ALLBITS);
            System.out.println(status & ImageObserver.ERROR);
            System.out.println("grabbing failed");
        }
        return result;
    }
}
