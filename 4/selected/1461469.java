package elaborazione.operativo;

import elaborazione.operativo.colospace.GrayScale;
import elaborazione.operativo.colospace.LSColorSpace;
import elaborazione.operativo.colospace.RGB;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 *
 * @author Luca
 */
public class ImageInfo {

    private int h;

    private int w;

    private BufferedImage bufImg = null;

    private int fileSize;

    private int bitsToWrite;

    private float tempoDecodifica = 0;

    private LSColorSpace actualColorSpace = null;

    private LSColorSpace originalColorSpace;

    public ImageInfo(BufferedImage buf, int fileSize) {
        this.fileSize = fileSize;
        bufImg = buf;
        w = buf.getWidth();
        h = buf.getHeight();
        int type = buf.getType();
        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            actualColorSpace = new GrayScale();
        } else if (type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_3BYTE_BGR || type == BufferedImage.TYPE_INT_BGR) {
            actualColorSpace = new RGB();
        } else {
            actualColorSpace = new RGB();
        }
        bitsToWrite = actualColorSpace.getBitsPerChannel() * actualColorSpace.getChannelsNumber();
        originalColorSpace = actualColorSpace;
    }

    /** Crea una nuova istanza di of ImageInfo */
    public ImageInfo(int h, int w, LSColorSpace space, int[][] imgData, int fileSize) {
        this.h = h;
        this.w = w;
        this.fileSize = fileSize;
        bitsToWrite = space.getBitsPerChannel() * space.getChannelsNumber();
        originalColorSpace = actualColorSpace = space;
        createImage(imgData);
    }

    public ImageInfo(int h, int w, LSColorSpace space, int[][] imgData, int fileSize, float tempoDecodifica) {
        this(h, w, space, imgData, fileSize);
        this.tempoDecodifica = tempoDecodifica;
    }

    public ImageInfo(int h, int w, LSColorSpace space, File f) throws IOException {
        this.h = h;
        this.w = w;
        int[][] imageData = new int[h][w];
        originalColorSpace = space;
        bitsToWrite = space.getBitsPerChannel() * space.getChannelsNumber();
        actualColorSpace = (space.getCodifica() == LSColorSpace.GRY ? new GrayScale() : new RGB());
        ImageInputStream ios = ImageIO.createImageInputStream(f);
        space.convertToRGB(ios, w, h, imageData);
        createImage(imageData);
        fileSize = (int) ios.getStreamPosition();
        ios.close();
    }

    public int getFileSize() {
        return fileSize;
    }

    public int[][] getImageData() {
        int[][] imageData = new int[h][w];
        byte[] buffer;
        int ind, offset;
        int[] bufferInt;
        switch(bufImg.getType()) {
            case BufferedImage.TYPE_BYTE_GRAY:
                buffer = ((DataBufferByte) bufImg.getRaster().getDataBuffer()).getData();
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        imageData[i][j] = buffer[w * i + j] & 0xff;
                    }
                }
                break;
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                buffer = ((DataBufferByte) bufImg.getRaster().getDataBuffer()).getData();
                for (int i = 0; i < h; i++) {
                    ind = 0;
                    offset = w * i * 3;
                    for (int j = 0; j < w; ) {
                        imageData[i][j++] = (buffer[offset + ind + 2] & 0xff) << 16 | (buffer[offset + ind + 1] & 0xff) << 8 | (buffer[offset + ind] & 0xff);
                        ind += 3;
                    }
                }
                break;
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                bufferInt = ((DataBufferInt) bufImg.getRaster().getDataBuffer()).getData();
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        imageData[i][j] = bufferInt[w * i + j] & 0xffffff;
                    }
                }
                break;
            case BufferedImage.TYPE_INT_BGR:
                bufferInt = ((DataBufferInt) bufImg.getRaster().getDataBuffer()).getData();
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        imageData[i][j] = Integer.reverse(bufferInt[w * i + j] & 0xffffff) >>> 8;
                    }
                }
                break;
            default:
                for (int i = 0; i < h; i++) {
                    bufImg.getRGB(0, i, w, 1, imageData[i], 0, w);
                }
        }
        return imageData;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    /**
     * Permette di salvare in raster in uno file raw, quindi senza intestazione.
     */
    public void saveImageData(File f) throws IOException {
        long start, end;
        ImageOutputStream ios = ImageIO.createImageOutputStream(f);
        System.out.print("salvataggio in corso...");
        int[][] imageData = getImageData();
        if (imageData != null) {
            start = System.currentTimeMillis();
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    ios.writeBits(imageData[i][j], bitsToWrite);
                }
            }
            end = System.currentTimeMillis();
            System.out.print("salvataggio terminato in " + (end - start) / 1000f + "\n");
        } else {
            start = System.currentTimeMillis();
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    ios.writeBits(bufImg.getRGB(j, i), bitsToWrite);
                }
            }
            end = System.currentTimeMillis();
            System.out.print("salvataggio terminato in " + (end - start) / 1000f + "\n");
        }
        ios.close();
        System.gc();
    }

    private void createImage(int[][] imageData) {
        if (actualColorSpace.getCodifica() == LSColorSpace.GRY) {
            bufImg = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            byte[] data = new byte[w * h];
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    data[i * w + j] = (byte) imageData[i][j];
                }
            }
            DataBufferByte db = new DataBufferByte(data, data.length);
            data = null;
            int[] bandOffsets = new int[] { 0 };
            int bandCount = bandOffsets.length;
            int pixelStride = bandCount;
            int scanlineStride = pixelStride * w;
            ComponentSampleModel csm = new ComponentSampleModel(db.getDataType(), w, h, pixelStride, scanlineStride, bandOffsets);
            WritableRaster r = Raster.createWritableRaster(csm, db, new Point(0, 0));
            bufImg.setData(r);
        } else {
            bufImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < h; i++) {
                bufImg.setRGB(0, i, w, 1, imageData[i], 0, w);
            }
        }
        System.gc();
    }

    /**
     * Permette di ottenere una BufferedImage(visualizzabile su di un JPanel),
     * a partire dai raster raw.
     */
    public BufferedImage getImage() {
        return bufImg;
    }

    public float getTempoDecodifica() {
        return tempoDecodifica;
    }

    public LSColorSpace getOriginalColorSpace() {
        return originalColorSpace;
    }

    public LSColorSpace getActualColorSpace() {
        return actualColorSpace;
    }
}
