package image;

import Exception.StegoException;
import Abstract.EmbeddingProperties;
import image.chunks.PngViewChunk;
import image.chunks.ViewChunk;
import image.chunks.ViewChunkIterator;
import image.png.ImageLine;
import image.png.PngReader;
import image.png.PngWriter;
import java.util.LinkedList;

/**
 *
 * @author vitaa
 */
public class PngImage extends Image {

    ImageLine[] imagelines;

    PngReader reader;

    PngWriter writer;

    public PngImage(String filename) {
        reader = new PngReader(filename);
        imagelines = new ImageLine[reader.imgInfo.height];
        for (int i = 0; i < reader.imgInfo.height; i++) {
            imagelines[i] = new ImageLine(reader.readRow(i));
        }
        reader.end();
        width = reader.imgInfo.width;
        height = reader.imgInfo.height;
    }

    public void getEmbeddingProps(EmbeddingProperties props) {
        reader.readEmbeddingProperties(props);
    }

    @Override
    public void save(String filename) {
        if (!filename.endsWith(".png")) filename += ".png";
        writer = new PngWriter(filename, reader.imgInfo);
        writer.setOverrideFile(true);
        writer.prepare(reader);
        for (int i = 0; i < imagelines.length; i++) {
            writer.writeRow(imagelines[i]);
        }
        writer.end();
    }

    @Override
    public void save(String filename, EmbeddingProperties props) {
        if (!filename.endsWith(".png")) filename += ".png";
        writer = new PngWriter(filename, reader.imgInfo);
        writer.props = props;
        writer.setOverrideFile(true);
        writer.prepare(reader);
        for (int i = 0; i < imagelines.length; i++) {
            writer.writeRow(imagelines[i]);
        }
        writer.end();
    }

    @Override
    public LinkedList<ViewChunk> getViewChunks(int sharesCount, int maxPortationInfo) throws StegoException {
        int wn = ((int) java.lang.Math.sqrt(sharesCount));
        int hn;
        if (wn * wn == sharesCount) {
            hn = wn;
        } else {
            hn = (int) (sharesCount / wn);
            if (sharesCount % wn != 0) {
                hn++;
            }
        }
        int chunkWidth = (width * 3) / wn;
        int chunkHeight = height / hn;
        if ((chunkWidth * chunkHeight < maxPortationInfo) || (chunkWidth < 8)) {
            throw new StegoException("Couldn't write message in this image. Try select other image or make shares count less");
        }
        LinkedList<ViewChunk> list = new LinkedList<ViewChunk>();
        int offset = 0;
        int lineNumber = 0;
        for (int i = 0; i < hn; i++) {
            for (int j = 0; j < wn; j++) {
                list.add(new PngViewChunk(this, lineNumber, offset, chunkWidth, chunkHeight));
                offset += chunkWidth;
            }
            offset = 0;
            lineNumber += chunkHeight;
        }
        return list;
    }

    public byte getByte(int lineNumber, int byteNumber) {
        int info = imagelines[lineNumber].scanline[byteNumber];
        info = info & 255;
        return (byte) info;
    }

    public void setByte(int lineNumber, int byteNumber, byte info) {
        int b = imagelines[lineNumber].scanline[byteNumber];
        b = (b & 65280);
        b = (b | (info & 255));
        imagelines[lineNumber].scanline[byteNumber] = b;
    }

    @Override
    public int getSize() {
        return height * width * 3;
    }

    @Override
    public int getByteWidth() {
        return width * 3;
    }

    @Override
    public byte[] getRGB() {
        byte[] rgb = new byte[getByteWidth() * getHeight()];
        for (int i = 0; i < getHeight(); i++) {
            for (int j = 0; j < getByteWidth(); j = j + 3) {
                rgb[i * getByteWidth() + j + 2] = getByte(getHeight() - 1 - i, j);
                rgb[i * getByteWidth() + j + 1] = getByte(getHeight() - 1 - i, j + 1);
                rgb[i * getByteWidth() + j] = getByte(getHeight() - 1 - i, j + 2);
            }
        }
        return rgb;
    }
}
