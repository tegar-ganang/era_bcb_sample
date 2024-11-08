package giftoapng;

import gif.GifInfo;
import gui.I_UIupdater;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import com.sun.imageio.plugins.gif.GIFImageMetadata;
import com.sun.imageio.plugins.gif.GIFImageReader;
import com.sun.imageio.plugins.gif.GIFStreamMetadata;

/**
 * Represents an APNG, is empty at creation and is fillable by addframe() or you can
 * use the convertGiftoAPNG()-method to read an animated Gif-file into an APNG-object.
 * @author Alexander Sch&auml;ffer
 *
 */
public class APNG {

    private ArrayList<Chunk> maChunks = new ArrayList<Chunk>();

    private boolean mbReadyToWrite = false;

    private int miNumFrames = 0;

    private final Emodes meMode;

    private ChunkacTL mcAniControl;

    private int miSequenceNumber = 0;

    /**
	 * Enumerates the modes for the APNG-class.
	 * It controls the image-conversion and which chunks are needed.
	 * @author Alexander Sch&auml;ffer
	 *
	 */
    enum Emodes {

        PALETTE, RGB, ARGB
    }

    ;

    /**
	 * creates a emtpy APNG and adds necessary chunks, like IHDR(header)
	 *  and acTL(animation control). If mode is PALETTE then PLTE(palette)
	 *   and tRNS(transparecy) -chunks are added between IHDR and acTL
	 * 
	 * @param width width of the first frame
	 * @param height height of the first frame
	 * @param mode palette, truecolor(RGB) or truecolor+alpha(ARGB); cannot be changed later
	 * @param numloops the animation stops after this number of iterations
	 * @param palette colortable in RGB-byte-order with length<=768 and multiple of 3.
	 * 				If mode is <b>not</b> PALETTE then it will be ignored and may be <i>null</i>
	 * @param transparentkey the index of the palette, which will be transparent.
	 * 				If mode is <b>not</b> PALETTE then it will be ignored and may be <i>-1</i> 
	 */
    public APNG(int width, int height, Emodes mode, int numloops, byte[] palette, int transparentkey) {
        meMode = mode;
        int colormode = 0;
        switch(meMode) {
            case PALETTE:
                colormode = 3;
                break;
            case RGB:
                colormode = 2;
                break;
            case ARGB:
                colormode = 6;
                break;
            default:
                break;
        }
        maChunks.add(new ChunkIHDR(width, height, (byte) colormode));
        if (meMode == Emodes.PALETTE) {
            maChunks.add(new ChunkPLTE(palette));
            maChunks.add(new ChunktRNS(transparentkey));
        }
        mcAniControl = new ChunkacTL(numloops);
        maChunks.add(mcAniControl);
    }

    /**
	 * add the IEND-chunk after the frame-chunks
	 *and tell the acTL how many frames the animation finally has
	 */
    public void prepareToWrite() {
        if (!mbReadyToWrite) {
            maChunks.add(new ChunkIEND());
            mcAniControl.setNumFrames(miNumFrames);
            mbReadyToWrite = true;
        }
    }

    /**
	 * adds the image with offset (0,0) and minimal frame-delay to the animation
	 * @see #addFrame(BufferedImage, int, int, int, int)
	 * @param imagedata
	 */
    public void addFrame(BufferedImage imagedata) {
        addFrame(imagedata, 0, 0, 0, 1);
    }

