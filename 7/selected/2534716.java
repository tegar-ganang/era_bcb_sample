package org.xith3d.image;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Handles dealing with windows bitmap files.
 * This class doesn't handle palettized files.
 * +--------------------------------------+
 * | Bitmap File Header                   |
 * +--------------------------------------+
 * | Bitmap Information Header            |
 * +--------------------------------------+
 * | Palette Data (only in 8 bit files)   |
 * +--------------------------------------+
 * | Bitmap Data                          |
 * +--------------------------------------+
 * 
 * @author Scott Shaver
 */
public class WindowsBitmapFile implements org.xith3d.image.ImageFile {

    private int FHsize = 0;

    private short FHreserved1 = 0;

    private short FHreserved2 = 0;

    private int FHoffsetBits = 0;

    private int IHsize = 0;

    private int IHwidth = 0;

    private int IHheight = 0;

    private short IHplanes = 0;

    private short IHbitCount = 0;

    private int IHcompression = 0;

    private int IHsizeImage = 0;

    private long IHxpelsPerMeter = 0;

    private long IHypelsPerMeter = 0;

    private int IHcolorsUsed = 0;

    private int IHcolorsImportant = 0;

    private int filePointer = 0;

    private byte[] fileContents = null;

    private byte[] data = null;

    public WindowsBitmapFile() {
    }

    public byte[] getData() {
        return (data);
    }

    public int getWidth() {
        return (IHwidth);
    }

    public int getHeight() {
        return (IHheight);
    }

    public int getBPP() {
        return (IHbitCount);
    }

    public int getDataLength() {
        return (data.length);
    }

    public static BufferedImage getBufferedImage(String filename) {
        WindowsBitmapFile loader = new WindowsBitmapFile();
        loader.load(filename);
        int width = loader.getWidth(), height = loader.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        byte[] imageData = loader.getData();
        for (int j = height - 1; j >= 0; j--) {
            for (int i = 0; i < width; i++) {
                int index = ((height - 1 - j) * width + i) * 3, color = (255 & 0xFF) << 24 | (imageData[index + 2] & 0xFF) << 16 | (imageData[index + 1] & 0xFF) << 8 | (imageData[index + 0] & 0xFF);
                bufferedImage.setRGB(i, j, color);
            }
        }
        return (bufferedImage);
    }

    public void printHeaders() {
        System.out.println("-----------------------------------");
        System.out.println("File Header");
        System.out.println("-----------------------------------");
        System.out.println("            File Size:" + FHsize);
        System.out.println("           Reserved 1:" + FHreserved1);
        System.out.println("           Reserved 2:" + FHreserved2);
        System.out.println("          Data offset:" + FHoffsetBits);
        System.out.println("-----------------------------------");
        System.out.println("Info Header");
        System.out.println("-----------------------------------");
        System.out.println("     Info Header Size:" + IHsize);
        System.out.println("                Width:" + IHwidth);
        System.out.println("               Height:" + IHheight);
        System.out.println("               Planes:" + IHplanes);
        System.out.println("                  BPP:" + IHbitCount);
        System.out.println("          Compression:" + IHcompression);
        System.out.println("           Image size:" + IHsizeImage);
        System.out.println("     Pels Per Meter X:" + IHxpelsPerMeter);
        System.out.println("     Pels Per Meter Y:" + IHypelsPerMeter);
        System.out.println("     # of Colors Used:" + IHcolorsUsed);
        System.out.println("# of Important Colors:" + IHcolorsImportant);
    }

    public void load(String filename) {
        FHsize = 0;
        FHreserved1 = 0;
        FHreserved2 = 0;
        FHoffsetBits = 0;
        IHsize = 0;
        IHwidth = 0;
        IHheight = 0;
        IHplanes = 0;
        IHbitCount = 0;
        IHcompression = 0;
        IHsizeImage = 0;
        IHxpelsPerMeter = 0;
        IHypelsPerMeter = 0;
        IHcolorsUsed = 0;
        IHcolorsImportant = 0;
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
            short magicNumber = readShort();
            if (magicNumber != 19778) {
                fileContents = null;
                return;
            }
            FHsize = readInt();
            FHreserved1 = readShort();
            FHreserved2 = readShort();
            FHoffsetBits = readInt();
            IHsize = readInt();
            IHwidth = readInt();
            IHheight = readInt();
            IHplanes = readShort();
            IHbitCount = readShort();
            IHcompression = readInt();
            IHsizeImage = readInt();
            IHxpelsPerMeter = readInt();
            IHypelsPerMeter = readInt();
            IHcolorsUsed = readInt();
            IHcolorsImportant = readInt();
            data = new byte[IHsizeImage];
            System.arraycopy(fileContents, FHoffsetBits, data, 0, IHsizeImage);
            fileContents = null;
            for (int loop = 0; loop < IHsizeImage; loop += 3) {
                byte btemp = data[loop];
                data[loop] = data[loop + 2];
                data[loop + 2] = btemp;
            }
        } catch (Exception x) {
            x.printStackTrace();
            System.out.println(x.getMessage());
        }
    }

    private short readShort() {
        int s1 = (fileContents[filePointer++] & 0xFF), s2 = (fileContents[filePointer++] & 0xFF) << 8;
        return ((short) (s1 | s2));
    }

    private int readInt() {
        return ((fileContents[filePointer++] & 0xFF) | (fileContents[filePointer++] & 0xFF) << 8 | (fileContents[filePointer++] & 0xFF) << 16 | (fileContents[filePointer++] & 0xFF) << 24);
    }
}
