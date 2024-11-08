package com.kenai.jbosh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Codec methods for compressing and uncompressing using GZIP.
 */
final class GZIPCodec {

    /**
     * Size of the internal buffer when decoding.
     */
    private static final int BUFFER_SIZE = 512;

    /**
     * Prevent construction.
     */
    private GZIPCodec() {
    }

    /**
     * Returns the name of the codec.
     *
     * @return string name of the codec (i.e., "gzip")
     */
    public static String getID() {
        return "gzip";
    }

    /**
     * Compress/encode the data provided using the GZIP format.
     *
     * @param data data to compress
     * @return compressed data
     * @throws IOException on compression failure
     */
    public static byte[] encode(final byte[] data) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        GZIPOutputStream gzOut = null;
        try {
            gzOut = new GZIPOutputStream(byteOut);
            gzOut.write(data);
            gzOut.close();
            byteOut.close();
            return byteOut.toByteArray();
        } finally {
            gzOut.close();
            byteOut.close();
        }
    }

    /**
     * Uncompress/decode the data provided using the GZIP format.
     *
     * @param data data to uncompress
     * @return uncompressed data
     * @throws IOException on decompression failure
     */
    public static byte[] decode(final byte[] compressed) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(compressed);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        GZIPInputStream gzIn = null;
        try {
            gzIn = new GZIPInputStream(byteIn);
            int read;
            byte[] buffer = new byte[BUFFER_SIZE];
            do {
                read = gzIn.read(buffer);
                if (read > 0) {
                    byteOut.write(buffer, 0, read);
                }
            } while (read >= 0);
            return byteOut.toByteArray();
        } finally {
            gzIn.close();
            byteOut.close();
        }
    }
}