    /**
	 * Adds the fcTL(frame-control), then
	 * converts the imagedata according to the <i>meMode</i> to the png-image-format and
	 * it inside a FDAT(for frame#1) or a fdAT(for following frames) to the chunk-array.
	 * 
	 * @param imagedata
	 * @param x_offset to offset a tile in x-direction within the frame of the animation 
	 * @param y_offset same in y-direction
	 * @param delaytime how long this frame should be shown (in 1/100ths seconds)
	 * @param disposalmethod 0=no disposal, 1=restore to background, 2=restore to previous
	 */
    public void addFrame(BufferedImage imagedata, int x_offset, int y_offset, int delaytime, int disposalmethod) {
        if (!mbReadyToWrite) {
            int width = imagedata.getWidth();
            int height = imagedata.getHeight();
            int blendmethod;
            if (meMode == Emodes.ARGB) {
                blendmethod = ChunkfcTL.APNG_BLEND_OP_OVER;
            } else {
                blendmethod = ChunkfcTL.APNG_BLEND_OP_SOURCE;
            }
            maChunks.add(new ChunkfcTL(nextSequenceNumer(), width, height, x_offset, y_offset, delaytime, disposalmethod, blendmethod));
            byte[] image = {};
            if (meMode == Emodes.PALETTE) {
                int[] indices = imagedata.getRaster().getSamples(0, 0, width, height, 0, (int[]) null);
                image = new byte[indices.length + imagedata.getHeight()];
                int read = 0;
                for (int i = 0; i < image.length; i++) {
                    if (i % (width + 1) == 0) {
                        image[i] = 0;
                    } else {
                        image[i] = (byte) indices[read];
                        read++;
                    }
                }
            } else {
                int byte_per_pixel;
                if (meMode == Emodes.RGB) {
                    byte_per_pixel = 3;
                } else {
                    byte_per_pixel = 4;
                }
                image = new byte[width * height * byte_per_pixel + imagedata.getHeight()];
                int filterBytePos = byte_per_pixel * width + 1;
                int[] colorValues = imagedata.getRGB(0, 0, width, height, null, 0, width);
                int i = 0;
                for (int color : colorValues) {
                    if (i % (filterBytePos) == 0) {
                        image[i] = 0;
                        i++;
                    }
                    image[i + 2] = (byte) color;
                    color >>= 8;
                    image[i + 1] = (byte) color;
                    color >>= 8;
                    image[i] = (byte) color;
                    if (meMode == Emodes.ARGB) {
                        color >>= 8;
                        image[i + 3] = (byte) color;
                    }
                    i += byte_per_pixel;
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            if (miNumFrames == 0) {
                maChunks.add(new ChunkIDAT(image));
            } else {
                maChunks.add(new ChunkfdAT(nextSequenceNumer(), image));
            }
            miNumFrames++;
        } else {
            throw new IllegalStateException("APNG is ready to write: no more frames can be added");
        }
    }

    /**
	 * Returns the next sequence-number for fcTL and fdAT-chunks, 
	 * and then increments miSequenceNumber
	 * @return the sequence-number
	 */
    private int nextSequenceNumer() {
        return miSequenceNumber++;
    }

    /**
	 * Will read, analyze the gif, create the APNG-object and adds the frames to it .
	 * The calling method should handle the exception.
	 * @param readGif
	 * @return the generated APNG-object
	 * @throws IOException 
	 */
    public static APNG convertGiftoAPNG(File readGif) throws IOException {
        return convertGiftoAPNG(readGif, null);
    }

    /**
	 * Will read, analyze the gif, create the APNG-object ands adds the frames to it.
	 * The calling method should handle the exception.
	 * @param readGif
	 * @param callback callback-methode, not used if null
	 * @return the generated APNG-object
	 * @throws IOException 
	 */
    public static APNG convertGiftoAPNG(File readGif, I_UIupdater callback) throws IOException {
        APNG theApng = null;
        FileInputStream fis = null;
        GIFImageReader reader = null;
        try {
            fis = new FileInputStream(readGif);
            reader = (GIFImageReader) ImageIO.getImageReadersByFormatName("GIF").next();
            ImageInputStream iis = ImageIO.createImageInputStream(fis);
            reader.setInput(iis);
            int numImages = reader.getNumImages(true);
            boolean hasLocalColors = false;
            boolean hasTransparecy = false;
            int iTransparency = -1;
            GIFStreamMetadata smd = (GIFStreamMetadata) reader.getStreamMetadata();
            GIFImageMetadata md;
            for (int i = 0; i < numImages; i++) {
                md = (GIFImageMetadata) reader.getImageMetadata(i);
                if (md.transparentColorFlag) {
                    iTransparency = md.transparentColorIndex;
                    hasTransparecy = true;
                }
                if (md.localColorTable != null) {
                    hasLocalColors = true;
                }
            }
            Emodes mode = Emodes.PALETTE;
            int decision = 0;
            if (hasLocalColors) {
                decision += 1;
            }
            if (hasTransparecy) {
                decision += 2;
            }
            if ((numImages > 1)) {
                decision += 4;
            }
            switch(decision) {
                default:
                case 0:
                case 2:
                    mode = Emodes.PALETTE;
                    break;
                case 1:
                case 4:
                case 5:
                    mode = Emodes.RGB;
                    break;
                case 3:
                case 6:
                case 7:
                    mode = Emodes.ARGB;
                    break;
            }
            md = (GIFImageMetadata) reader.getImageMetadata(0);
            if (md.imageLeftPosition > 0 || md.imageTopPosition > 0 || md.imageHeight != smd.logicalScreenHeight || md.imageWidth != smd.logicalScreenWidth) {
                mode = Emodes.ARGB;
            }
            int numLoops = ChunkacTL.INFINITE_LOOP;
            GifInfo readgifinfo = new GifInfo(readGif);
            numLoops = readgifinfo.getLoops();
            theApng = new APNG(smd.logicalScreenWidth, smd.logicalScreenHeight, mode, numLoops, (mode == Emodes.PALETTE) ? smd.globalColorTable : null, iTransparency);
            if (callback != null) {
                callback.updatemax(numImages);
            }
            for (int i = 0; i < numImages; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    reader.dispose();
                    fis.close();
                    throw new CancellationException("stop-button was pushed");
                }
                if (callback != null) {
                    callback.updatecur(i + 1);
                }
                md = (GIFImageMetadata) reader.getImageMetadata(i);
                int idismet = md.disposalMethod;
                if (idismet > 0 && idismet <= 3) {
                    idismet = idismet - 1;
                }
                if (i == 0 && (md.imageLeftPosition > 0 || md.imageTopPosition > 0 || md.imageHeight != smd.logicalScreenHeight || md.imageWidth != smd.logicalScreenWidth)) {
                    BufferedImage tmp = new BufferedImage(smd.logicalScreenWidth, smd.logicalScreenHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics tmpg = tmp.getGraphics();
                    tmpg.setColor(new Color(0, 0, 0, 0));
                    tmpg.fillRect(0, 0, smd.logicalScreenWidth, smd.logicalScreenHeight);
                    tmpg.drawImage(reader.read(i), md.imageLeftPosition, md.imageTopPosition, null);
                    theApng.addFrame(tmp, 0, 0, md.delayTime, idismet);
                } else {
                    theApng.addFrame(reader.read(i), md.imageLeftPosition, md.imageTopPosition, md.delayTime, idismet);
                }
            }
            theApng.maChunks.add(new ChunktEXt());
            theApng.maChunks.add(new ChunktEXt("Comment", readgifinfo.getComment()));
            reader.dispose();
            fis.close();
            if (callback != null) {
                callback.updatecur(numImages + 1);
            }
        } catch (IOException ex) {
            if (reader != null) {
                reader.dispose();
            }
            if (fis != null) {
                fis.close();
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (reader != null) {
                reader.dispose();
            }
            if (fis != null) {
                fis.close();
            }
            throw ex;
        }
        return theApng;
    }

    /**
	 * Writes all chunks of the array into the given BufferedOutputStream.
	 * The calling method should handle the exception. 
	 * @param bos
	 * @throws IOException 
	 */
    public void write(BufferedOutputStream bos) throws IOException {
        if (!mbReadyToWrite) {
            prepareToWrite();
        }
        Iterator<Chunk> allchunks = maChunks.iterator();
        while (allchunks.hasNext()) {
            allchunks.next().write(bos);
        }
    }
}
