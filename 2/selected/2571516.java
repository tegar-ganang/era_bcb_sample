package com.flagstone.transform.util.image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import com.flagstone.transform.coder.BigDecoder;
import com.flagstone.transform.coder.Coder;
import com.flagstone.transform.image.DefineImage;
import com.flagstone.transform.image.DefineImage2;
import com.flagstone.transform.image.ImageFormat;
import com.flagstone.transform.image.ImageTag;

/**
 * PNGDecoder decodes Portable Network Graphics (PNG) format images so they can
 * be used in a Flash file.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class PNGDecoder implements ImageProvider, ImageDecoder {

    /** Alpha channel value for opaque colours. */
    private static final int OPAQUE = 255;

    /** Mask for reading unsigned 8-bit values. */
    private static final int UNSIGNED_BYTE = 255;

    /** Size of each colour table entry or pixel in a true colour image. */
    private static final int RGBA_CHANNELS = 4;

    /** Size of each colour table entry or pixel in a true colour image. */
    private static final int RGB_CHANNELS = 3;

    /** Size of a pixel in a RGB555 true colour image. */
    private static final int RGB5_SIZE = 16;

    /** Size of a pixel in a RGB8 true colour image. */
    private static final int RGB8_SIZE = 24;

    /** Byte offset to red channel. */
    private static final int RED = 0;

    /** Byte offset to red channel. */
    private static final int GREEN = 1;

    /** Byte offset to blue channel. */
    private static final int BLUE = 2;

    /** Byte offset to alpha channel. */
    private static final int ALPHA = 3;

    private static final int DEPTH_1 = 1;

    private static final int DEPTH_2 = 2;

    private static final int DEPTH_4 = 4;

    private static final int DEPTH_8 = 8;

    private static final int DEPTH_16 = 16;

    /** Message used to signal that the image cannot be decoded. */
    private static final String BAD_FORMAT = "Unsupported format";

    /** Table for mapping monochrome images onto a colour palette. */
    private static final int[] MONOCHROME = { 0, 255 };

    /** Table for mapping 2-level grey-scale images onto a colour palette. */
    private static final int[] GREYCSALE2 = { 0, 85, 170, 255 };

    /** Table for mapping 4-level grey-scale images onto a colour palette. */
    private static final int[] GREYCSALE4 = { 0, 17, 34, 51, 68, 85, 102, 119, 136, 153, 170, 187, 204, 221, 238, 255 };

    /** signature identifying a PNG format image. */
    private static final int[] SIGNATURE = { 137, 80, 78, 71, 13, 10, 26, 10 };

    /** signature identifying a header block. */
    private static final int IHDR = 0x49484452;

    /** signature identifying a colour palette block. */
    private static final int PLTE = 0x504c5445;

    /** signature identifying an image data block. */
    private static final int IDAT = 0x49444154;

    /** signature identifying an end block. */
    private static final int IEND = 0x49454e44;

    /** signature identifying a transparency block. */
    private static final int TRNS = 0x74524e53;

    /** colorType value for grey-scale images. */
    private static final int GREYSCALE = 0;

    /** colorType value for true-colour images. */
    private static final int TRUE_COLOUR = 2;

    /** colorType value for indexed colour images. */
    private static final int INDEXED_COLOUR = 3;

    /** colorType value for grey-scale images with transparency. */
    private static final int ALPHA_GREYSCALE = 4;

    /** colorType value for true-colour images with transparency. */
    private static final int ALPHA_TRUECOLOUR = 6;

    /** filterMethod value for images with sub-pixel filtering. */
    private static final int SUB_FILTER = 1;

    /** filterMethod value for images with upper filtering. */
    private static final int UP_FILTER = 2;

    /** filterMethod value for images with average filtering. */
    private static final int AVG_FILTER = 3;

    /** filterMethod value for images with Paeth filtering. */
    private static final int PAETH_FILTER = 4;

    /** starting row for each image block. */
    private static final int[] START_ROW = { 0, 0, 4, 0, 2, 0, 1 };

    /** starting column for each image block. */
    private static final int[] START_COLUMN = { 0, 4, 0, 2, 0, 1, 0 };

    /** row increment for each image block. */
    private static final int[] ROW_STEP = { 8, 8, 8, 4, 4, 2, 2 };

    /** column increment for each image block. */
    private static final int[] COLUMN_STEP = { 8, 8, 4, 4, 2, 2, 1 };

    /** The number of bits used to represent each colour component. */
    private transient int bitDepth;

    /** The number of colour components in each pixel. */
    private transient int colorComponents;

    /** The method used to encode colours in the image. */
    private transient int colorType;

    /** Row interlacing method used in the image. */
    private transient int interlaceMethod;

    /** Default value for transparent grey-scale pixels. */
    private transient int transparentGrey;

    /** Default value for transparent red pixels. */
    private transient int transparentRed;

    /** Binary data taken directly from encoded image. */
    private transient byte[] chunkData = new byte[0];

    /** The format of the decoded image. */
    private transient ImageFormat format;

    /** The width of the image in pixels. */
    private transient int width;

    /** The height of the image in pixels. */
    private transient int height;

    /** The colour table for indexed images. */
    private transient byte[] table;

    /** The image data. */
    private transient byte[] image;

    /** {@inheritDoc} */
    public ImageDecoder newDecoder() {
        return new PNGDecoder();
    }

    /** {@inheritDoc} */
    public void read(final File file) throws IOException, DataFormatException {
        final ImageInfo info = new ImageInfo();
        info.setInput(new RandomAccessFile(file, "r"));
        if (!info.check()) {
            throw new DataFormatException(BAD_FORMAT);
        }
        read(new FileInputStream(file));
    }

    /** {@inheritDoc} */
    public void read(final URL url) throws IOException, DataFormatException {
        final URLConnection connection = url.openConnection();
        final int length = connection.getContentLength();
        if (length < 0) {
            throw new FileNotFoundException(url.getFile());
        }
        read(url.openStream());
    }

    /** {@inheritDoc} */
    public ImageTag defineImage(final int identifier) {
        ImageTag object = null;
        final ImageFilter filter = new ImageFilter();
        switch(format) {
            case IDX8:
                object = new DefineImage(identifier, width, height, table.length / RGBA_CHANNELS, zip(filter.merge(filter.adjustScan(width, height, image), table)));
                break;
            case IDXA:
                object = new DefineImage2(identifier, width, height, table.length / RGBA_CHANNELS, zip(filter.mergeAlpha(filter.adjustScan(width, height, image), table)));
                break;
            case RGB5:
                object = new DefineImage(identifier, width, height, zip(filter.packColors(width, height, image)), RGB5_SIZE);
                break;
            case RGB8:
                filter.orderAlpha(image);
                object = new DefineImage(identifier, width, height, zip(image), RGB8_SIZE);
                break;
            case RGBA:
                applyAlpha(image);
                object = new DefineImage2(identifier, width, height, zip(image));
                break;
            default:
                throw new AssertionError(BAD_FORMAT);
        }
        return object;
    }

    /**
     * Apply the level for the alpha channel to the red, green and blue colour
     * channels for encoding the image so it can be added to a Flash movie.
     * @param img the image data.
     */
    private void applyAlpha(final byte[] img) {
        int alpha;
        for (int i = 0; i < img.length; i += RGBA_CHANNELS) {
            alpha = img[i + ALPHA] & UNSIGNED_BYTE;
            img[i + ALPHA] = (byte) (((img[i + BLUE] & UNSIGNED_BYTE) * alpha) / OPAQUE);
            img[i + BLUE] = (byte) (((img[i + GREEN] & UNSIGNED_BYTE) * alpha) / OPAQUE);
            img[i + GREEN] = (byte) (((img[i + RED] & UNSIGNED_BYTE) * alpha) / OPAQUE);
            img[i + RED] = (byte) alpha;
        }
    }

    /** {@inheritDoc} */
    public void read(final InputStream stream) throws DataFormatException, IOException {
        final BigDecoder coder = new BigDecoder(stream);
        int length = 0;
        int chunkType = 0;
        boolean moreChunks = true;
        chunkData = new byte[0];
        transparentGrey = -1;
        transparentRed = -1;
        for (int i = 0; i < 8; i++) {
            if (coder.readByte() != SIGNATURE[i]) {
                throw new DataFormatException(BAD_FORMAT);
            }
        }
        while (moreChunks) {
            length = coder.readInt();
            chunkType = coder.readInt();
            coder.mark();
            switch(chunkType) {
                case IHDR:
                    decodeIHDR(coder);
                    break;
                case PLTE:
                    decodePLTE(coder, length);
                    break;
                case TRNS:
                    decodeTRNS(coder, length);
                    break;
                case IDAT:
                    decodeIDAT(coder, length);
                    break;
                case IEND:
                    moreChunks = false;
                    coder.skip(length + 4);
                    break;
                default:
                    coder.skip(length + 4);
                    break;
            }
        }
        decodeImage();
    }

    /**
     * Decode the header, IHDR, block from a PNG image.
     * @param coder the decoder containing the image data.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException is the image contains an unsupported format.
     */
    private void decodeIHDR(final BigDecoder coder) throws IOException, DataFormatException {
        width = coder.readInt();
        height = coder.readInt();
        bitDepth = coder.readByte();
        colorType = coder.readByte();
        coder.readByte();
        coder.readByte();
        interlaceMethod = coder.readByte();
        coder.readInt();
        decodeFormat();
    }

    private void decodeFormat() throws DataFormatException {
        switch(colorType) {
            case GREYSCALE:
                format = ImageFormat.RGB8;
                colorComponents = 1;
                break;
            case TRUE_COLOUR:
                format = ImageFormat.RGB8;
                colorComponents = RGB_CHANNELS;
                break;
            case INDEXED_COLOUR:
                format = ImageFormat.IDX8;
                colorComponents = 1;
                break;
            case ALPHA_GREYSCALE:
                format = ImageFormat.RGBA;
                colorComponents = 2;
                break;
            case ALPHA_TRUECOLOUR:
                format = ImageFormat.RGBA;
                colorComponents = RGBA_CHANNELS;
                break;
            default:
                throw new DataFormatException(BAD_FORMAT);
        }
        if (format == ImageFormat.RGB8 && bitDepth <= 5) {
            format = ImageFormat.RGB5;
        }
    }

    /**
     * Decode the colour palette, PLTE, block from a PNG image.
     * @param coder the decoder containing the image data.
     * @param length the length of the block in bytes.
     * @throws IOException if there is an error decoding the data.
     */
    private void decodePLTE(final BigDecoder coder, final int length) throws IOException {
        if (colorType == RGB_CHANNELS) {
            final int paletteSize = length / RGB_CHANNELS;
            int index = 0;
            table = new byte[paletteSize * RGBA_CHANNELS];
            for (int i = 0; i < paletteSize; i++, index += RGBA_CHANNELS) {
                table[index + ALPHA] = (byte) OPAQUE;
                table[index + BLUE] = (byte) coder.readByte();
                table[index + GREEN] = (byte) coder.readByte();
                table[index + RED] = (byte) coder.readByte();
            }
        } else {
            coder.skip(length);
        }
        coder.readInt();
    }

    /**
     * Decode the transparency, TRNS, block from a PNG image.
     * @param coder the decoder containing the image data.
     * @param length the length of the block in bytes.
     * @throws IOException if there is an error decoding the data.
     */
    private void decodeTRNS(final BigDecoder coder, final int length) throws IOException {
        int index = 0;
        switch(colorType) {
            case GREYSCALE:
                transparentGrey = coder.readUnsignedShort();
                format = ImageFormat.RGBA;
                break;
            case TRUE_COLOUR:
                transparentRed = coder.readUnsignedShort();
                format = ImageFormat.RGBA;
                coder.readUnsignedShort();
                coder.readUnsignedShort();
                break;
            case INDEXED_COLOUR:
                format = ImageFormat.IDXA;
                for (int i = 0; i < length; i++, index += RGBA_CHANNELS) {
                    table[index + ALPHA] = (byte) coder.readByte();
                    if (table[index + ALPHA] == 0) {
                        table[index + RED] = 0;
                        table[index + GREEN] = 0;
                        table[index + BLUE] = 0;
                    }
                }
                break;
            default:
                break;
        }
        coder.readInt();
    }

    /**
     * Decode the image data, IDAT, block from a PNG image.
     * @param coder the decoder containing the image data.
     * @param length the length of the block in bytes.
     * @throws IOException if there is an error decoding the data.
     */
    private void decodeIDAT(final BigDecoder coder, final int length) throws IOException {
        final int currentLength = chunkData.length;
        final int newLength = currentLength + length;
        final byte[] data = new byte[newLength];
        System.arraycopy(chunkData, 0, data, 0, currentLength);
        for (int i = currentLength; i < newLength; i++) {
            data[i] = (byte) coder.readByte();
        }
        chunkData = data;
        coder.readInt();
    }

    /**
     * Decode a PNG encoded image.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the image cannot be decoded.
     */
    private void decodeImage() throws IOException, DataFormatException {
        if ((format == ImageFormat.IDX8) || (format == ImageFormat.IDXA)) {
            image = new byte[height * width];
        } else {
            image = new byte[height * width * RGBA_CHANNELS];
        }
        if (interlaceMethod == 1) {
            decodeInterlaced();
        } else {
            decodeProgressive();
        }
    }

    private void decodeInterlaced() throws IOException, DataFormatException {
        final byte[] encodedImage = unzip(chunkData);
        final int bitsPerPixel = bitDepth * colorComponents;
        final int bitsPerRow = width * bitsPerPixel;
        final int rowWidth = (bitsPerRow + 7) >> 3;
        final int bytesPerPixel = (bitsPerPixel < 8) ? 1 : bitsPerPixel / 8;
        final byte[] current = new byte[rowWidth];
        final byte[] previous = new byte[rowWidth];
        for (int i = 0; i < rowWidth; i++) {
            previous[i] = (byte) 0;
        }
        int imageIndex = 0;
        int row = 0;
        int col = 0;
        int filter = 0;
        int scanBits = 0;
        int scanLength = 0;
        for (int pass = 0; pass < 7; pass++) {
            for (row = START_ROW[pass]; (row < height) && (imageIndex < encodedImage.length); row += ROW_STEP[pass]) {
                for (col = START_COLUMN[pass], scanBits = 0; col < width; col += COLUMN_STEP[pass]) {
                    scanBits += bitsPerPixel;
                }
                scanLength = (scanBits + 7) >> 3;
                filter = encodedImage[imageIndex++];
                for (int i = 0; i < scanLength; i++, imageIndex++) {
                    current[i] = (imageIndex < encodedImage.length) ? encodedImage[imageIndex] : previous[i];
                }
                defilter(filter, bytesPerPixel, scanLength, current, previous);
                deblock(row, current, START_COLUMN[pass], COLUMN_STEP[pass]);
                System.arraycopy(current, 0, previous, 0, scanLength);
            }
        }
    }

    private void decodeProgressive() throws IOException, DataFormatException {
        final byte[] data = unzip(chunkData);
        final int bitsPerPixel = bitDepth * colorComponents;
        final int bitsPerRow = width * bitsPerPixel;
        final int rowWidth = (bitsPerRow + 7) >> 3;
        final int bytesPerPixel = (bitsPerPixel < 8) ? 1 : bitsPerPixel / 8;
        final byte[] current = new byte[rowWidth];
        final byte[] previous = new byte[rowWidth];
        for (int i = 0; i < rowWidth; i++) {
            previous[i] = (byte) 0;
        }
        int index = 0;
        int row = 0;
        int col = 0;
        int filter = 0;
        int scanBits = 0;
        int scanLength = 0;
        for (row = 0; (row < height) && (index < data.length); row++) {
            for (col = 0, scanBits = 0; col < width; col++) {
                scanBits += bitsPerPixel;
            }
            scanLength = (scanBits + 7) >> 3;
            filter = data[index++];
            for (int i = 0; i < scanLength; i++, index++) {
                current[i] = (index < data.length) ? data[index] : previous[i];
            }
            defilter(filter, bytesPerPixel, scanLength, current, previous);
            deblock(row, current, 0, 1);
            System.arraycopy(current, 0, previous, 0, scanLength);
        }
    }

    private void defilter(final int filter, final int size, final int scan, final byte[] current, final byte[] previous) {
        switch(filter) {
            case SUB_FILTER:
                subFilter(size, scan, current);
                break;
            case UP_FILTER:
                upFilter(scan, current, previous);
                break;
            case AVG_FILTER:
                averageFilter(size, scan, current, previous);
                break;
            case PAETH_FILTER:
                paethFilter(size, scan, current, previous);
                break;
            default:
                break;
        }
    }

    private void subFilter(final int start, final int count, final byte[] current) {
        for (int i = start, j = 0; i < count; i++, j++) {
            current[i] = (byte) (current[i] + current[j]);
        }
    }

    private void upFilter(final int count, final byte[] current, final byte[] previous) {
        for (int i = 0; i < count; i++) {
            current[i] = (byte) (current[i] + previous[i]);
        }
    }

    private void averageFilter(final int start, final int count, final byte[] current, final byte[] previous) {
        for (int cindex = 0; cindex < start; cindex++) {
            current[cindex] = (byte) (current[cindex] + (0 + (UNSIGNED_BYTE & previous[cindex])) / 2);
        }
        for (int cindex = start, pindex = 0; cindex < count; cindex++, pindex++) {
            current[cindex] = (byte) (current[cindex] + ((UNSIGNED_BYTE & current[pindex]) + (UNSIGNED_BYTE & previous[cindex])) / 2);
        }
    }

    private void paethFilter(final int start, final int count, final byte[] current, final byte[] previous) {
        for (int cindex = 0; cindex < start; cindex++) {
            current[cindex] = (byte) (current[cindex] + paeth((byte) 0, previous[cindex], (byte) 0));
        }
        for (int cindex = start, pindex = 0; cindex < count; cindex++, pindex++) {
            current[cindex] = (byte) (current[cindex] + paeth(current[pindex], previous[cindex], previous[pindex]));
        }
    }

    /**
     * Decode a Paeth encoded pixel.
     * @param lower the current pixel.
     * @param upper the pixel on the previous row.
     * @param next the next pixel in current row.
     * @return the decoded value.
     */
    private int paeth(final byte lower, final byte upper, final byte next) {
        final int left = UNSIGNED_BYTE & lower;
        final int above = UNSIGNED_BYTE & upper;
        final int upperLeft = UNSIGNED_BYTE & next;
        final int estimate = left + above - upperLeft;
        int distLeft = estimate - left;
        if (distLeft < 0) {
            distLeft = -distLeft;
        }
        int distAbove = estimate - above;
        if (distAbove < 0) {
            distAbove = -distAbove;
        }
        int distUpperLeft = estimate - upperLeft;
        if (distUpperLeft < 0) {
            distUpperLeft = -distUpperLeft;
        }
        final int value;
        if ((distLeft <= distAbove) && (distLeft <= distUpperLeft)) {
            value = left;
        } else if (distAbove <= distUpperLeft) {
            value = above;
        } else {
            value = upperLeft;
        }
        return value;
    }

    private void deblock(final int row, final byte[] current, final int start, final int inc) throws IOException, DataFormatException {
        final ByteArrayInputStream stream = new ByteArrayInputStream(current);
        final BigDecoder coder = new BigDecoder(stream);
        for (int col = start; col < width; col += inc) {
            switch(colorType) {
                case GREYSCALE:
                    decodeGreyscale(coder, row, col);
                    break;
                case TRUE_COLOUR:
                    decodeTrueColour(coder, row, col);
                    break;
                case INDEXED_COLOUR:
                    decodeIndexedColour(coder, row, col);
                    break;
                case ALPHA_GREYSCALE:
                    decodeAlphaGreyscale(coder, row, col);
                    break;
                case ALPHA_TRUECOLOUR:
                    decodeAlphaTrueColour(coder, row, col);
                    break;
                default:
                    throw new DataFormatException(BAD_FORMAT);
            }
        }
    }

    /**
     * Decode a grey-scale pixel with no transparency.
     * @param coder the decode containing the image data.
     * @param row the row number of the pixel in the image.
     * @param col the column number of the pixel in the image.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the pixel data cannot be decoded.
     */
    private void decodeGreyscale(final BigDecoder coder, final int row, final int col) throws IOException, DataFormatException {
        int pixel = 0;
        byte colour = 0;
        switch(bitDepth) {
            case DEPTH_1:
                pixel = coder.readBits(bitDepth, false);
                colour = (byte) MONOCHROME[pixel];
                break;
            case DEPTH_2:
                pixel = coder.readBits(bitDepth, false);
                colour = (byte) GREYCSALE2[pixel];
                break;
            case DEPTH_4:
                pixel = coder.readBits(bitDepth, false);
                colour = (byte) GREYCSALE4[pixel];
                break;
            case DEPTH_8:
                pixel = coder.readByte();
                colour = (byte) pixel;
                break;
            case DEPTH_16:
                pixel = coder.readUnsignedShort();
                colour = (byte) (pixel >> Coder.TO_LOWER_BYTE);
                break;
            default:
                throw new DataFormatException(BAD_FORMAT);
        }
        int index = row * (width << 2) + (col << 2);
        image[index++] = colour;
        image[index++] = colour;
        image[index++] = colour;
        image[index++] = (byte) transparentGrey;
    }

    /**
     * Decode a true colour pixel with no transparency.
     * @param coder the decode containing the image data.
     * @param row the row number of the pixel in the image.
     * @param col the column number of the pixel in the image.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the pixel data cannot be decoded.
     */
    private void decodeTrueColour(final BigDecoder coder, final int row, final int col) throws IOException, DataFormatException {
        int pixel = 0;
        byte colour = 0;
        final int index = row * (width << 2) + (col << 2);
        for (int i = 0; i < colorComponents; i++) {
            if (bitDepth == DEPTH_8) {
                pixel = coder.readByte();
                colour = (byte) pixel;
            } else if (bitDepth == DEPTH_16) {
                pixel = coder.readUnsignedShort();
                colour = (byte) (pixel >> Coder.TO_LOWER_BYTE);
            } else {
                throw new DataFormatException(BAD_FORMAT);
            }
            image[index + i] = colour;
        }
        image[index + ALPHA] = (byte) transparentRed;
    }

    /**
     * Decode an index colour pixel.
     * @param coder the decode containing the image data.
     * @param row the row number of the pixel in the image.
     * @param col the column number of the pixel in the image.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the pixel data cannot be decoded.
     */
    private void decodeIndexedColour(final BigDecoder coder, final int row, final int col) throws IOException, DataFormatException {
        int index = 0;
        switch(bitDepth) {
            case DEPTH_1:
                index = coder.readBits(bitDepth, false);
                break;
            case DEPTH_2:
                index = coder.readBits(bitDepth, false);
                break;
            case DEPTH_4:
                index = coder.readBits(bitDepth, false);
                break;
            case DEPTH_8:
                index = coder.readByte();
                break;
            case DEPTH_16:
                index = coder.readUnsignedShort();
                break;
            default:
                throw new DataFormatException(BAD_FORMAT);
        }
        image[row * width + col] = (byte) index;
    }

    /**
     * Decode a grey-scale pixel with transparency.
     * @param coder the decode containing the image data.
     * @param row the row number of the pixel in the image.
     * @param col the column number of the pixel in the image.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the pixel data cannot be decoded.
     */
    private void decodeAlphaGreyscale(final BigDecoder coder, final int row, final int col) throws IOException, DataFormatException {
        int pixel = 0;
        byte colour = 0;
        int alpha = 0;
        switch(bitDepth) {
            case DEPTH_1:
                pixel = coder.readBits(bitDepth, false);
                colour = (byte) MONOCHROME[pixel];
                alpha = coder.readBits(bitDepth, false);
                break;
            case DEPTH_2:
                pixel = coder.readBits(bitDepth, false);
                colour = (byte) GREYCSALE2[pixel];
                alpha = coder.readBits(bitDepth, false);
                break;
            case DEPTH_4:
                pixel = coder.readBits(bitDepth, false);
                colour = (byte) GREYCSALE4[pixel];
                alpha = coder.readBits(bitDepth, false);
                break;
            case DEPTH_8:
                pixel = coder.readByte();
                colour = (byte) pixel;
                alpha = coder.readByte();
                break;
            case DEPTH_16:
                pixel = coder.readUnsignedShort();
                colour = (byte) (pixel >> Coder.TO_LOWER_BYTE);
                alpha = coder.readUnsignedShort() >> Coder.TO_LOWER_BYTE;
                break;
            default:
                throw new DataFormatException(BAD_FORMAT);
        }
        int index = row * (width << 2) + (col << 2);
        image[index++] = colour;
        image[index++] = colour;
        image[index++] = colour;
        image[index] = (byte) alpha;
    }

    /**
     * Decode a true colour pixel with transparency.
     * @param coder the decode containing the image data.
     * @param row the row number of the pixel in the image.
     * @param col the column number of the pixel in the image.
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the pixel data cannot be decoded.
     */
    private void decodeAlphaTrueColour(final BigDecoder coder, final int row, final int col) throws IOException, DataFormatException {
        int pixel = 0;
        byte colour = 0;
        final int index = row * (width << 2) + (col << 2);
        for (int i = 0; i < colorComponents; i++) {
            if (bitDepth == DEPTH_8) {
                pixel = coder.readByte();
                colour = (byte) pixel;
            } else if (bitDepth == DEPTH_16) {
                pixel = coder.readUnsignedShort();
                colour = (byte) (pixel >> Coder.TO_LOWER_BYTE);
            } else {
                throw new DataFormatException(BAD_FORMAT);
            }
            image[index + i] = colour;
        }
    }

    /**
     * Uncompress the image using the ZIP format.
     * @param bytes the compressed image data.
     * @return the uncompressed image.
     * @throws DataFormatException if the compressed image is not in the ZIP
     * format or cannot be uncompressed.
     */
    private byte[] unzip(final byte[] bytes) throws DataFormatException {
        final byte[] data = new byte[width * height * 8];
        int count = 0;
        final Inflater inflater = new Inflater();
        inflater.setInput(bytes);
        count = inflater.inflate(data);
        final byte[] uncompressedData = new byte[count];
        System.arraycopy(data, 0, uncompressedData, 0, count);
        return uncompressedData;
    }

    /**
     * Compress the image using the ZIP format.
     * @param img the image data.
     * @return the compressed image.
     */
    private byte[] zip(final byte[] img) {
        final Deflater deflater = new Deflater();
        deflater.setInput(img);
        deflater.finish();
        final byte[] compressedData = new byte[img.length * 2];
        final int bytesCompressed = deflater.deflate(compressedData);
        final byte[] newData = Arrays.copyOf(compressedData, bytesCompressed);
        return newData;
    }

    /** {@inheritDoc} */
    public int getWidth() {
        return width;
    }

    /** {@inheritDoc} */
    public int getHeight() {
        return height;
    }

    /** {@inheritDoc} */
    public byte[] getImage() {
        return Arrays.copyOf(image, image.length);
    }
}
