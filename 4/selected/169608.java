package akme.mobile.rim;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Image;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.EncodedImage;
import akme.mobile.io.IoUtil;

/**
 * Helper methods for the BlackBerry EncodedImage.
 * 
 * @author kmashint
 */
public abstract class ImageUtil {

    public static final EncodedImage loadEncodedImg(String url) {
        byte[] imgBytes = null;
        InputStream ins = null;
        try {
            ins = Connector.openInputStream(url);
            ByteArrayOutputStream ous = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int n; (n = ins.read(buf)) != -1; ) ous.write(buf, 0, n);
            buf = null;
            imgBytes = ous.toByteArray();
            IoUtil.closeQuiet(ous);
            ous = null;
        } catch (Exception ex) {
            ;
        } finally {
            IoUtil.closeQuiet(ins);
            ins = null;
        }
        EncodedImage result = (imgBytes != null) ? EncodedImage.createEncodedImage(imgBytes, 0, imgBytes.length) : null;
        return result;
    }

    public static final EncodedImage[][] loadEncodedGrid(String url) {
        EncodedImage[][] result = null;
        int pos = url.lastIndexOf('*');
        if (pos != -1) {
            int rows = 0;
            while (IoUtil.exists(url.substring(0, pos) + (rows + 1) + "x1" + url.substring(pos + 1))) rows++;
            int cols = 0;
            while (IoUtil.exists(url.substring(0, pos) + "1x" + (cols + 1) + url.substring(pos + 1))) cols++;
            result = new EncodedImage[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result[i][j] = loadEncodedImg(url.substring(0, pos) + (i + 1) + "x" + (j + 1) + url.substring(pos + 1));
                }
            }
        }
        return result;
    }

    /**
	 * Cross-fade img1 towards img2 at the given percentage and update img3 with the result.
	 * TODO: test!
	 */
    public void crossFade(Bitmap img1, Bitmap img2, Bitmap img3, int pctFrom1To2) {
        int r, g, b;
        int w = img3.getWidth();
        int h = img3.getHeight();
        int[] buf1 = new int[w];
        int[] buf2 = new int[w];
        int[] buf3 = new int[w];
        for (int y = 0; y < h; y++) {
            img1.getARGB(buf1, 0, 1, 0, y, w, h);
            img2.getARGB(buf2, 0, 1, 0, y, w, h);
            img3.getARGB(buf3, 0, 1, 0, y, w, h);
            for (int x = 0; x < w; x++) {
                r = buf1[x] & 0x00ff0000;
                r += ((buf2[x] & 0x00ff0000) - r) * pctFrom1To2 / 100;
                g = buf1[x] & 0x0000ff00;
                g += ((buf2[x] & 0x0000ff00) - g) * pctFrom1To2 / 100;
                b = buf1[x] & 0x000000ff;
                b += ((buf2[x] & 0x000000ff) - b) * pctFrom1To2 / 100;
                buf3[x] = (buf3[x] & 0xff000000) | r | g | b;
            }
            img3.setARGB(buf3, 0, 1, 0, y, w, h);
        }
    }
}
