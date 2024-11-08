package org.xith3d.image;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handles dealing with targa image files.
 * +--------------------------------------+
 * | File Header                          |
 * +--------------------------------------+
 * | Bitmap Data                          |
 * +--------------------------------------+
 * 
 * @author Scott Shaver
 */
public class TargaFile implements org.xith3d.image.ImageFile {

    private byte FHimageIDLength = 0;

    private byte FHcolorMapType = 0;

    private byte FHimageType = 0;

    private short FHcolorMapOrigin = 0;

    private short FHcolorMapLength = 0;

    private byte FHcolorMapDepth = 0;

    private short FHimageXOrigin = 0;

    private short FHimageYOrigin = 0;

    private short FHwidth = 0;

    private short FHheight = 0;

    private byte FHbitCount = 0;

    private byte FHimageDescriptor = 0;

    private int filePointer = 0;

    private byte fileContents[] = null;

    private byte[] data = null;

    public TargaFile() {
    }

    public byte[] getData() {
        return (data);
    }

    public int getWidth() {
        return (FHwidth);
    }

    public int getHeight() {
        return (FHheight);
    }

    public int getBPP() {
        return (FHbitCount);
    }

    public int getDataLength() {
        return (data.length);
    }

    public static BufferedImage getBufferedImage(String filename) {
        TargaFile loader = new TargaFile();
        loader.load(filename);
        int width = loader.getWidth(), height = loader.getHeight(), bytePerPixel = loader.getBPP() / 8;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        byte[] imageData = loader.getData();
        for (int j = height - 1; j >= 0; j--) for (int i = 0; i < width; i++) {
            int index = ((height - 1 - j) * width + i) * bytePerPixel;
            byte alpha = (bytePerPixel == 4) ? imageData[index + 3] : (byte) 255;
            int color = (alpha & 0xFF) << 24 | (imageData[index + 2] & 0xFF) << 16 | (imageData[index + 1] & 0xFF) << 8 | (imageData[index + 0] & 0xFF);
            bufferedImage.setRGB(i, j, color);
        }
        return (bufferedImage);
    }

    public void printHeaders() {
        System.out.println("-----------------------------------");
        System.out.println("File Header");
        System.out.println("-----------------------------------");
        System.out.println("      Image ID Length:" + FHimageIDLength);
        System.out.println("       Color Map Type:" + FHcolorMapType);
        System.out.println("           Image Type:" + FHimageType);
        System.out.println("     Color Map Origin:" + FHcolorMapOrigin);
        System.out.println("     Color Map Length:" + FHcolorMapLength);
        System.out.println(" Color Map Entry Size:" + FHcolorMapDepth);
        System.out.println("       Image X Origin:" + FHimageXOrigin);
        System.out.println("       Image Y Origin:" + FHimageYOrigin);
        System.out.println("                Width:" + FHwidth);
        System.out.println("               Height:" + FHheight);
        System.out.println("                  BBP:" + FHbitCount);
        System.out.println("     Image Descriptor:" + FHimageDescriptor);
    }

    public void load(String filename) {
        FHimageIDLength = 0;
        FHcolorMapType = 0;
        FHimageType = 0;
        FHcolorMapOrigin = 0;
        FHcolorMapLength = 0;
        FHcolorMapDepth = 0;
        FHimageXOrigin = 0;
        FHimageYOrigin = 0;
        FHwidth = 0;
        FHheight = 0;
        FHbitCount = 0;
        FHimageDescriptor = 0;
        filePointer = 0;
        InputStream dis = ClassLoader.getSystemResourceAsStream(filename);
        try {
            if (dis == null) dis = new FileInputStream(filename);
            fileContents = new byte[dis.available()];
            dis.read(fileContents);
            try {
                dis.close();
            } catch (Exception x) {
            }
            FHimageIDLength = (byte) readUnsignedByte();
            FHcolorMapType = (byte) readUnsignedByte();
            FHimageType = (byte) readUnsignedByte();
            FHcolorMapOrigin = readShort();
            FHcolorMapLength = readShort();
            FHcolorMapDepth = (byte) readUnsignedByte();
            FHimageXOrigin = readShort();
            FHimageYOrigin = readShort();
            FHwidth = readShort();
            FHheight = readShort();
            FHbitCount = (byte) readUnsignedByte();
            FHimageDescriptor = (byte) readUnsignedByte();
            if (FHimageType != 2 && FHimageType != 3) {
                if (FHimageType == 10) loadCompressed();
                fileContents = null;
                return;
            }
            int bytesPerPixel = (FHbitCount / 8);
            data = new byte[FHwidth * FHheight * bytesPerPixel];
            System.arraycopy(fileContents, filePointer, data, 0, data.length);
            if (FHbitCount == 24 || FHbitCount == 32) {
                for (int loop = 0; loop < data.length; loop += bytesPerPixel) {
                    byte btemp = data[loop];
                    data[loop] = data[loop + 2];
                    data[loop + 2] = btemp;
                }
            }
            fileContents = null;
        } catch (Exception x) {
            x.printStackTrace();
            System.out.println(x.getMessage());
        }
    }

    public void loadCompressed() {
        printHeaders();
        int bytesPerPixel = (FHbitCount / 8);
        data = new byte[FHwidth * FHheight * bytesPerPixel];
        int pixelcount = FHwidth * FHheight, currentbyte = 0, currentpixel = 0;
        byte[] colorbuffer = new byte[bytesPerPixel];
        try {
            do {
                int chunkheader = readUnsignedByte();
                System.out.println(chunkheader);
                if (chunkheader < 128) {
                    chunkheader++;
                    for (short counter = 0; counter < chunkheader; counter++) {
                        readColorBuffer(colorbuffer);
                        data[currentbyte + 0] = colorbuffer[2];
                        data[currentbyte + 1] = colorbuffer[1];
                        data[currentbyte + 2] = colorbuffer[0];
                        if (bytesPerPixel == 4) data[currentbyte + 3] = (byte) readUnsignedByte();
                        currentbyte += bytesPerPixel;
                        currentpixel++;
                        if (currentpixel > pixelcount) throw (new IOException("Too many pixels read"));
                    }
                } else {
                    chunkheader -= 127;
                    readColorBuffer(colorbuffer);
                    for (short counter = 0; counter < chunkheader; counter++) {
                        data[currentbyte + 0] = colorbuffer[2];
                        data[currentbyte + 1] = colorbuffer[1];
                        data[currentbyte + 2] = colorbuffer[0];
                        if (bytesPerPixel == 4) data[currentbyte + 3] = (byte) readUnsignedByte();
                        currentbyte += bytesPerPixel;
                        currentpixel++;
                        if (currentpixel > pixelcount) throw (new IOException("Too many pixels read"));
                    }
                }
            } while (currentpixel < pixelcount);
        } catch (Exception x) {
            x.printStackTrace();
            System.out.println(x.getMessage());
        }
    }

    private void readColorBuffer(byte[] buffer) {
        buffer[0] = (byte) readUnsignedByte();
        buffer[1] = (byte) readUnsignedByte();
        buffer[2] = (byte) readUnsignedByte();
    }

    private int readUnsignedByte() {
        return ((int) fileContents[filePointer++] & 0xFF);
    }

    private short readShort() {
        int s1 = (fileContents[filePointer++] & 0xFF), s2 = (fileContents[filePointer++] & 0xFF) << 8;
        return ((short) (s1 | s2));
    }
}
