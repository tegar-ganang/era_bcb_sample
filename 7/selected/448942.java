package jemu.ui.paint;

import java.awt.image.*;
import java.awt.*;
import java.awt.Toolkit;
import java.awt.image.MemoryImageSource;
import java.io.BufferedInputStream;
import java.io.*;
import java.net.URL;

public class Tga {

    public Tga(String file) {
        try {
            loadTGA(new File(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Tga() {
    }

    public Tga(File url) {
        loadTGA(url);
    }

    public static final int TGA_RGB = 2;

    public static final int TGA_A = 3;

    public static final int TGA_RLE = 10;

    public static final int TGA_FILE_NOT_FOUND = 13;

    public static final int TGA_BAD_IMAGE_TYPE = 14;

    public static final int TGA_BAD_DIMENSION = 15;

    public static final int TGA_BAD_BITS = 16;

    public static final int TGA_BAD_DATA = 17;

    public static final int BOTTOM_LEFT_ORIGIN = 0;

    public static final int BOTTOM_RIGHT_ORIGIN = 1;

    public static final int TOP_LEFT_ORIGIN = 2;

    public static final int TOP_RIGHT_ORIGIN = 3;

    public byte[] imageData;

    public int iBits;

    public int texFormat;

    public int imageWidth;

    public int imageHeight;

    int checkSize(int x) {
        if (x == 2 || x == 4 || x == 8 || x == 16 || x == 32 || x == 64 || x == 128 || x == 256 || x == 512) {
            return 1;
        } else {
            return 0;
        }
    }

    byte[] getRGBA(byte[] data, int size) throws IOException {
        byte[] rgba = data;
        byte temp;
        int i;
        int numOfBytes = size * 4;
        for (i = 0; i < numOfBytes; i += 4) {
            temp = rgba[i];
            rgba[i] = rgba[i + 2];
            rgba[i + 2] = temp;
        }
        return rgba;
    }

    byte[] getRGB(byte[] data, int size) throws IOException {
        byte[] rgb = data;
        byte temp;
        int i;
        for (i = 0; i < size * 3; i += 3) {
            temp = rgb[i];
            rgb[i] = rgb[i + 2];
            rgb[i + 2] = temp;
        }
        return rgb;
    }

    byte[] getGray(byte[] data, int size) throws IOException {
        byte[] grayData = data;
        int bread;
        return grayData;
    }

    byte[] getData(byte[] s, int sz) throws IOException {
        if (iBits == 32) {
            return getRGBA(s, sz);
        } else if (iBits == 24) {
            return getRGB(s, sz);
        } else if (iBits == 8) {
            return getGray(s, sz);
        }
        return null;
    }

    int returnError(InputStream s, int error) throws IOException {
        System.out.println("error is: " + error);
        return error;
    }

    int loadTGA(File url) {
        int type[] = new int[4];
        int info[] = new int[6];
        int size;
        FileInputStream stream;
        BufferedInputStream s;
        try {
            stream = new FileInputStream(url);
            s = new BufferedInputStream(stream);
            type[0] = s.read();
            type[1] = s.read();
            type[2] = s.read();
            s.skip(9);
            info[0] = s.read();
            info[1] = s.read();
            info[2] = s.read();
            info[3] = s.read();
            info[4] = s.read();
            info[5] = s.read();
            if (type[1] != 0 || (type[2] != TGA_RGB && type[2] != TGA_A && type[2] != TGA_RLE)) {
                returnError(s, TGA_BAD_IMAGE_TYPE);
            }
            imageWidth = info[0] + info[1] * 256;
            imageHeight = info[2] + info[3] * 256;
            if (imageHeight == 0) {
                imageWidth = 1280;
                imageHeight = 720;
            }
            System.out.println(imageWidth + "x" + imageHeight);
            iBits = info[4];
            texFormat = info[5] >>> 4;
            size = imageWidth * imageHeight;
            if (checkSize(imageWidth) == 0 || checkSize(imageHeight) == 0) {
                System.out.println("Bad dimension!");
            }
            if (iBits != 32 && iBits != 24 && iBits != 8) {
                returnError(s, TGA_BAD_BITS);
            }
            int skipAmount = type[0];
            if (skipAmount < 0) {
                skipAmount += 256;
            }
            if (skipAmount != 0) {
                s.skip(skipAmount);
            }
            if (type[2] == TGA_RLE) {
                imageData = getRLEData(s);
            } else {
                byte[] buf = new byte[size * (iBits / 8)];
                s.read(buf, 0, buf.length);
                imageData = getData(buf, size);
            }
            stream.close();
            s.close();
            if (imageData == null) {
                returnError(s, TGA_BAD_DATA);
            }
            return 1;
        } catch (IOException e) {
            return TGA_FILE_NOT_FOUND;
        }
    }

    BufferedImage getImage(String file) {
        try {
            loadTGA(new File(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        int pixel[] = new int[imageWidth * imageHeight];
        int z = 0;
        int red, green, blue, alpha;
        int startX = 0, maxX = 0, startY = 0, maxY = 0, xInc = 1, yInc = 1;
        switch(texFormat) {
            case BOTTOM_LEFT_ORIGIN:
                {
                    startX = 0;
                    startY = 0;
                    maxX = imageWidth - 1;
                    maxY = imageHeight - 1;
                    break;
                }
            case BOTTOM_RIGHT_ORIGIN:
                {
                    startX = imageWidth - 1;
                    startY = 0;
                    maxX = -1;
                    maxY = imageHeight - 1;
                    xInc = -1;
                    break;
                }
            case TOP_LEFT_ORIGIN:
                {
                    startX = 0;
                    startY = imageHeight - 1;
                    maxX = imageWidth - 1;
                    maxY = -1;
                    yInc = -1;
                    break;
                }
            case TOP_RIGHT_ORIGIN:
                {
                    startX = imageWidth - 1;
                    startY = imageHeight - 1;
                    maxX = -1;
                    maxY = -1;
                    xInc = -1;
                    yInc = -1;
                    break;
                }
        }
        if (iBits == 32) {
            for (int i = startX; i != maxX; i += xInc) {
                for (int j = startY; j != maxY; j += yInc) {
                    red = imageData[(j * 4) + (i * (maxY * 4))] & 0xff;
                    green = imageData[(j * 4) + (i * (maxY * 4)) + 1] & 0xff;
                    blue = imageData[(j * 4) + (i * (maxY * 4)) + 2] & 0xff;
                    alpha = imageData[(j * 4) + (i * (maxY * 4)) + 3] & 0xff;
                    pixel[z++] = (alpha << 24) | (red << 16) | (green << 8) | blue;
                }
            }
        }
        if (iBits == 24) {
            for (int i = startX; i != maxX; i += xInc) {
                for (int j = startY; j != maxY; j += yInc) {
                    red = imageData[(j * 3) + (i * (maxY * 3))] & 0xff;
                    green = imageData[(j * 3) + (i * (maxY * 3)) + 1] & 0xff;
                    blue = imageData[(j * 3) + (i * (maxY * 3)) + 2] & 0xff;
                    pixel[z++] = (255 << 24) | (red << 16) | (green << 8) | blue;
                }
            }
        }
        if (iBits == 8) {
            for (int i = startX; i != maxX; i += xInc) {
                for (int j = startY; j != maxY; j += yInc) {
                    red = imageData[(j * 3) + (i * (maxY * 3))] & 0xff;
                    green = imageData[(j * 3) + (i * (maxY * 3)) + 1] & 0xff;
                    blue = imageData[(j * 3) + (i * (maxY * 3)) + 2] & 0xff;
                    pixel[z++] = (255 << 24) | (red << 16) | (green << 8) | blue;
                }
            }
        }
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image im = tk.createImage(new MemoryImageSource(imageWidth, imageHeight, pixel, 0, imageWidth));
        BufferedImage ima = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = ima.getGraphics();
        g.drawImage(im, 0, 0, null);
        for (int x = 0; x < imageWidth; x++) {
            for (int y = 0; y < imageHeight / 2; y++) {
                Color c = new Color(ima.getRGB(x, y));
                Color d = new Color(ima.getRGB(x, imageHeight - 1 - y));
                g.setColor(d);
                g.drawLine(x, y, x, y);
                g.setColor(c);
                g.drawLine(x, imageHeight - y, x, imageHeight - y);
            }
        }
        return ima;
    }

    byte[] getRLEData(InputStream stream) throws IOException {
        int rleID = 0;
        int colorsRead = 0;
        int channels = iBits / 8;
        int stride = channels * imageWidth;
        int i = 0;
        byte[] data = new byte[stride * imageHeight];
        byte[] pColors = new byte[channels];
        int size = imageWidth * imageHeight;
        while (i < size) {
            rleID = stream.read();
            if (rleID >> 7 == 0) {
                rleID++;
                while (rleID > 0) {
                    stream.read(pColors, 0, pColors.length);
                    data[colorsRead + 0] = pColors[2];
                    data[colorsRead + 1] = pColors[1];
                    data[colorsRead + 2] = pColors[0];
                    if (iBits == 32) {
                        data[colorsRead + 3] = pColors[3];
                    }
                    i++;
                    rleID--;
                    colorsRead += channels;
                }
            } else {
                rleID -= 127;
                stream.read(pColors, 0, pColors.length);
                while (rleID > 0) {
                    data[colorsRead + 0] = pColors[2];
                    data[colorsRead + 1] = pColors[1];
                    data[colorsRead + 2] = pColors[0];
                    if (iBits == 32) {
                        data[colorsRead + 3] = pColors[3];
                    }
                    i++;
                    rleID--;
                    colorsRead += channels;
                }
            }
        }
        return data;
    }
}
