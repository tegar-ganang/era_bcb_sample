package net.sf.evemsp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;
import javax.microedition.lcdui.Image;
import PTViewer.JPEGDecoder;
import net.sf.evemsp.data.ChrRecord;
import net.sf.evemsp.data.EveRecordManager;
import net.sf.evemsp.data.ImgRecord;

public final class ChrImageUtil {

    private ChrImageUtil() {
    }

    /**
	 * Decodes JPEG information
	 */
    private static class ImgArray implements JPEGDecoder.PixelArray {

        int width, height;

        int[] rawImage;

        ImgArray() {
        }

        public void setPixel(int x, int y, int argb) {
            rawImage[x + (width * y)] = argb;
        }

        public void setSize(int width, int height) throws Exception {
            this.width = width;
            this.height = height;
            rawImage = new int[width * height];
        }
    }

    public static final int SMALL = 16;

    public static final int MEDIUM = 32;

    public static final int LARGE = 64;

    public static Image getImage(ChrRecord charRec, int scale) {
        ImgRecord imgRec = EveRecordManager.getInstance().getImgRecord(charRec);
        if (imgRec == null) {
            imgRec = loadImgRecord(charRec.getId());
        }
        if (imgRec == null) {
            return null;
        }
        return createImage(imgRec, scale);
    }

    private static class ImageLoader implements Runnable {

        private int charId;

        private byte[] data = null;

        public ImageLoader(int charId) {
            this.charId = charId;
        }

        public void run() {
            try {
                String url = "http://img.eve.is/serv.asp?s=64&c=" + charId;
                try {
                    HttpConnection conn = null;
                    try {
                        conn = (HttpConnection) Connector.open(url);
                    } catch (SecurityException x) {
                        System.err.println(x);
                        return;
                    }
                    try {
                        conn.setRequestMethod(HttpsConnection.GET);
                        InputStream is = conn.openInputStream();
                        if (conn.getResponseCode() == HttpConnection.HTTP_OK) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1000];
                            int read;
                            do {
                                read = is.read(buffer);
                                if (read < 0) {
                                    baos.flush();
                                    baos.close();
                                    data = baos.toByteArray();
                                }
                                if (read > 0) {
                                    baos.write(buffer, 0, read);
                                }
                            } while (data == null);
                        } else {
                            System.out.println("conn.getResponseCode():" + conn.getResponseCode());
                        }
                    } finally {
                        conn.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    private static ImgRecord loadImgRecord(final int chrId) {
        ImageLoader loader = new ImageLoader(chrId);
        byte[] imgData = null;
        synchronized (loader) {
            Thread t = new Thread(loader);
            t.start();
            try {
                loader.wait();
                imgData = loader.data;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ImgRecord imgRec = null;
        if (imgData != null) {
            imgRec = new ImgRecord();
            imgRec.setChrId(chrId);
            imgRec.setImgType(ImgRecord.JPG);
            imgRec.setImgData(imgData);
            EveRecordManager.getInstance().store(imgRec);
        }
        return imgRec;
    }

    public static Image createImage(ImgRecord rec, int scale) {
        switch(rec.getImgType()) {
            case ImgRecord.JPG:
                return createJpg(rec.getImgData(), scale);
            case ImgRecord.PNG:
                return createPng(rec.getImgData(), scale);
            default:
                throw new IllegalArgumentException("Uknown image type:" + rec.getImgType());
        }
    }

    private static Image createJpg(byte[] jpgData, int scale) {
        ByteArrayInputStream bais = new ByteArrayInputStream(jpgData);
        JPEGDecoder decoder = new JPEGDecoder();
        ImgArray imgArray = new ImgArray();
        Image image = null;
        try {
            decoder.decode(bais, imgArray);
            int[] raw = imgArray.rawImage;
            int w = imgArray.width;
            int h = imgArray.height;
            switch(scale) {
                case SMALL:
                    {
                        w = h = SMALL;
                        raw = new int[w * h];
                        for (int x = 0; x < w; x++) {
                            for (int y = 0; y < h; y++) {
                                int tgt = x + (w * y);
                                int src = (x * 4) + (w * (y * 8));
                                raw[tgt] = imgArray.rawImage[src];
                            }
                        }
                    }
                    break;
                case MEDIUM:
                    {
                        w = h = MEDIUM;
                        raw = new int[w * h];
                        for (int x = 0; x < w; x++) {
                            for (int y = 0; y < h; y++) {
                                int tgt = x + (w * y);
                                int src = (x * 2) + (w * (y * 4));
                                raw[tgt] = imgArray.rawImage[src];
                            }
                        }
                    }
                    break;
            }
            image = Image.createRGBImage(raw, w, h, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

    private static Image createPng(byte[] pngData, int scale) {
        Image image = Image.createImage(pngData, 0, pngData.length);
        if (scale != LARGE) {
        }
        return image;
    }
}
