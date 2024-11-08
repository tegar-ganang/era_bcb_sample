package org.paw.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class Pack {

    public static final byte[] zipData(byte[] data) {
        ByteArrayOutputStream baos = null;
        GZIPOutputStream gos = null;
        byte[] out = null;
        try {
            baos = new ByteArrayOutputStream();
            gos = new GZIPOutputStream(baos);
            gos.write(data);
            gos.finish();
            out = baos.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (gos != null) gos.close();
                if (baos != null) baos.close();
            } catch (Exception e) {
            }
        }
        return out;
    }

    public static final byte[] unzipData(byte[] in) {
        ByteArrayInputStream bais = null;
        GZIPInputStream gis = null;
        ByteArrayOutputStream baos = null;
        byte[] buffer = new byte[1024];
        try {
            bais = new ByteArrayInputStream(in);
            gis = new GZIPInputStream(bais);
            baos = new ByteArrayOutputStream();
            int read = 0;
            while ((read = gis.read(buffer)) != -1) baos.write(buffer, 0, read);
        } catch (Exception ioe) {
            buffer = null;
            ioe.printStackTrace();
            baos = null;
        } finally {
            try {
                if (gis != null) gis.close();
                if (bais != null) bais.close();
                if (baos != null) baos.close();
            } catch (Exception e) {
            }
        }
        return baos.toByteArray();
    }

    public static final byte[] compressData(byte[] uncompressedData) {
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(uncompressedData);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(uncompressedData.length);
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] compressedData = bos.toByteArray();
        return compressedData;
    }

    public static final byte[] deCompressData(byte[] compressedData) {
        Inflater decompressor = new Inflater();
        decompressor.setInput(compressedData);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
                e.printStackTrace();
                break;
            }
        }
        try {
            bos.close();
        } catch (IOException e) {
        }
        byte[] decompressedData = bos.toByteArray();
        return (decompressedData);
    }
}
