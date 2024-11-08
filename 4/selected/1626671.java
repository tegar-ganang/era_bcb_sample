package com.lepidllama.packageeditor.resources;

import gr.zdimensions.jsquish.Squish;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import com.lepidllama.packageeditor.core.exception.ParsingRuntimeException;
import com.lepidllama.packageeditor.dbpf.Header;
import com.lepidllama.packageeditor.dbpf.IndexBlock;
import com.lepidllama.packageeditor.fileio.ByteArrayDataReader;
import com.lepidllama.packageeditor.fileio.DataReader;
import com.lepidllama.packageeditor.fileio.DataWriter;
import com.lepidllama.packageeditor.resources.interfaces.ImageOutputting;
import com.lepidllama.packageeditor.utility.DDSImage;

public class Dds extends Resource implements ImageOutputting {

    DDSImage dds;

    DataReader inBuffer;

    long offset;

    int originalLength;

    @Override
    public void read(DataReader in, Header header, IndexBlock indexBlock) {
        inBuffer = in;
        offset = in.getFilePointer();
        originalLength = (int) indexBlock.getDecompressedSize();
        ByteArrayDataReader newin = new ByteArrayDataReader(in.readChunk(originalLength));
        dds = DDSImage.read(newin);
        refreshAll();
    }

    @Override
    public void write(DataWriter out, Header header, IndexBlock indexBlock) {
        if (dds.isChanged()) {
            dds.write(out);
        } else {
            long returnTo = inBuffer.getFilePointer();
            inBuffer.seek(offset);
            out.writeChunk(inBuffer.readChunk(originalLength));
            inBuffer.seek(returnTo);
        }
    }

    public void setImage(BufferedImage image) {
        dds.setImage(image);
        refreshAll();
    }

    public BufferedImage getImage() {
        int width = dds.getWidth();
        int height = dds.getHeight();
        byte[] texture;
        try {
            texture = dds.getMipMap(0);
            switch(dds.getCompressionFormat()) {
                case DDSImage.D3DFMT_UNKNOWN:
                    return getRaw32bit(texture, width, height);
                case DDSImage.D3DFMT_A8R8G8B8:
                    return getRaw32bit(texture, width, height);
                case DDSImage.D3DFMT_R8G8B8:
                    return getRaw24bit(texture, width, height);
                case DDSImage.D3DFMT_DXT1:
                    return getDXTImage(Squish.CompressionType.DXT1, texture, width, height);
                case DDSImage.D3DFMT_DXT3:
                    return getDXTImage(Squish.CompressionType.DXT3, texture, width, height);
                case DDSImage.D3DFMT_DXT5:
                    return getDXTImage(Squish.CompressionType.DXT5, texture, width, height);
            }
        } catch (IOException e) {
            throw new ParsingRuntimeException(e);
        }
        return null;
    }

    public Icon getIcon() throws IOException {
        BufferedImage b = getImage();
        if (b != null) {
            return new ImageIcon(b);
        }
        return null;
    }

    private BufferedImage getDXTImage(Squish.CompressionType type, byte[] texture, int width, int height) {
        byte[] decompressed = Squish.decompressImage(null, width, height, texture, type);
        BufferedImage bm = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        WritableRaster raster = bm.getRaster();
        DataBuffer db = raster.getDataBuffer();
        for (int i = 0; i < decompressed.length / 4; i++) {
            db.setElem(i * 4, decompressed[i * 4 + 3] & 0xFF);
            db.setElem(i * 4 + 1, decompressed[i * 4 + 2] & 0xFF);
            db.setElem(i * 4 + 2, decompressed[i * 4 + 1] & 0xFF);
            db.setElem(i * 4 + 3, decompressed[i * 4] & 0xFF);
        }
        return bm;
    }

    private BufferedImage getRaw32bit(byte[] texture, int width, int height) {
        BufferedImage bm = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        WritableRaster raster = bm.getRaster();
        DataBuffer db = raster.getDataBuffer();
        for (int i = 0; i < texture.length / 4; i++) {
            db.setElem(i * 4, texture[i * 4 + 3] & 0xFF);
            db.setElem(i * 4 + 1, texture[i * 4] & 0xFF);
            db.setElem(i * 4 + 2, texture[i * 4 + 1] & 0xFF);
            db.setElem(i * 4 + 3, texture[i * 4 + 2] & 0xFF);
        }
        return bm;
    }

    private BufferedImage getRaw24bit(byte[] texture, int width, int height) {
        BufferedImage bm = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster = bm.getRaster();
        DataBuffer db = raster.getDataBuffer();
        ByteArrayInputStream bais = new ByteArrayInputStream(texture);
        for (int i = 0; bais.available() > 0; i++) {
            db.setElem(i, bais.read() & 0xFF);
        }
        return bm;
    }

    private BufferedImage getRaw16bitGreyscale(byte[] texture, int width, int height) {
        BufferedImage bm = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = bm.getRaster();
        ByteArrayInputStream bais = new ByteArrayInputStream(texture);
        for (int i = 0; i < height; i++) {
            for (int j = 0; bais.available() > 0 && j < width; j++) {
                int value = bais.read() | (bais.read() << 8);
                raster.setPixel(j, i, new int[] { value });
            }
        }
        return bm;
    }
}
