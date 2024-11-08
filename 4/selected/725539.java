package ij.plugin.filter;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.Animator;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
This plugin saves stacks in AVI format.
Supported formats:
Uncompressed 8-bit (gray or indexed color), 24-bit (RGB).
JPEG and PNG compression.
16-bit and 32-bit (float) images are converted to 8-bit.
The plugin is based on the FileAvi class written by William Gandler.
The FileAvi class is part of Matthew J. McAuliffe's MIPAV program,
available from http://mipav.cit.nih.gov/.
2008-06-05 Support for jpeg and png-compressed output and composite images by Michael Schmid.
*/
public class AVI_Writer implements PlugInFilter {

    public static final int NO_COMPRESSION = 0;

    public static final int JPEG_COMPRESSION = 0x47504a4d;

    public static final int PNG_COMPRESSION = 0x20676e70;

    private static final int FOURCC_00db = 0x62643030;

    private static final int FOURCC_00dc = 0x63643030;

    private static int compressionIndex = 2;

    private static int jpegQuality = 90;

    private static final String[] COMPRESSION_STRINGS = new String[] { "Uncompressed", "PNG", "JPEG" };

    private static final int[] COMPRESSION_TYPES = new int[] { NO_COMPRESSION, PNG_COMPRESSION, JPEG_COMPRESSION };

    private ImagePlus imp;

    private RandomAccessFile raFile;

    private int xDim, yDim;

    private int zDim;

    private int bytesPerPixel;

    private int frameDataSize;

    private int biCompression;

    private int linePad;

    private byte[] bufferWrite;

    private BufferedImage bufferedImage;

    private RaOutputStream raOutputStream;

    private long[] sizePointers = new long[5];

