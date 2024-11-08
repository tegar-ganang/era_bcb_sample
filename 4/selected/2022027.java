package net.sf.ovanttasks.ov4native.pefile.res;

import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.ImageIcon;
import net.sf.ovanttasks.structures.StructHelper;

/**
 * @see
 */
public class ResIcon {

    private long size;

    private long width;

    private long height;

    private int planes;

    private int bitsPerPixel;

    private long compression;

    private long sizeOfBitmap;

    private long horzResolution;

    private long vertResolution;

    private long colorsUsed;

    private long colorsImportant;

    private PaletteElement[] palette;

    private short[] bitmapXOR;

    private short[] bitmapAND;

    void readMemFileForamt(ReadableByteChannel ch) throws IOException, IllegalArgumentException, IllegalAccessException {
        Icons_H.MEMICONDIR iconDir = new Icons_H.MEMICONDIR();
        StructHelper.readStructure(iconDir, ch);
    }

    public class PaletteElement {

        private int blue;

        private int green;

        private int red;

        private int reserved;

        @Override
        public String toString() {
            return "{" + blue + "," + green + "," + red + "," + reserved + "}";
        }
    }

    /**
     * Creates a new instance of ResIcon
     * 
     * @see
     * @param in
     * @throws IOException 
     */
    public ResIcon() {
        super();
    }

