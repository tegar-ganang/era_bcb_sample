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

    protected int palSize = 7;

    protected int dispose = -1;

    protected boolean closeStream = false;

    protected boolean firstFrame = true;

    protected boolean sizeSet = false;

    protected int sample = 10;

    /**
	 * Sets the delay time between each frame, or changes it
	 * for subsequent frames (applies to last frame added).
	 *
	 * @param ms int delay time in milliseconds
	 */
    public void setDelay(int ms) {
        _prof.prof.cnt[407]++;
        {
            _prof.prof.cnt[408]++;
            delay = Math.round(ms / 10.0f);
        }
    }

    /**
	 * Sets the GIF frame disposal code for the last added frame
	 * and any subsequent frames.  Default is 0 if no transparent
	 * color has been set, otherwise 2.
	 * @param code int disposal code.
	 */
    public void setDispose(int code) {
        _prof.prof.cnt[409]++;
        {
            _prof.prof.cnt[410]++;
            if (code >= 0) {
                {
                    _prof.prof.cnt[411]++;
                    dispose = code;
                }
            }
        }
    }

    /**
	 * Sets the number of times the set of GIF frames
	 * should be played.  Default is 1; 0 means play
	 * infinitely.	Must be invoked before the first
	 * image is added.
	 *
	 * @param iter int number of iterations.
	 * @return
	 */
    public void setRepeat(int iter) {
        _prof.prof.cnt[412]++;
        {
            _prof.prof.cnt[413]++;
            if (iter >= 0) {
                {
                    _prof.prof.cnt[414]++;
                    repeat = iter;
                }
            }
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
        _prof.prof.cnt[415]++;
        {
            _prof.prof.cnt[416]++;
            transparent = c;
        }
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
        _prof.prof.cnt[417]++;
        {
            _prof.prof.cnt[418]++;
            if ((im == null) || !started) {
                {
                    _prof.prof.cnt[419]++;
                    return false;
                }
            }
        }
        _prof.prof.cnt[420]++;
        boolean ok = true;
        {
            _prof.prof.cnt[421]++;
            try {
                {
                    _prof.prof.cnt[422]++;
                    if (!sizeSet) {
                        {
                            _prof.prof.cnt[423]++;
                            setSize(im.getWidth(), im.getHeight());
                        }
                    }
                }
                {
                    _prof.prof.cnt[424]++;
                    image = im;
                }
                {
                    _prof.prof.cnt[425]++;
                    getImagePixels();
                }
                {
                    _prof.prof.cnt[426]++;
                    analyzePixels();
                }
                {
                    _prof.prof.cnt[427]++;
                    if (firstFrame) {
                        {
                            _prof.prof.cnt[428]++;
                            writeLSD();
                        }
                        {
                            _prof.prof.cnt[429]++;
                            writePalette();
                        }
                        {
                            _prof.prof.cnt[430]++;
                            if (repeat >= 0) {
                                {
                                    _prof.prof.cnt[431]++;
                                    writeNetscapeExt();
                                }
                            }
                        }
                    }
                }
                {
                    _prof.prof.cnt[432]++;
                    writeGraphicCtrlExt();
                }
                {
                    _prof.prof.cnt[433]++;
                    writeImageDesc();
                }
                {
                    _prof.prof.cnt[434]++;
                    if (!firstFrame) {
                        {
                            _prof.prof.cnt[435]++;
                            writePalette();
                        }
                    }
                }
                {
                    _prof.prof.cnt[436]++;
                    writePixels();
                }
                {
                    _prof.prof.cnt[437]++;
                    firstFrame = false;
                }
            } catch (IOException e) {
                {
                    _prof.prof.cnt[438]++;
                    ok = false;
                }
            }
        }
        {
            _prof.prof.cnt[439]++;
            return ok;
        }
    }

    /**
	 * Flushes any pending data and closes output file.
	 * If writing to an OutputStream, the stream is not
	 * closed.
	 */
    public boolean finish() {
        _prof.prof.cnt[440]++;
        {
            _prof.prof.cnt[441]++;
            if (!started) {
                _prof.prof.cnt[442]++;
                return false;
            }
        }
        _prof.prof.cnt[443]++;
        boolean ok = true;
        {
            _prof.prof.cnt[444]++;
            started = false;
        }
        {
            _prof.prof.cnt[445]++;
            try {
                {
                    _prof.prof.cnt[446]++;
                    out.write(0x3b);
                }
                {
                    _prof.prof.cnt[447]++;
                    out.flush();
                }
                {
                    _prof.prof.cnt[448]++;
                    if (closeStream) {
                        {
                            _prof.prof.cnt[449]++;
                            out.close();
                        }
                    }
                }
            } catch (IOException e) {
                {
                    _prof.prof.cnt[450]++;
                    ok = false;
                }
            }
        }
        {
            _prof.prof.cnt[451]++;
            transIndex = 0;
        }
        {
            _prof.prof.cnt[452]++;
            out = null;
        }
        {
            _prof.prof.cnt[453]++;
            image = null;
        }
        {
            _prof.prof.cnt[454]++;
            pixels = null;
        }
        {
            _prof.prof.cnt[455]++;
            indexedPixels = null;
        }
        {
            _prof.prof.cnt[456]++;
            colorTab = null;
        }
        {
            _prof.prof.cnt[457]++;
            closeStream = false;
        }
        {
            _prof.prof.cnt[458]++;
            firstFrame = true;
        }
        {
            _prof.prof.cnt[459]++;
            return ok;
        }
    }

    /**
	 * Sets frame rate in frames per second.  Equivalent to
	 * <code>setDelay(1000/fps)</code>.
	 *
	 * @param fps float frame rate (frames per second)
	 */
    public void setFrameRate(float fps) {
        _prof.prof.cnt[460]++;
        {
            _prof.prof.cnt[461]++;
            if (fps != 0f) {
                {
                    _prof.prof.cnt[462]++;
                    delay = Math.round(100f / fps);
                }
            }
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
        _prof.prof.cnt[463]++;
        {
            _prof.prof.cnt[464]++;
            if (quality < 1) {
                _prof.prof.cnt[465]++;
                quality = 1;
            }
        }
        {
            _prof.prof.cnt[466]++;
            sample = quality;
        }
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
        _prof.prof.cnt[467]++;
        {
            _prof.prof.cnt[468]++;
            if (started && !firstFrame) {
                _prof.prof.cnt[469]++;
                return;
            }
        }
        {
            _prof.prof.cnt[470]++;
            width = w;
        }
        {
            _prof.prof.cnt[471]++;
            height = h;
        }
        {
            _prof.prof.cnt[472]++;
            if (width < 1) {
                _prof.prof.cnt[473]++;
                width = 320;
            }
        }
        {
            _prof.prof.cnt[474]++;
            if (height < 1) {
                _prof.prof.cnt[475]++;
                height = 240;
            }
        }
        {
            _prof.prof.cnt[476]++;
            sizeSet = true;
        }
    }

    /**
	 * Initiates GIF file creation on the given stream.  The stream
	 * is not closed automatically.
	 *
	 * @param os OutputStream on which GIF images are written.
	 * @return false if initial write failed.
	 */
    public boolean start(OutputStream os) {
        _prof.prof.cnt[477]++;
        {
            _prof.prof.cnt[478]++;
            if (os == null) {
                _prof.prof.cnt[479]++;
                return false;
            }
        }
        _prof.prof.cnt[480]++;
        boolean ok = true;
        {
            _prof.prof.cnt[481]++;
            closeStream = false;
        }
        {
            _prof.prof.cnt[482]++;
            out = os;
        }
        {
            _prof.prof.cnt[483]++;
            try {
                {
                    _prof.prof.cnt[484]++;
                    writeString("GIF89a");
                }
            } catch (IOException e) {
                {
                    _prof.prof.cnt[485]++;
                    ok = false;
                }
            }
        }
        {
            _prof.prof.cnt[486]++;
            return started = ok;
        }
    }

    /**
	 * Initiates writing of a GIF file with the specified name.
	 *
	 * @param file String containing output file name.
	 * @return false if open or initial write failed.
	 */
    public boolean start(String file) {
        _prof.prof.cnt[487]++;
        _prof.prof.cnt[488]++;
        boolean ok = true;
        {
            _prof.prof.cnt[489]++;
            try {
                {
                    _prof.prof.cnt[490]++;
                    out = new BufferedOutputStream(new FileOutputStream(file));
                }
                {
                    _prof.prof.cnt[491]++;
                    ok = start(out);
                }
                {
                    _prof.prof.cnt[492]++;
                    closeStream = true;
                }
            } catch (IOException e) {
                {
                    _prof.prof.cnt[493]++;
                    ok = false;
                }
            }
        }
        {
            _prof.prof.cnt[494]++;
            return started = ok;
        }
    }

    /**
	 * Analyzes image colors and creates color map.
	 */
    protected void analyzePixels() {
        _prof.prof.cnt[495]++;
        _prof.prof.cnt[496]++;
        int len = pixels.length;
        _prof.prof.cnt[497]++;
        int nPix = len / 3;
        {
            _prof.prof.cnt[498]++;
            indexedPixels = new byte[nPix];
        }
        _prof.prof.cnt[499]++;
        NeuQuant nq = new NeuQuant(pixels, len, sample);
        {
            _prof.prof.cnt[500]++;
            colorTab = nq.process();
        }
        {
            _prof.prof.cnt[501]++;
            for (int i = 0; i < colorTab.length; i += 3) {
                _prof.prof.cnt[502]++;
                byte temp = colorTab[i];
                {
                    _prof.prof.cnt[503]++;
                    colorTab[i] = colorTab[i + 2];
                }
                {
                    _prof.prof.cnt[504]++;
                    colorTab[i + 2] = temp;
                }
                {
                    _prof.prof.cnt[505]++;
                    usedEntry[i / 3] = false;
                }
            }
        }
        _prof.prof.cnt[506]++;
        int k = 0;
        {
            _prof.prof.cnt[507]++;
            for (int i = 0; i < nPix; i++) {
                _prof.prof.cnt[508]++;
                int index = nq.map(pixels[k++] & 0xff, pixels[k++] & 0xff, pixels[k++] & 0xff);
                {
                    _prof.prof.cnt[509]++;
                    usedEntry[index] = true;
                }
                {
                    _prof.prof.cnt[510]++;
                    indexedPixels[i] = (byte) index;
                }
            }
        }
        {
            _prof.prof.cnt[511]++;
            pixels = null;
        }
        {
            _prof.prof.cnt[512]++;
            colorDepth = 8;
        }
        {
            _prof.prof.cnt[513]++;
            palSize = 7;
        }
        {
            _prof.prof.cnt[514]++;
            if (transparent != null) {
                {
                    _prof.prof.cnt[515]++;
                    transIndex = findClosest(transparent);
                }
            }
        }
    }

    /**
	 * Returns index of palette color closest to c
	 *
	 */
    protected int findClosest(Color c) {
        _prof.prof.cnt[516]++;
        {
            _prof.prof.cnt[517]++;
            if (colorTab == null) {
                _prof.prof.cnt[518]++;
                return -1;
            }
        }
        _prof.prof.cnt[519]++;
        int r = c.getRed();
        _prof.prof.cnt[520]++;
        int g = c.getGreen();
        _prof.prof.cnt[521]++;
        int b = c.getBlue();
        _prof.prof.cnt[522]++;
        int minpos = 0;
        _prof.prof.cnt[523]++;
        int dmin = 256 * 256 * 256;
        _prof.prof.cnt[524]++;
        int len = colorTab.length;
        {
            _prof.prof.cnt[525]++;
            for (int i = 0; i < len; ) {
                _prof.prof.cnt[526]++;
                int dr = r - (colorTab[i++] & 0xff);
                _prof.prof.cnt[527]++;
                int dg = g - (colorTab[i++] & 0xff);
                _prof.prof.cnt[528]++;
                int db = b - (colorTab[i] & 0xff);
                _prof.prof.cnt[529]++;
                int d = dr * dr + dg * dg + db * db;
                _prof.prof.cnt[530]++;
                int index = i / 3;
                {
                    _prof.prof.cnt[531]++;
                    if (usedEntry[index] && (d < dmin)) {
                        {
                            _prof.prof.cnt[532]++;
                            dmin = d;
                        }
                        {
                            _prof.prof.cnt[533]++;
                            minpos = index;
                        }
                    }
                }
                {
                    _prof.prof.cnt[534]++;
                    i++;
                }
            }
        }
        {
            _prof.prof.cnt[535]++;
            return minpos;
        }
    }

    /**
	 * Extracts image pixels into byte array "pixels"
	 */
    protected void getImagePixels() {
        _prof.prof.cnt[536]++;
        _prof.prof.cnt[537]++;
        int w = image.getWidth();
        _prof.prof.cnt[538]++;
        int h = image.getHeight();
        _prof.prof.cnt[539]++;
        int type = image.getType();
        {
            _prof.prof.cnt[540]++;
            if ((w != width) || (h != height) || (type != BufferedImage.TYPE_3BYTE_BGR)) {
                _prof.prof.cnt[541]++;
                BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                _prof.prof.cnt[542]++;
                Graphics2D g = temp.createGraphics();
                {
                    _prof.prof.cnt[543]++;
                    g.drawImage(image, 0, 0, null);
                }
                {
                    _prof.prof.cnt[544]++;
                    image = temp;
                }
            }
        }
        {
            _prof.prof.cnt[545]++;
            pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        }
    }

    /**
	 * Writes Graphic Control Extension
	 */
    protected void writeGraphicCtrlExt() throws IOException {
        _prof.prof.cnt[546]++;
        {
            _prof.prof.cnt[547]++;
            out.write(0x21);
        }
        {
            _prof.prof.cnt[548]++;
            out.write(0xf9);
        }
        {
            _prof.prof.cnt[549]++;
            out.write(4);
        }
        _prof.prof.cnt[550]++;
        int transp, disp;
        {
            _prof.prof.cnt[551]++;
            if (transparent == null) {
                {
                    _prof.prof.cnt[552]++;
                    transp = 0;
                }
                {
                    _prof.prof.cnt[553]++;
                    disp = 0;
                }
            } else {
                {
                    _prof.prof.cnt[554]++;
                    transp = 1;
                }
                {
                    _prof.prof.cnt[555]++;
                    disp = 2;
                }
            }
        }
        {
            _prof.prof.cnt[556]++;
            if (dispose >= 0) {
                {
                    _prof.prof.cnt[557]++;
                    disp = dispose & 7;
                }
            }
        }
        {
            _prof.prof.cnt[558]++;
            disp <<= 2;
        }
        {
            _prof.prof.cnt[559]++;
            out.write(0 | disp | 0 | transp);
        }
        {
            _prof.prof.cnt[560]++;
            writeShort(delay);
        }
        {
            _prof.prof.cnt[561]++;
            out.write(transIndex);
        }
        {
            _prof.prof.cnt[562]++;
            out.write(0);
        }
    }

    /**
	 * Writes Image Descriptor
	 */
    protected void writeImageDesc() throws IOException {
        _prof.prof.cnt[563]++;
        {
            _prof.prof.cnt[564]++;
            out.write(0x2c);
        }
        {
            _prof.prof.cnt[565]++;
            writeShort(0);
        }
        {
            _prof.prof.cnt[566]++;
            writeShort(0);
        }
        {
            _prof.prof.cnt[567]++;
            writeShort(width);
        }
        {
            _prof.prof.cnt[568]++;
            writeShort(height);
        }
        {
            _prof.prof.cnt[569]++;
            if (firstFrame) {
                {
                    _prof.prof.cnt[570]++;
                    out.write(0);
                }
            } else {
                {
                    _prof.prof.cnt[571]++;
                    out.write(0x80 | 0 | 0 | 0 | palSize);
                }
            }
        }
    }

    /**
	 * Writes Logical Screen Descriptor
	 */
    protected void writeLSD() throws IOException {
        _prof.prof.cnt[572]++;
        {
            _prof.prof.cnt[573]++;
            writeShort(width);
        }
        {
            _prof.prof.cnt[574]++;
            writeShort(height);
        }
        {
            _prof.prof.cnt[575]++;
            out.write((0x80 | 0x70 | 0x00 | palSize));
        }
        {
            _prof.prof.cnt[576]++;
            out.write(0);
        }
        {
            _prof.prof.cnt[577]++;
            out.write(0);
        }
    }

    /**
	 * Writes Netscape application extension to define
	 * repeat count.
	 */
    protected void writeNetscapeExt() throws IOException {
        _prof.prof.cnt[578]++;
        {
            _prof.prof.cnt[579]++;
            out.write(0x21);
        }
        {
            _prof.prof.cnt[580]++;
            out.write(0xff);
        }
        {
            _prof.prof.cnt[581]++;
            out.write(11);
        }
        {
            _prof.prof.cnt[582]++;
            writeString("NETSCAPE" + "2.0");
        }
        {
            _prof.prof.cnt[583]++;
            out.write(3);
        }
        {
            _prof.prof.cnt[584]++;
            out.write(1);
        }
        {
            _prof.prof.cnt[585]++;
            writeShort(repeat);
        }
        {
            _prof.prof.cnt[586]++;
            out.write(0);
        }
    }

    /**
	 * Writes color table
	 */
    protected void writePalette() throws IOException {
        _prof.prof.cnt[587]++;
        {
            _prof.prof.cnt[588]++;
            out.write(colorTab, 0, colorTab.length);
        }
        _prof.prof.cnt[589]++;
        int n = (3 * 256) - colorTab.length;
        {
            _prof.prof.cnt[590]++;
            for (int i = 0; i < n; i++) {
                {
                    _prof.prof.cnt[591]++;
                    out.write(0);
                }
            }
        }
    }

    /**
	 * Encodes and writes pixel data
	 */
    protected void writePixels() throws IOException {
        _prof.prof.cnt[592]++;
        _prof.prof.cnt[593]++;
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        {
            _prof.prof.cnt[594]++;
            encoder.encode(out);
        }
    }

    /**
	 *    Write 16-bit value to output stream, LSB first
	 */
    protected void writeShort(int value) throws IOException {
        _prof.prof.cnt[595]++;
        {
            _prof.prof.cnt[596]++;
            out.write(value & 0xff);
        }
        {
            _prof.prof.cnt[597]++;
            out.write((value >> 8) & 0xff);
        }
    }

    /**
	 * Writes string to output stream
	 */
    protected void writeString(String s) throws IOException {
        _prof.prof.cnt[598]++;
        {
            _prof.prof.cnt[599]++;
            for (int i = 0; i < s.length(); i++) {
                {
                    _prof.prof.cnt[600]++;
                    out.write((byte) s.charAt(i));
                }
            }
        }
    }
}
