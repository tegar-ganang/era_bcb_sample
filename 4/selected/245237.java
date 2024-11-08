package com.jogamp.opengl.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Utility class which helps take fast screenshots of OpenGL rendering
 * results into Targa-format files. Used by the {@link com.jogamp.opengl.util.awt.Screenshot}
 * class; can also be used in conjunction with the {@link com.jogamp.opengl.util.gl2.TileRenderer} class.
 */
public class TGAWriter {

    private static final int TARGA_HEADER_SIZE = 18;

    private FileChannel ch;

    private ByteBuffer buf;

    /** Constructor for the TGAWriter. */
    public TGAWriter() {
    }

    /**
   * Opens the specified Targa file for writing, overwriting any
   * existing file, and sets up the header of the file expecting the
   * data to be filled in before closing it.
   *
   * @param file the file to write containing the screenshot
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   * @param alpha whether the alpha channel should be saved. If true,
   *   requires GL_EXT_abgr extension to be present.
   *
   * @throws IOException if an I/O error occurred while writing the
   *   file
   */
    public void open(File file, int width, int height, boolean alpha) throws IOException {
        RandomAccessFile out = new RandomAccessFile(file, "rw");
        ch = out.getChannel();
        int pixelSize = (alpha ? 32 : 24);
        int numChannels = (alpha ? 4 : 3);
        int fileLength = TARGA_HEADER_SIZE + width * height * numChannels;
        out.setLength(fileLength);
        MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0, fileLength);
        image.put(0, (byte) 0).put(1, (byte) 0);
        image.put(2, (byte) 2);
        image.put(12, (byte) (width & 0xFF));
        image.put(13, (byte) (width >> 8));
        image.put(14, (byte) (height & 0xFF));
        image.put(15, (byte) (height >> 8));
        image.put(16, (byte) pixelSize);
        image.position(TARGA_HEADER_SIZE);
        buf = image.slice();
    }

    /**
   * Returns the ByteBuffer corresponding to the data for the image.
   * This must be filled in with data in either BGR or BGRA format
   * depending on whether an alpha channel was specified during
   * open().
   */
    public ByteBuffer getImageData() {
        return buf;
    }

    public void close() throws IOException {
        ch.close();
        buf = null;
    }
}
