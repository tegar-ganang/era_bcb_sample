package gov.nasa.gsfc.visbard.util.gif;

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
 *    e.setDelay(1000);   // 1 frame per sec
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
 * @version 1.01 April 2002
 *
 */
public class AnimatedGifEncoder {

    protected int width;

    protected int height;

    protected Color transparent = null;

    protected int transIndex;

    protected int repeat = -1;

    protected int delay = 0;

    protected boolean started = false;

    protected OutputStream out;

    protected BufferedImage image;

    protected byte[] pixels;

    protected byte[] indexedPixels;

    protected int colorDepth;

    protected byte[] colorTab;

    protected boolean[] usedEntry = new boolean[256];

    protected int lctSize = 7;

    protected int dispose = -1;

    protected boolean closeStream = false;

    protected boolean firstFrame = true;

    protected boolean sizeSet = false;

    protected int sample = 10;

    /**
   * Adds next GIF frame.  The frame is not written immediately, but is
   * actually deferred until the next frame is received so that timing
   * data can be inserted.  Invoking <code>finish()</code> flushes all
   * frames.  If <code>setSize</code> was not invoked, the size of the
   * first image is used for all subsequent frames.
   *
   * @param im BufferedImage containing frame to write.
   * @return true if successful.
   */
    public boolean addFrame(BufferedImage im) {
        if ((im == null) || !started) return false;
        boolean ok = true;
        try {
            if (firstFrame) {
                if (!sizeSet) {
                    setSize(im.getWidth(), im.getHeight());
                }
                writeLSD();
                if (repeat >= 0) writeNetscapeExt();
                firstFrame = false;
            }
            image = im;
            getImagePixels();
            analyzePixels();
            writeGraphicCtrlExt();
            writeImageDesc();
            writePalette();
            writePixels();
        } catch (IOException e) {
            ok = false;
        }
        return ok;
    }

    /**
   * Flushes any pending data and closes output file.
   * If writing to an OutputStream, the stream is not
   * closed.
   */
    public boolean finish() {
        if (!started) return false;
        boolean ok = true;
        started = false;
        try {
            out.write(0x3b);
            out.flush();
            if (closeStream) out.close();
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
        if (code >= 0) dispose = code;
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
   * @return
   */
    public void setQuality(int quality) {
        if (quality < 1) quality = 1;
        sample = quality;
    }

    /**
   * Sets the number of times the set of GIF frames
   * should be played.  Default is 1; 0 means play
   * indefinitely.  Must be invoked before the first
   * image is added.
   *
   * @param iter int number of iterations.
   * @return
   */
    public void setRepeat(int iter) {
        if (iter >= 0) repeat = iter;
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
        lctSize = 7;
        if (transparent != null) transIndex = findClosest(transparent);
    }

    /**
   * Returns index of palette color closest to c
   *
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
   * Extracts image pixels into byte array "pixels"
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
        }
        pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    }

    /**
   * Writes Graphic Control Extension
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
        if (dispose >= 0) disp = dispose & 7;
        disp <<= 2;
        out.write(0 | disp | 0 | transp);
        writeShort(delay);
        out.write(transIndex);
        out.write(0);
    }

    /**
   * Writes Image Descriptor
   */
    protected void writeImageDesc() throws IOException {
        out.write(0x2c);
        writeShort(0);
        writeShort(0);
        writeShort(width);
        writeShort(height);
        out.write(0x80 | 0 | 0 | 0 | lctSize);
    }

    /**
   * Writes Logical Screen Descriptor
   */
    protected void writeLSD() throws IOException {
        writeShort(width);
        writeShort(height);
        out.write((0x00 | 0x70 | 0x00 | 0x00));
        out.write(0);
        out.write(0);
    }

    /**
   * Writes Netscape application extension to define
   * repeat count.
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
   * Writes color table
   */
    protected void writePalette() throws IOException {
        out.write(colorTab, 0, colorTab.length);
        int n = (3 * 256) - colorTab.length;
        for (int i = 0; i < n; i++) out.write(0);
    }

    /**
   * Encodes and writes pixel data
   */
    protected void writePixels() throws IOException {
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        encoder.encode(out);
    }

    /**
   *    Write 16-bit value to output stream, LSB first
   */
    protected void writeShort(int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    /**
   * Writes string to output stream
   */
    protected void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) out.write((byte) s.charAt(i));
    }
}
