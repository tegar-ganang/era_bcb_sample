import java.io.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Class AnimatedGifEncoder - Encodes a GIF file consisting of one or
 * more frames.
 * <pre>
 * Example:
 *    AnimatedGifEncoder e = new AnimatedGifEncoder();
 *    e.start(outputFileName);
 *    e.setDelay(1000);	  // 1 frame per sec
 *    e.addFrame(image1);
 *    e.addFrame(image2);
 *    e.finish();
 * </pre>
 * No copyright asserted on the source code of this class.  May be used
 * for any purpose, however, refer to the Unisys LZW patent for restrictions
 * on use of the associated LZWEncoder class.  Please forward any corrections
 * to kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software
 * @version 1.03 November 2003
 *
 */
public class AnimatedGifEncoder {

    /**int that represents the width of the image size.
	  */
    protected int width;

    /**int that represents the height of the image size.
	  */
    protected int height;

    /**Color object that represents the transparent color.
	  */
    protected Color transparent = null;

    /**int that represents the transparent index in the color table.
	  */
    protected int transIndex;

    /**int that represents the number of times to repeat the animation, DEFALUT is -1 (no repeat).
	  */
    protected int repeat = -1;

    /**int that represents the frame delay in hundredths.
	  */
    protected int delay = 0;

    /**boolean that represents whether or not it is ready to output frames.
	  */
    protected boolean started = false;

    /**OutputStream object for printing.
	  */
    protected OutputStream out;

    /**BufferedImage object that represents the current frame.
	  */
    protected BufferedImage image;

    /**byte array to hold RGB values from frame.
	  */
    protected byte[] pixels;

    /**byte array to hold converted frame indexed to palette.
	  */
    protected byte[] indexedPixels;

    /**int that represents the number of bit planes.
	  */
    protected int colorDepth;

    /**byte array that represents the RGB palette.
	  */
    protected byte[] colorTab;

    /**boolean array to represent active palette entries.
	  */
    protected boolean[] usedEntry = new boolean[256];

    /**int that represents the color table size (bits-1).
	  */
    protected int palSize = 7;

    /**int that represents the disposal code (-1 = use default).
	  */
    protected int dispose = -1;

    /**boolean that represents closing the stream when finished (False) and not closing the stream (True).
	  */
    protected boolean closeStream = false;

    /**boolean that represents animation is on the first frame(True) and not on the first frame (False).
	  */
    protected boolean firstFrame = true;

    /**boolean that represents getting size from first frame (False) do not get size from first frame (True).
	  */
    protected boolean sizeSet = false;

    /**int that represents the default sample interval for quantizer (10).
	  */
    protected int sample = 10;

    /**
	 * Sets the delay time between each frame, or changes it
	 * for subsequent frames (applies to last frame added).
	 *
	 * @param ms int delay time in milliseconds
	 */
    public void setDelay(int ms) {
        delay = Math.round(ms / 10.0f);
    }

    /**
	 * Sets the GIF frame disposal code for the last added frame
	 * and any subsequent frames.  Default is 0 if no transparent
	 * color has been set, otherwise 2.
	 * @param code int disposal code.
	 */
    public void setDispose(int code) {
        if (code >= 0) {
            dispose = code;
        }
    }

    /**
	 * Sets the number of times the set of GIF frames
	 * should be played.  Default is 1; 0 means play
	 * infinitely.	Must be invoked before the first
	 * image is added.
	 *
	 * @param iter int number of iterations.
	 */
    public void setRepeat(int iter) {
        if (iter >= 0) {
            repeat = iter;
        }
    }

    /**
	 * Sets the transparent color for the last added frame
	 * and any subsequent frames.
	 * Since all colors are subject to modification
	 * in the quantization process, the color in the final
	 * palette for each frame closest to the given color
	 * becomes the transparent color for that frame.
	 * May be set to null to indicate no transparent color.
	 *
	 * @param c Color to be treated as transparent on display.
	 */
    public void setTransparent(Color c) {
        transparent = c;
    }