    private int stackPointer;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL + NO_CHANGES;
    }

    /** Asks for the compression type and filename; then saves as AVI file */
    public void run(ImageProcessor ip) {
        if (!showDialog(imp)) return;
        SaveDialog sd = new SaveDialog("Save as AVI...", imp.getTitle(), ".avi");
        String fileName = sd.getFileName();
        if (fileName == null) return;
        String fileDir = sd.getDirectory();
        FileInfo fi = imp.getOriginalFileInfo();
        if (imp.getStack().isVirtual() && fileDir.equals(fi.directory) && fileName.equals(fi.fileName)) {
            IJ.error("AVI Writer", "Virtual stacks cannot be saved in place.");
            return;
        }
        try {
            writeImage(imp, fileDir + fileName, COMPRESSION_TYPES[compressionIndex], jpegQuality);
            IJ.showStatus("");
        } catch (IOException e) {
            IJ.error("AVI Writer", "An error occured writing the file.\n \n" + e);
        }
        IJ.showStatus("");
    }

    private boolean showDialog(ImagePlus imp) {
        String options = Macro.getOptions();
        if (options != null && options.indexOf("compression=") == -1) Macro.setOptions("compression=Uncompressed " + options);
        double fps = getFrameRate(imp);
        int decimalPlaces = (int) fps == fps ? 0 : 1;
        GenericDialog gd = new GenericDialog("Save as AVI...");
        gd.addChoice("Compression:", COMPRESSION_STRINGS, COMPRESSION_STRINGS[compressionIndex]);
        gd.addNumericField("Frame Rate:", fps, decimalPlaces, 3, "fps");
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        compressionIndex = gd.getNextChoiceIndex();
        fps = gd.getNextNumber();
        if (fps <= 0.5) fps = 0.5;
        imp.getCalibration().fps = fps;
        return true;
    }

    /** Writes an ImagePlus (stack) as AVI file. */
    public void writeImage(ImagePlus imp, String path, int compression, int jpegQuality) throws IOException {
        if (compression != NO_COMPRESSION && compression != JPEG_COMPRESSION && compression != PNG_COMPRESSION) throw new IllegalArgumentException("Unsupported Compression 0x" + Integer.toHexString(compression));
        this.biCompression = compression;
        if (jpegQuality < 0) jpegQuality = 0;
        if (jpegQuality > 100) jpegQuality = 100;
        this.jpegQuality = jpegQuality;
        File file = new File(path);
        raFile = new RandomAccessFile(file, "rw");
        raFile.setLength(0);
        imp.startTiming();
        boolean isComposite = imp.isComposite();
        boolean isHyperstack = imp.isHyperStack();
        boolean isOverlay = imp.getOverlay() != null && !imp.getHideOverlay();
        xDim = imp.getWidth();
        yDim = imp.getHeight();
        zDim = imp.getStackSize();
        boolean saveFrames = false, saveSlices = false, saveChannels = false;
        int channels = imp.getNChannels();
        int slices = imp.getNSlices();
        int frames = imp.getNFrames();
        int channel = imp.getChannel();
        int slice = imp.getSlice();
        int frame = imp.getFrame();
        if (isHyperstack || isComposite) {
            if (frames > 1) {
                saveFrames = true;
                zDim = frames;
            } else if (slices > 1) {
                saveSlices = true;
                zDim = slices;
            } else if (channels > 1) {
                saveChannels = true;
                zDim = channels;
            } else isHyperstack = false;
        }
        if (imp.getType() == ImagePlus.COLOR_RGB || isComposite || biCompression == JPEG_COMPRESSION || isOverlay) bytesPerPixel = 3; else bytesPerPixel = 1;
        boolean writeLUT = bytesPerPixel == 1;
        linePad = 0;
        int minLineLength = bytesPerPixel * xDim;
        if (biCompression == NO_COMPRESSION && minLineLength % 4 != 0) linePad = 4 - minLineLength % 4;
        frameDataSize = (bytesPerPixel * xDim + linePad) * yDim;
        int microSecPerFrame = (int) Math.round((1.0 / getFrameRate(imp)) * 1.0e6);
        writeString("RIFF");
        chunkSizeHere();
        writeString("AVI ");
        writeString("LIST");
        chunkSizeHere();
        writeString("hdrl");
        writeString("avih");
        writeInt(0x38);
        writeInt(microSecPerFrame);
        writeInt(0);
        writeInt(0);
        writeInt(0x10);
        writeInt(zDim);
        writeInt(0);
        writeInt(1);
        writeInt(0);
        writeInt(xDim);
        writeInt(yDim);
        writeInt(0);
        writeInt(0);
        writeInt(0);
        writeInt(0);
        writeString("LIST");
        chunkSizeHere();
        writeString("strl");
        writeString("strh");
        writeInt(56);
        writeString("vids");
        writeString("DIB ");
        writeInt(0);
        writeInt(0);
        writeInt(0);
        writeInt(1);
        writeInt((int) Math.round(getFrameRate(imp)));
        writeInt(0);
        writeInt(zDim);
        writeInt(0);
        writeInt(-1);
        writeInt(0);
        writeShort((short) 0);
        writeShort((short) 0);
        writeShort((short) 0);
        writeShort((short) 0);
        writeString("strf");
        chunkSizeHere();
        writeInt(40);
        writeInt(xDim);
        writeInt(yDim);
        writeShort(1);
        writeShort((short) (8 * bytesPerPixel));
        writeInt(biCompression);
        int biSizeImage = (biCompression == NO_COMPRESSION) ? 0 : xDim * yDim * bytesPerPixel;
        writeInt(biSizeImage);
        writeInt(0);
        writeInt(0);
        writeInt(writeLUT ? 256 : 0);
        writeInt(0);
        if (writeLUT) writeLUT(imp.getProcessor());
        chunkEndWriteSize();
        writeString("strn");
        writeInt(16);
        writeString("ImageJ AVI     \0");
        chunkEndWriteSize();
        chunkEndWriteSize();
        writeString("JUNK");
        chunkSizeHere();
        raFile.seek(4096);
        chunkEndWriteSize();
        writeString("LIST");
        chunkSizeHere();
        long moviPointer = raFile.getFilePointer();
        writeString("movi");
        if (biCompression == NO_COMPRESSION) bufferWrite = new byte[frameDataSize]; else raOutputStream = new RaOutputStream(raFile);
        int dataSignature = biCompression == NO_COMPRESSION ? FOURCC_00db : FOURCC_00dc;
        int maxChunkLength = 0;
        int[] dataChunkOffset = new int[zDim];
        int[] dataChunkLength = new int[zDim];
        for (int z = 0; z < zDim; z++) {
            IJ.showProgress(z, zDim);
            IJ.showStatus(z + "/" + zDim);
            ImageProcessor ip = null;
            if (isComposite || isHyperstack || isOverlay) {
                if (saveFrames) imp.setPositionWithoutUpdate(channel, slice, z + 1); else if (saveSlices) imp.setPositionWithoutUpdate(channel, z + 1, frame); else if (saveChannels) imp.setPositionWithoutUpdate(z + 1, slice, frame);
                ImagePlus imp2 = imp;
                if (isOverlay) {
                    if (!(saveFrames || saveSlices || saveChannels)) imp.setSliceWithoutUpdate(z + 1);
                    imp2 = imp.flatten();
                }
                ip = new ColorProcessor(imp2.getImage());
            } else ip = zDim == 1 ? imp.getProcessor() : imp.getStack().getProcessor(z + 1);
            int chunkPointer = (int) raFile.getFilePointer();
            writeInt(dataSignature);
            chunkSizeHere();
            if (biCompression == NO_COMPRESSION) {
                if (bytesPerPixel == 1) writeByteFrame(ip); else writeRGBFrame(ip);
            } else writeCompressedFrame(ip);
            dataChunkOffset[z] = (int) (chunkPointer - moviPointer);
            dataChunkLength[z] = (int) (raFile.getFilePointer() - chunkPointer - 8);
            if (maxChunkLength < dataChunkLength[z]) maxChunkLength = dataChunkLength[z];
            chunkEndWriteSize();
        }
        chunkEndWriteSize();
        if (isComposite || isHyperstack) imp.setPosition(channel, slice, frame);
        writeString("idx1");
        chunkSizeHere();
        for (int z = 0; z < zDim; z++) {
            writeInt(dataSignature);
            writeInt(0x10);
            writeInt(dataChunkOffset[z]);
            writeInt(dataChunkLength[z]);
        }
        chunkEndWriteSize();
        chunkEndWriteSize();
        raFile.close();
        IJ.showProgress(1.0);
    }

    /** Reserve space to write the size of chunk and remember the position
     *  for a later call to chunkEndWriteSize().
     *  Several levels of chunkSizeHere() and chunkEndWriteSize() may be nested.
     */
    private void chunkSizeHere() throws IOException {
        sizePointers[stackPointer] = raFile.getFilePointer();
        writeInt(0);
        stackPointer++;
    }

    /** At the end of a chunk, calculate its size and write it to the
     *  position remembered previously. Also pads to 2-byte boundaries.
     */
    private void chunkEndWriteSize() throws IOException {
        stackPointer--;
        long position = raFile.getFilePointer();
        raFile.seek(sizePointers[stackPointer]);
        writeInt((int) (position - (sizePointers[stackPointer] + 4)));
        raFile.seek(((position + 1) / 2) * 2);
    }

    /** Write Grayscale (or indexed color) data. Lines are padded to a length
     *  that is a multiple of 4 bytes. */
    private void writeByteFrame(ImageProcessor ip) throws IOException {
        ip = ip.convertToByte(true);
        byte[] pixels = (byte[]) ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        int c, offset, index = 0;
        for (int y = height - 1; y >= 0; y--) {
            offset = y * width;
            for (int x = 0; x < width; x++) bufferWrite[index++] = pixels[offset++];
            for (int i = 0; i < linePad; i++) bufferWrite[index++] = (byte) 0;
        }
        raFile.write(bufferWrite);
    }

    /** Write RGB data. Each 3-byte triplet in the bitmap array represents
     *  blue, green, and red, respectively, for a pixel.  The color bytes are
     *  in reverse order (Windows convention). Lines are padded to a length
     *  that is a multiple of 4 bytes. */
    private void writeRGBFrame(ImageProcessor ip) throws IOException {
        ip = ip.convertToRGB();
        int[] pixels = (int[]) ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        int c, offset, index = 0;
        for (int y = height - 1; y >= 0; y--) {
            offset = y * width;
            for (int x = 0; x < width; x++) {
                c = pixels[offset++];
                bufferWrite[index++] = (byte) (c & 0xff);
                bufferWrite[index++] = (byte) ((c & 0xff00) >> 8);
                bufferWrite[index++] = (byte) ((c & 0xff0000) >> 16);
            }
            for (int i = 0; i < linePad; i++) bufferWrite[index++] = (byte) 0;
        }
        raFile.write(bufferWrite);
    }

    /** Write a frame as jpeg- or png-compressed image */
    private void writeCompressedFrame(ImageProcessor ip) throws IOException {
        if (biCompression == JPEG_COMPRESSION) {
            BufferedImage bi = getBufferedImage(ip);
            ImageIO.write(bi, "jpeg", raOutputStream);
        } else {
            BufferedImage bi = ip.getBufferedImage();
            ImageIO.write(bi, "png", raOutputStream);
        }
    }

    private BufferedImage getBufferedImage(ImageProcessor ip) {
        BufferedImage bi = new BufferedImage(ip.getWidth(), ip.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.drawImage(ip.createImage(), 0, 0, null);
        return bi;
    }

    /** Write the color table entries (for 8 bit grayscale or indexed color).
     *  Byte order or LUT entries: blue byte, green byte, red byte, 0 byte */
    private void writeLUT(ImageProcessor ip) throws IOException {
        IndexColorModel cm = (IndexColorModel) (ip.getCurrentColorModel());
        int mapSize = cm.getMapSize();
        byte[] lutWrite = new byte[4 * 256];
        for (int i = 0; i < 256; i++) {
            if (i < mapSize) {
                lutWrite[4 * i] = (byte) cm.getBlue(i);
                lutWrite[4 * i + 1] = (byte) cm.getGreen(i);
                lutWrite[4 * i + 2] = (byte) cm.getRed(i);
                lutWrite[4 * i + 3] = (byte) 0;
            }
        }
        raFile.write(lutWrite);
    }

    private double getFrameRate(ImagePlus imp) {
        double rate = imp.getCalibration().fps;
        if (rate == 0.0) rate = Animator.getFrameRate();
        if (rate <= 0.5) rate = 0.5;
        return rate;
    }

    private void writeString(String s) throws IOException {
        byte[] bytes = s.getBytes("UTF8");
        raFile.write(bytes);
    }

    /** Write 4-byte int with Intel (little-endian) byte order
     * (note: RandomAccessFile.writeInt has other byte order than AVI) */
    private void writeInt(int v) throws IOException {
        raFile.write(v & 0xFF);
        raFile.write((v >>> 8) & 0xFF);
        raFile.write((v >>> 16) & 0xFF);
        raFile.write((v >>> 24) & 0xFF);
    }

    /** Write 2-byte short with Intel (little-endian) byte order
     * (note: RandomAccessFile.writeShort has other byte order than AVI) */
    private void writeShort(int v) throws IOException {
        raFile.write(v & 0xFF);
        raFile.write((v >>> 8) & 0xFF);
    }

    /** An output stream directed to a RandomAccessFile (starting at the current position) */
    class RaOutputStream extends OutputStream {

        RandomAccessFile raFile;

        RaOutputStream(RandomAccessFile raFile) {
            this.raFile = raFile;
        }

        public void write(int b) throws IOException {
            raFile.writeByte(b);
        }

        public void write(byte[] b) throws IOException {
            raFile.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            raFile.write(b, off, len);
        }
    }
}
