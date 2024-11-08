package com.kenai.jbosh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Codec methods for compressing and uncompressing using ZLIB.
 */
final class ZLIBCodec {

    /**
     * Size of the internal buffer when decoding.
     */
    private static final int BUFFER_SIZE = 512;

    /**
     * Prevent construction.
     */
    private ZLIBCodec() {
    }

    /**
     * Returns the name of the codec.
     *
     * @return string name of the codec (i.e., "deflate")
     */
    public static String getID() {
        return "deflate";
    }

    /**
     * Compress/encode the data provided using the ZLIB format.
     *
     * @param data data to compress
     * @return compressed data
     * @throws IOException on compression failure
     */
    public static byte[] encode(final byte[] data) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DeflaterOutputStream deflateOut = null;
        try {
            deflateOut = new DeflaterOutputStream(byteOut);
            deflateOut.write(data);
            deflateOut.close();
            byteOut.close();
            return byteOut.toByteArray();
        } finally {
            deflateOut.close();
            byteOut.close();
        }
    }

    /**
     * Uncompress/decode the data provided using the ZLIB format.
     *
     * @param data data to uncompress
     * @return uncompressed data
     * @throws IOException on decompression failure
     */
    public static byte[] decode(final byte[] compressed) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(compressed);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        InflaterInputStream inflaterIn = null;
        try {
            inflaterIn = new InflaterInputStream(byteIn);
            int read;
            byte[] buffer = new byte[BUFFER_SIZE];
            do {
                read = inflaterIn.read(buffer);
                if (read > 0) {
                    byteOut.write(buffer, 0, read);
                }
            } while (read >= 0);
            return byteOut.toByteArray();
        } finally {
            inflaterIn.close();
            byteOut.close();
        }
    }
}
