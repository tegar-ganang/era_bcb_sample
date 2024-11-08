package com.wizzer.m3g.toolkit.png;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

class PNGData {

    private int m_numberOfChunks;

    private PNGChunk[] m_chunks;

    public PNGData() {
        m_numberOfChunks = 0;
        m_chunks = new PNGChunk[10];
    }

    public void add(PNGChunk chunk) {
        m_chunks[m_numberOfChunks++] = chunk;
        if (m_numberOfChunks >= m_chunks.length) {
            PNGChunk[] largerArray = new PNGChunk[m_chunks.length + 10];
            System.arraycopy(m_chunks, 0, largerArray, 0, m_chunks.length);
            m_chunks = largerArray;
        }
    }

    public long getWidth() {
        return getChunk("IHDR").getUnsignedInt(0);
    }

    public long getHeight() {
        return getChunk("IHDR").getUnsignedInt(4);
    }

    public short getBitsPerPixel() {
        return getChunk("IHDR").getUnsignedByte(8);
    }

    public short getColorType() {
        return getChunk("IHDR").getUnsignedByte(9);
    }

    public short getCompression() {
        return getChunk("IHDR").getUnsignedByte(10);
    }

    public short getFilter() {
        return getChunk("IHDR").getUnsignedByte(11);
    }

    public short getInterlace() {
        return getChunk("IHDR").getUnsignedByte(12);
    }

    public ColorModel getColorModel() {
        short colorType = getColorType();
        int bitsPerPixel = getBitsPerPixel();
        if (colorType == 3) {
            byte[] paletteData = getChunk("PLTE").getData();
            int paletteLength = paletteData.length / 3;
            return new IndexColorModel(bitsPerPixel, paletteLength, paletteData, 0, false);
        }
        System.out.println("Unsupported color type: " + colorType);
        return null;
    }

    public WritableRaster getRaster() {
        int width = (int) getWidth();
        int height = (int) getHeight();
        int bitsPerPixel = getBitsPerPixel();
        short colorType = getColorType();
        if (colorType == 3) {
            byte[] imageData = getImageData();
            DataBuffer db = new DataBufferByte(imageData, imageData.length);
            WritableRaster raster = Raster.createPackedRaster(db, width, height, bitsPerPixel, null);
            return raster;
        } else System.out.println("Unsupported color type!");
        return null;
    }

    public byte[] getImageData() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 0; i < m_numberOfChunks; i++) {
                PNGChunk chunk = m_chunks[i];
                if (chunk.getTypeString().equals("IDAT")) {
                    out.write(chunk.getData());
                }
            }
            out.flush();
            InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(out.toByteArray()));
            ByteArrayOutputStream inflatedOut = new ByteArrayOutputStream();
            int readLength;
            byte[] block = new byte[8192];
            while ((readLength = in.read(block)) != -1) inflatedOut.write(block, 0, readLength);
            inflatedOut.flush();
            byte[] imageData = inflatedOut.toByteArray();
            int width = (int) getWidth();
            int height = (int) getHeight();
            int bitsPerPixel = getBitsPerPixel();
            int length = width * height * bitsPerPixel / 8;
            byte[] prunedData = new byte[length];
            if (getInterlace() == 0) {
                int index = 0;
                for (int i = 0; i < length; i++) {
                    if ((i * 8 / bitsPerPixel) % width == 0) {
                        index++;
                    }
                    prunedData[i] = imageData[index++];
                }
            } else System.out.println("Couldn't undo interlacing.");
            return prunedData;
        } catch (IOException ioe) {
        }
        return null;
    }

    public PNGChunk getChunk(String type) {
        for (int i = 0; i < m_numberOfChunks; i++) if (m_chunks[i].getTypeString().equals(type)) return m_chunks[i];
        return null;
    }
}
