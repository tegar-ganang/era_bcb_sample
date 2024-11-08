package com.jachsoft.imagelib;

public class RGBColor {

    private int color;

    private int r;

    private int g;

    private int b;

    public static RGBColor BLACK = new RGBColor(0x00, 0x00, 0x00);

    public static RGBColor WHITE = new RGBColor(0xFF, 0xFF, 0xFF);

    public static final int ALL_CHANNELS = 0;

    public static final int RED_CHANNEL = 1;

    public static final int GREEN_CHANNEL = 2;

    public static final int BLUE_CHANNEL = 3;

    public RGBColor() {
    }

    public RGBColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
        color = 255 << 8;
        color |= r;
        color <<= 8;
        color |= g;
        color <<= 8;
        color |= b;
    }

    public RGBColor(float r, float g, float b) {
        this.r = (int) (r * 255);
        this.g = (int) (g * 255);
        this.b = (int) (b * 255);
        color = 255 << 8;
        color |= this.r;
        color <<= 8;
        color |= this.g;
        color <<= 8;
        color |= this.b;
    }

    public RGBColor(int color) {
        this.color = color;
        b = color & 255;
        g = (color >> 8) & 255;
        r = (color >> 16) & 255;
    }

    public int getPixelColor() {
        return color;
    }

    public int getRed() {
        return r;
    }

    public float getRedN() {
        return (r / 255.0f);
    }

    public int getGreen() {
        return g;
    }

    public float getGreenN() {
        return (g / 255.0f);
    }

    public int getBlue() {
        return b;
    }

    public float getBlueN() {
        return (b / 255.0f);
    }

    public static int pack(int r, int g, int b) {
        int color = 255 << 8;
        color |= r;
        color <<= 8;
        color |= g;
        color <<= 8;
        color |= b;
        return color;
    }

    public int getChannel(int channel) {
        switch(channel) {
            case ALL_CHANNELS:
                return getPixelColor();
            case RED_CHANNEL:
                return getRed();
            case GREEN_CHANNEL:
                return getGreen();
            case BLUE_CHANNEL:
                return getBlue();
        }
        return 0;
    }

    public float getChannelN(int channel) {
        switch(channel) {
            case RED_CHANNEL:
                return getRedN();
            case GREEN_CHANNEL:
                return getGreenN();
            case BLUE_CHANNEL:
                return getBlueN();
        }
        return 0;
    }

    public String toString() {
        String result = "(" + r + "," + g + "," + b + ")";
        return result;
    }

    public boolean equals(Object o) {
        RGBColor color = (RGBColor) o;
        if ((color.getBlue() == this.b) && (color.getRed() == this.r) && (color.getGreen() == this.g)) {
            return true;
        } else {
            return false;
        }
    }
}
