package com.jachsoft.imagelib;

import java.awt.image.BufferedImage;

public class RGBImage {

    protected BufferedImage bimg;

    public RGBImage() {
    }

    public RGBImage(RGBImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        this.bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                this.bimg.setRGB(x, y, img.getBufferedImage().getRGB(x, y));
            }
        }
    }

    public RGBImage(BufferedImage bimg) {
        this.bimg = bimg;
    }

    public RGBImage(int w, int h) {
        bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    public void setRGB(int x, int y, int r, int g, int b) {
        int rgb = RGBColor.pack(r, g, b);
        bimg.setRGB(x, y, rgb);
    }

    public void setRGB(int x, int y, float r, float g, float b) {
        int rgb = RGBColor.pack((int) (r * 255), (int) (g * 255), (int) (b * 255));
        bimg.setRGB(x, y, rgb);
    }

    public void setRGB(int x, int y, int rgb) {
        bimg.setRGB(x, y, rgb);
    }

    public RGBColor getRGBColor(int x, int y) {
        RGBColor rgb = new RGBColor(bimg.getRGB(x, y));
        return rgb;
    }

    public BufferedImage getBufferedImage() {
        return bimg;
    }

    public RGBImage getGrayScaleImage(float wr, float wg, float wb) {
        int w = bimg.getWidth();
        int h = bimg.getHeight();
        RGBImage gray = new RGBImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                RGBColor col = this.getRGBColor(x, y);
                float color = ((col.getBlue() * wb + col.getGreen() * wg + col.getRed() * wr) / 255f);
                gray.setRGB(x, y, color, color, color);
            }
        }
        return gray;
    }

    public RGBImage getNegative() {
        int w = bimg.getWidth();
        int h = bimg.getHeight();
        RGBImage retval = this;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                RGBColor col = this.getRGBColor(x, y);
                int r = 255 - col.getRed();
                int g = 255 - col.getGreen();
                int b = 255 - col.getBlue();
                retval.setRGB(x, y, r, g, b);
            }
        }
        return retval;
    }

    public RGBImage getGrayScaleImage() {
        return getGrayScaleImage(0.333f, 0.333f, 0.333f);
    }

    public void setBufferedImage(BufferedImage bimg) {
        this.bimg = bimg;
    }

    public int getWidth() {
        return bimg.getWidth();
    }

    public int getHeight() {
        return bimg.getHeight();
    }

    public void clear() {
        int width = bimg.getWidth();
        int height = bimg.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bimg.setRGB(x, y, 0xFF000000);
            }
        }
    }

    public Neighbor getNeighbor(int x, int y, int channel, int type) {
        Neighbor nbor = new Neighbor(type);
        int offset = nbor.getOffset();
        int ulx = x - offset;
        int uly = y - offset;
        for (int i = (y - offset); i <= y + offset; i++) {
            for (int j = (x - offset); j <= x + offset; j++) {
                RGBColor rgb = this.getRGBColor(j, i);
                nbor.setValue(j - ulx, i - uly, rgb.getChannel(channel));
            }
        }
        return nbor;
    }

    public RGBImage copy() {
        int width = bimg.getWidth();
        int height = bimg.getHeight();
        RGBImage dup = new RGBImage(width, height);
        for (int i = 0; i < height; i++) for (int j = 0; j < width; j++) {
            RGBColor color = this.getRGBColor(i, j);
            dup.setRGB(i, j, color.getRed(), color.getGreen(), color.getBlue());
        }
        return dup;
    }

    public DataArray getDataArray(int channel) {
        int width = bimg.getWidth();
        int height = bimg.getHeight();
        DataArray retval = new DataArray(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                RGBColor c = this.getRGBColor(x, y);
                switch(channel) {
                    case RGBColor.RED_CHANNEL:
                        retval.setValue(x, y, c.getRed());
                        break;
                    case RGBColor.GREEN_CHANNEL:
                        retval.setValue(x, y, c.getGreen());
                        break;
                    case RGBColor.BLUE_CHANNEL:
                        retval.setValue(x, y, c.getBlue());
                        break;
                }
            }
        }
        return retval;
    }

    public RGBImage getRegion(ImageRegion region) {
        int ulx = region.getUlx();
        int uly = region.getUly();
        int rw = region.getWidth();
        int rh = region.getHeight();
        RGBImage retval = new RGBImage(rw, rh);
        for (int y = uly, i = 0; y < (uly + rh); y++, i++) {
            for (int x = ulx, j = 0; x < (ulx + rw); x++, j++) {
                RGBColor c = this.getRGBColor(x, y);
                retval.setRGB(j, i, c.getRed(), c.getGreen(), c.getBlue());
            }
        }
        return retval;
    }
}
