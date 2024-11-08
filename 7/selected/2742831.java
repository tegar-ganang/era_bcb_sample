package com.flagstone.transform.util.image;

import com.flagstone.transform.coder.Coder;

/**
 * ImageFilter contains a set of convenience methods for processing the
 * pixels in an image.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class ImageFilter {

    /** Shift used to align the RGB555 red channel to a 8-bit pixel. */
    private static final int RGB5_MSB_MASK = 0x00F8;

    /** Shift used to align the RGB555 red channel to a 8-bit pixel. */
    private static final int R5_SHIFT = 7;

    /** Shift used to align the RGB555 green channel to a 8-bit pixel. */
    private static final int G5_SHIFT = 2;

    /** Shift used to align the RGB555 blue channel to a 8-bit pixel. */
    private static final int B5_SHIFT = 3;

    /** Level used to indicate an opaque colour. */
    private static final int OPAQUE = 255;

    /** Byte offset to red channel. */
    private static final int RED = 0;

    /** Byte offset to red channel. */
    private static final int GREEN = 1;

    /** Byte offset to blue channel. */
    private static final int BLUE = 2;

    /** Byte offset to alpha channel. */
    private static final int ALPHA = 3;

    private static final int RGBA_CHANNELS = 4;

    private static final int RGB_CHANNELS = 3;

    public byte[] removeAlpha(final byte[] image) {
        final byte[] out = new byte[(image.length / RGBA_CHANNELS) * RGB_CHANNELS];
        int dst = 0;
        for (int i = 0; i < image.length; i += RGBA_CHANNELS) {
            out[dst++] = image[i];
            out[dst++] = image[i + 1];
            out[dst++] = image[i + 2];
        }
        return out;
    }

    public byte[] invertRGB(final byte[] image, final int width, final int height) {
        final byte[] out = new byte[image.length];
        int dst = 0;
        int src = 0;
        for (int row = height - 1; row >= 0; row--) {
            src = row * width;
            for (int col = 0; col < width; col++, src += RGB_CHANNELS) {
                out[dst++] = image[src];
                out[dst++] = image[src + 1];
                out[dst++] = image[src + 2];
            }
        }
        return out;
    }

    public void reverseRGB(final byte[] image) {
        byte swap;
        for (int i = 0; i < image.length; i += RGB_CHANNELS) {
            swap = image[i];
            image[i] = image[i + 2];
            image[i + 2] = swap;
        }
    }

    public void reverseRGBA(final byte[] image) {
        byte alpha;
        for (int i = 0; i < image.length; i += RGBA_CHANNELS) {
            alpha = image[i + ALPHA];
            image[i + RED] = alpha;
            image[i + GREEN] = image[i + RED];
            image[i + BLUE] = image[i + GREEN];
            image[i + ALPHA] = image[i + BLUE];
        }
    }

    /**
     * Convert an image with 32-bits for the red, green, blue and alpha channels
     * to one where the channels each take 5-bits in a 16-bit word.
     * @param imgWidth the width of the image in pixels.
     * @param imgHeight the height of the image in pixels.
     * @param img the image data.
     * @return the image data with the red, green and blue channels packed into
     * 16-bit words. Alpha is discarded.
     */
    public byte[] packColors(final int imgWidth, final int imgHeight, final byte[] img) {
        int src = 0;
        int dst = 0;
        int row;
        int col;
        final int scan = imgWidth + (imgWidth & 1);
        final byte[] formattedImage = new byte[scan * imgHeight * 2];
        for (row = 0; row < imgHeight; row++) {
            for (col = 0; col < imgWidth; col++, src++) {
                final int red = (img[src++] & RGB5_MSB_MASK) << R5_SHIFT;
                final int green = (img[src++] & RGB5_MSB_MASK) << G5_SHIFT;
                final int blue = (img[src++] & RGB5_MSB_MASK) >> B5_SHIFT;
                final int colour = (red | green | blue) & Coder.LOWEST15;
                formattedImage[dst++] = (byte) (colour >> 8);
                formattedImage[dst++] = (byte) colour;
            }
            while (col < scan) {
                formattedImage[dst++] = 0;
                formattedImage[dst++] = 0;
                col++;
            }
        }
        return formattedImage;
    }

    /**
     * Adjust the width of each row in an image so the data is aligned to a
     * 16-bit word boundary when loaded in memory. The additional bytes are
     * all set to zero and will not be displayed in the image.
     *
     * @param imgWidth the width of the image in pixels.
     * @param imgHeight the height of the image in pixels.
     * @param img the image data.
     * @return the image data with each row aligned to a 16-bit boundary.
     */
    public byte[] adjustScan(final int imgWidth, final int imgHeight, final byte[] img) {
        int src = 0;
        int dst = 0;
        int row;
        int col;
        int scan = 0;
        byte[] formattedImage = null;
        scan = (imgWidth + 3) & ~3;
        formattedImage = new byte[scan * imgHeight];
        for (row = 0; row < imgHeight; row++) {
            for (col = 0; col < imgWidth; col++) {
                formattedImage[dst++] = img[src++];
            }
            while (col++ < scan) {
                formattedImage[dst++] = 0;
            }
        }
        return formattedImage;
    }

    /**
     * Reorder the image pixels from RGBA to ARGB.
     *
     * @param img the image data.
     */
    public void orderAlpha(final byte[] img) {
        byte alpha;
        for (int i = 0; i < img.length; i += RGBA_CHANNELS) {
            alpha = img[i + ALPHA];
            img[i + ALPHA] = img[i + BLUE];
            img[i + BLUE] = img[i + GREEN];
            img[i + GREEN] = img[i + RED];
            img[i + RED] = alpha;
        }
    }

    /**
     * Reorder the image pixels from RGBA to ABGR.
     *
     * @param img the image data.
     */
    public void orderABGR(final byte[] img) {
        byte swap;
        for (int i = 0; i < img.length; i += RGBA_CHANNELS) {
            swap = img[i + ALPHA];
            img[i + ALPHA] = img[i + RED];
            img[i + RED] = swap;
            swap = img[i + BLUE];
            img[i + BLUE] = img[i + GREEN];
            img[i + GREEN] = swap;
        }
    }

    /**
     * Apply the level for the alpha channel to the red, green and blue colour
     * channels for encoding the image so it can be added to a Flash movie.
     * @param img the image data.
     */
    public void applyAlpha(final byte[] img) {
        int alpha;
        for (int i = 0; i < img.length; i += RGBA_CHANNELS) {
            alpha = img[i + ALPHA] & OPAQUE;
            img[i + RED] = (byte) (((img[i + RED] & OPAQUE) * alpha) / OPAQUE);
            img[i + GREEN] = (byte) (((img[i + GREEN] & OPAQUE) * alpha) / OPAQUE);
            img[i + BLUE] = (byte) (((img[i + BLUE] & OPAQUE) * alpha) / OPAQUE);
        }
    }

    /**
    * Concatenate the colour table and the image data together.
    * @param img the image data.
    * @param colors the colour table.
    * @return a single array containing the red, green and blue (not alpha)
    * entries from the colour table followed by the red, green, blue and
    * alpha channels from the image. The alpha defaults to 255 for an opaque
    * image.
    */
    public byte[] merge(final byte[] img, final byte[] colors) {
        final int entries = colors.length / RGBA_CHANNELS;
        final byte[] merged = new byte[entries * (RGBA_CHANNELS - 1) + img.length];
        int dst = 0;
        for (int i = 0; i < colors.length; i += RGBA_CHANNELS) {
            merged[dst++] = colors[i + BLUE];
            merged[dst++] = colors[i + GREEN];
            merged[dst++] = colors[i + RED];
        }
        for (final byte element : img) {
            merged[dst++] = element;
        }
        return merged;
    }

    /**
     * Concatenate the colour table and the image data together.
     * @param img the image data.
     * @param colors the colour table.
     * @return a single array containing entries from the colour table followed
     * by the image.
     */
    public byte[] mergeAlpha(final byte[] img, final byte[] colors) {
        final byte[] merged = new byte[colors.length + img.length];
        int dst = 0;
        for (final byte element : colors) {
            merged[dst++] = element;
        }
        for (final byte element : img) {
            merged[dst++] = element;
        }
        return merged;
    }
}