    public void readFromBuffer(ReadableByteChannel chan) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(0x34);
        headerBuffer.order(ByteOrder.LITTLE_ENDIAN);
        chan.read(headerBuffer);
        headerBuffer.position(0);
        int idReserved = headerBuffer.getShort(0x00);
        int idType = headerBuffer.getShort(0x02);
        int idCount = headerBuffer.getShort(0x04);
        short bWidth = headerBuffer.get(0x06);
        short bHeight = headerBuffer.get(0x07);
        short bColorCount = headerBuffer.get(0x08);
        short bReserved = headerBuffer.get(0x09);
        int wPlanes = headerBuffer.getShort(0x0a);
        int wBitCount = headerBuffer.getShort(0x0c);
        int dwBytesInRes = headerBuffer.getShort(0x0e);
        int dwImageOffset = headerBuffer.getShort(0x10);
        size = headerBuffer.getShort(0x12);
        width = headerBuffer.getShort(0x14);
        height = headerBuffer.getShort(0x16);
        planes = headerBuffer.getShort(0x18);
        bitsPerPixel = headerBuffer.getShort(0x1a);
        compression = headerBuffer.getInt(0x1c);
        sizeOfBitmap = headerBuffer.getInt(0x20);
        horzResolution = headerBuffer.getInt(0x24);
        vertResolution = headerBuffer.getInt(0x28);
        colorsUsed = headerBuffer.getInt(0x2c);
        colorsImportant = headerBuffer.getInt(0x30);
        int cols = (int) colorsUsed;
        if (cols == 0) {
            cols = 1 << bitsPerPixel;
        }
        ByteBuffer dataBuffer = ByteBuffer.allocate((int) size);
        chan.read(dataBuffer);
    }

    /**
     * Creates a new instance based on the data of the Image argument.
     * 
     * @param img
     */
    public ResIcon(Image img) throws Exception {
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        if ((width % 8) != 0) {
            width += (7 - (width % 8));
        }
        if ((height % 8) != 0) {
            height += (7 - (height % 8));
        }
        int[] pixelbuffer = new int[width * height];
        PixelGrabber grabber = new PixelGrabber(img, 0, 0, width, height, pixelbuffer, 0, width);
        try {
            grabber.grabPixels();
        } catch (InterruptedException e) {
            System.err.println("interrupted waiting for pixels!");
            throw new Exception("Can't load the image provided", e);
        }
        Hashtable colors = calculateColorCount(pixelbuffer);
        this.bitsPerPixel = 8;
        palette = new ResIcon.PaletteElement[1 << bitsPerPixel];
        for (Enumeration e = colors.keys(); e.hasMoreElements(); ) {
            Integer pixi = (Integer) e.nextElement();
            int pix = pixi.intValue();
            int index = ((Integer) colors.get(pixi)).intValue();
            palette[index] = new ResIcon.PaletteElement();
            palette[index].blue = pix & 0xFF;
            palette[index].green = (pix >> 8) & 0xff;
            palette[index].red = (pix >> 16) & 0xff;
        }
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == null) {
                palette[i] = new ResIcon.PaletteElement();
            }
        }
        this.size = 40;
        this.width = width;
        this.height = height * 2;
        this.planes = 1;
        this.compression = 0;
        this.sizeOfBitmap = 0;
        this.horzResolution = 0;
        this.vertResolution = 0;
        this.colorsUsed = 0;
        this.colorsImportant = 0;
        bitmapXOR = new short[(((int) height / 2) * (int) width * bitsPerPixel) / 8];
        bitmapAND = new short[(((int) height / 2) * (int) width) / 8];
        int bxl = bitmapXOR.length - 1;
        int bal = bitmapAND.length - 1;
        for (int i = 0; i < pixelbuffer.length; i++) {
            int col = i % width;
            int line = i / width;
            bxl = (width * height) - (((i / width) + 1) * width) + (i % width);
            bal = ((width * height) / 8) - ((line + 1) * (width / 8)) + (col / 8);
            if (false && (((pixelbuffer[i] >> 24) & 0xFF) != 0xff)) {
                bitmapAND[bal] |= 1 << (7 - (i % 8));
                bitmapXOR[bxl] = 0xFF;
            } else {
                int pixel = pixelbuffer[i] & 0x00FFFFFF;
                Integer icol = (Integer) colors.get(new Integer(pixel));
                if (icol != null) {
                    int palindex = icol.intValue();
                    bitmapXOR[bxl] = (short) palindex;
                }
            }
        }
    }

    private int getBrightest() {
        int result = 0;
        int averesult = 0;
        for (int i = 0; i < palette.length; i++) {
            int ave1 = (palette[0].red + palette[0].green + palette[0].blue) / 3;
            if (ave1 > averesult) {
                averesult = ave1;
                result = i;
            }
        }
        return result;
    }

    private Hashtable calculateColorCount(int[] pixels) {
        Hashtable result = new Hashtable();
        int colorindex = 0;
        for (int i = 0; i < pixels.length; i++) {
            int pix = pixels[i];
            pix &= 0x00FFFFFF;
            Integer pixi = new Integer(pix);
            Object o = result.get(pixi);
            if (o == null) {
                result.put(pixi, new Integer(colorindex++));
            }
            if (colorindex > 256) {
                return result;
            }
        }
        return result;
    }

    /**
     * Creates and returns a ByteBuffer containing an image under the .ico
     * format expected by Windows.
     * 
     * @return a ByteBuffer with the .ico data
     */
    public ByteBuffer getData() {
        int cols = (int) colorsUsed;
        if (cols == 0) {
            cols = 1 << bitsPerPixel;
        }
        ByteBuffer buf = ByteBuffer.allocate((int) (40 + (cols * 4) + (width * (height / 2) * bitsPerPixel) / 8 + (width * (height / 2)) / 8));
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(0);
        buf.putInt((int) size);
        buf.putInt((int) width);
        buf.putInt((int) height);
        buf.putShort((short) planes);
        buf.putShort((short) bitsPerPixel);
        buf.putInt((int) compression);
        buf.putInt((int) sizeOfBitmap);
        buf.putInt((int) horzResolution);
        buf.putInt((int) vertResolution);
        buf.putInt((int) colorsUsed);
        buf.putInt((int) colorsImportant);
        for (int i = 0; i < palette.length; i++) {
            PaletteElement el = palette[i];
            buf.put((byte) el.blue);
            buf.put((byte) el.green);
            buf.put((byte) el.red);
            buf.put((byte) el.reserved);
        }
        switch(bitsPerPixel) {
            case 4:
                {
                    for (int i = 0; i < bitmapXOR.length; i += 2) {
                        int v1 = bitmapXOR[i];
                        int v2 = bitmapXOR[i + 1];
                        buf.put((byte) ((v1 << 4) | v2));
                    }
                }
                break;
            case 8:
                {
                    for (int i = 0; i < bitmapXOR.length; i++) {
                        buf.put((byte) bitmapXOR[i]);
                    }
                }
                break;
            default:
                throw new RuntimeException("BitRes " + bitsPerPixel + " not supported!");
        }
        for (int i = 0; i < bitmapAND.length; i++) {
            buf.put((byte) bitmapAND[i]);
        }
        buf.position(0);
        return buf;
    }

    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append("size: " + size);
        out.append("\nWidth: " + width);
        out.append("\nHeight: " + height);
        out.append("\nPlanes: " + planes);
        out.append("\nBitsPerPixel: " + bitsPerPixel);
        out.append("\nCompression: " + compression);
        out.append("\nSizeOfBitmap: " + sizeOfBitmap);
        out.append("\nHorzResolution: " + horzResolution);
        out.append("\nVertResolution: " + vertResolution);
        out.append("\nColorsUsed: " + colorsUsed);
        out.append("\nColorsImportant: " + colorsImportant);
        out.append("\nBitmapXOR[" + bitmapXOR.length + "]={");
        for (int i = 0; i < bitmapXOR.length; i++) {
            out.append((byte) bitmapXOR[i]);
        }
        out.append("}\nBitmapAnd[" + bitmapAND.length + "]={");
        for (int i = 0; i < bitmapAND.length; i++) {
            out.append((byte) bitmapAND[i]);
        }
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        ImageIcon ii = new ImageIcon("/home/aploese/workspace/AmikoViewer/AmikoViewer.gif");
        Image img = ii.getImage().getScaledInstance(32, 32, Image.SCALE_DEFAULT);
        while (img.getHeight(null) == -1) {
            Thread.sleep(50);
        }
        ResIcon ri = new ResIcon(img);
        ByteBuffer bb = ri.getData();
        FileOutputStream fos = new FileOutputStream("/home/aploese/test_20070720.ico");
        ByteArrayInputStream bas = new ByteArrayInputStream(bb.array());
        byte[] buffer = new byte[8192];
        int read;
        while ((read = bas.read(buffer)) > -1) {
            fos.write(buffer, 0, read);
        }
        fos.close();
    }

    public short[] getBitmapAND() {
        return bitmapAND;
    }

    public short[] getBitmapXOR() {
        return bitmapXOR;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public long getColorsImportant() {
        return colorsImportant;
    }

    public long getColorsUsed() {
        return colorsUsed;
    }

    public long getCompression() {
        return compression;
    }

    public long getHeight() {
        return height;
    }

    public long getHorzResolution() {
        return horzResolution;
    }

    public PaletteElement[] getPalette() {
        return palette;
    }

    public int getPlanes() {
        return planes;
    }

    public long getSize() {
        return size;
    }

    public long getSizeOfBitmap() {
        return sizeOfBitmap;
    }

    public long getVertResolution() {
        return vertResolution;
    }

    public long getWidth() {
        return width;
    }
}