    /**
	 * Adds next GIF frame.	 The frame is not written immediately, but is
	 * actually deferred until the next frame is received so that timing
	 * data can be inserted.  Invoking <code>finish()</code> flushes all
	 * frames.  If <code>setSize</code> was not invoked, the size of the
	 * first image is used for all subsequent frames.
	 *
	 * @param im BufferedImage containing frame to write.
	 * @return true if successful.
	 */
    public boolean addFrame(BufferedImage im) {
        if ((im == null) || !started) {
            return false;
        }
        boolean ok = true;
        try {
            if (!sizeSet) {
                setSize(im.getWidth(), im.getHeight());
            }
            image = im;
            getImagePixels();
            analyzePixels();
            if (firstFrame) {
                writeLSD();
                writePalette();
                if (repeat >= 0) {
                    writeNetscapeExt();
                }
            }
            writeGraphicCtrlExt();
            writeImageDesc();
            if (!firstFrame) {
                writePalette();
            }
            writePixels();
            firstFrame = false;
        } catch (IOException e) {
            ok = false;
        }
        im.flush();
        return ok;
    }

    /**
	 * Flushes any pending data and closes output file.
	 * If writing to an OutputStream, the stream is not
	 * closed.
	 * @return true if no exception is caught otherwise returns false
	 */
    public boolean finish() {
        if (!started) return false;
        boolean ok = true;
        started = false;
        try {
            out.write(0x3b);
            out.flush();
            if (closeStream) {
                out.close();
            }
        } catch (IOException e) {
            ok = false;
        }
        transIndex = 0;
        out = null;
        image = null;
        pixels = null;
        indexedPixels = null;
        colorTab = null;
        closeStream = false;
        firstFrame = true;
        return ok;
    }

    /**
	 * Sets frame rate in frames per second.  Equivalent to
	 * <code>setDelay(1000/fps)</code>.
	 *
	 * @param fps float frame rate (frames per second)
	 */
    public void setFrameRate(float fps) {
        if (fps != 0f) {
            delay = Math.round(100f / fps);
        }
    }

    /**
	 * Sets quality of color quantization (conversion of images
	 * to the maximum 256 colors allowed by the GIF specification).
	 * Lower values (minimum = 1) produce better colors, but slow
	 * processing significantly.  10 is the default, and produces
	 * good color mapping at reasonable speeds.  Values greater
	 * than 20 do not yield significant improvements in speed.
	 *
	 * @param quality int greater than 0.
	 */
    public void setQuality(int quality) {
        if (quality < 1) quality = 1;
        sample = quality;
    }

    /**
	 * Sets the GIF frame size.  The default size is the
	 * size of the first frame added if this method is
	 * not invoked.
	 *
	 * @param w int frame width.
	 * @param h int frame width.
	 */
    public void setSize(int w, int h) {
        if (started && !firstFrame) return;
        width = w;
        height = h;
        if (width < 1) width = 320;
        if (height < 1) height = 240;
        sizeSet = true;
    }

    /**
	 * Initiates GIF file creation on the given stream.  The stream
	 * is not closed automatically.
	 *
	 * @param os OutputStream on which GIF images are written.
	 * @return false if initial write failed.
	 */
    public boolean start(OutputStream os) {
        if (os == null) return false;
        boolean ok = true;
        closeStream = false;
        out = os;
        try {
            writeString("GIF89a");
        } catch (IOException e) {
            ok = false;
        }
        return started = ok;
    }

    /**
	 * Initiates writing of a GIF file with the specified name.
	 *
	 * @param file String containing output file name.
	 * @return false if open or initial write failed.
	 */
    public boolean start(String file) {
        boolean ok = true;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            ok = start(out);
            closeStream = true;
        } catch (IOException e) {
            ok = false;
        }
        return started = ok;
    }

    /**
	 * Analyzes image colors and creates color map.
	 */
    protected void analyzePixels() {
        int len = pixels.length;
        int nPix = len / 3;
        indexedPixels = new byte[nPix];
        NeuQuant nq = new NeuQuant(pixels, len, sample);
        colorTab = nq.process();
        for (int i = 0; i < colorTab.length; i += 3) {
            byte temp = colorTab[i];
            colorTab[i] = colorTab[i + 2];
            colorTab[i + 2] = temp;
            usedEntry[i / 3] = false;
        }
        int k = 0;
        for (int i = 0; i < nPix; i++) {
            int index = nq.map(pixels[k++] & 0xff, pixels[k++] & 0xff, pixels[k++] & 0xff);
            usedEntry[index] = true;
            indexedPixels[i] = (byte) index;
        }
        pixels = null;
        colorDepth = 8;
        palSize = 7;
        if (transparent != null) {
            transIndex = findClosest(transparent);
        }
    }

    /**
	 * Returns index of palette color closest to c.
	 * @param c  a Color object
	 * @return an int representing the index of palette color closest to c
	 */
    protected int findClosest(Color c) {
        if (colorTab == null) return -1;
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        int minpos = 0;
        int dmin = 256 * 256 * 256;
        int len = colorTab.length;
        for (int i = 0; i < len; ) {
            int dr = r - (colorTab[i++] & 0xff);
            int dg = g - (colorTab[i++] & 0xff);
            int db = b - (colorTab[i] & 0xff);
            int d = dr * dr + dg * dg + db * db;
            int index = i / 3;
            if (usedEntry[index] && (d < dmin)) {
                dmin = d;
                minpos = index;
            }
            i++;
        }
        return minpos;
    }

    /**
	 * Extracts image pixels into byte array "pixels".
	 */
    protected void getImagePixels() {
        int w = image.getWidth();
        int h = image.getHeight();
        int type = image.getType();
        if ((w != width) || (h != height) || (type != BufferedImage.TYPE_3BYTE_BGR)) {
            BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = temp.createGraphics();
            g.drawImage(image, 0, 0, null);
            image = temp;
            temp.flush();
        }
        pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    }

    /**
	 * Writes Graphic Control Extension.
	 * @throws IOException
	 */
    protected void writeGraphicCtrlExt() throws IOException {
        out.write(0x21);
        out.write(0xf9);
        out.write(4);
        int transp, disp;
        if (transparent == null) {
            transp = 0;
            disp = 0;
        } else {
            transp = 1;
            disp = 2;
        }
        if (dispose >= 0) {
            disp = dispose & 7;
        }
        disp <<= 2;
        out.write(0 | disp | 0 | transp);
        writeShort(delay);
        out.write(transIndex);
        out.write(0);
    }

    /**
	 * Writes Image Descriptor.
	 * @throws IOException
	 */
    protected void writeImageDesc() throws IOException {
        out.write(0x2c);
        writeShort(0);
        writeShort(0);
        writeShort(width);
        writeShort(height);
        if (firstFrame) {
            out.write(0);
        } else {
            out.write(0x80 | 0 | 0 | 0 | palSize);
        }
    }

    /**
	 * Writes Logical Screen Descriptor.
	 * @throws IOException
	 */
    protected void writeLSD() throws IOException {
        writeShort(width);
        writeShort(height);
        out.write((0x80 | 0x70 | 0x00 | palSize));
        out.write(0);
        out.write(0);
    }

    /**
	 * Writes Netscape application extension to define
	 * repeat count.
	 * @throws IOException
	 */
    protected void writeNetscapeExt() throws IOException {
        out.write(0x21);
        out.write(0xff);
        out.write(11);
        writeString("NETSCAPE" + "2.0");
        out.write(3);
        out.write(1);
        writeShort(repeat);
        out.write(0);
    }

    /**
	 * Writes color table.
	 * @throws IOException
	 */
    protected void writePalette() throws IOException {
        out.write(colorTab, 0, colorTab.length);
        int n = (3 * 256) - colorTab.length;
        for (int i = 0; i < n; i++) {
            out.write(0);
        }
    }

    /**
	 * Encodes and writes pixel data.
	 * @throws IOException
	 */
    protected void writePixels() throws IOException {
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        encoder.encode(out);
    }

    /**
	 *    Write 16-bit value to output stream, LSB first.
	 * @throws IOException
	 * @param value  an int specifying a number to be printed as a 16-bit value
	 */
    protected void writeShort(int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    /**
	 * Writes string to output stream.
	 * @throws IOException
	 * @param s  a String to print to output stream
	 */
    protected void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write((byte) s.charAt(i));
        }
    }
}
