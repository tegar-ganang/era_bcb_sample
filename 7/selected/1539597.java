package io;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.Color;
import java.awt.Point;
import java.io.OutputStream;
import java.io.IOException;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;

/**
 * Class AnimatedGifEncoder2 - Encodes a GIF file consisting of one or
 * more frames.
 * <pre>
 *
 *
 * Extensively Modified for ImagePlus
 * Extended to handle 8 bit Images with more complex Color lookup tables with transparency index
 *
 * Ryan Raz March 2002
 * raz@rraz.ca
 * Version 1.01
 ** Extensively Modified for ImagePlus
 * Extended to handle 8 bit Images with more complex Color lookup tables with transparency index
 *
 * Ryan Raz March 2002
 * ryan@rraz.ca
 * Version 1.01 Please report any bugs
 *
 * Operation Manual
 *
 *
 * 1) Load stack with 8 bit or RGB images it is possible to use the animated gif reader but because the color
 *   table is lost it is best to also load a separate copy of the first image in the series this will allow 
 *   extraction of the original image color look up table (see 1below)
 * 2)Check the option list to bring up the option list.
 * 3)Experiment with the option list. I usually use a global color table to save space, set to do not dispose if 
 *      each consecutive image is overlayed on the previous image.
 * 4)Color table can be imported from another image or extracted from 8bit stack images or loaded as the
 *    first 256  RGB triplets from a RGB images, the last mode takes either a imported image or current 
 *    stack and creates the color table from scratch.
 *  
 *
 *    To do list 
 *
 *     1) Modify existing Animated Gif reader plug in to import in 8 bit mode (currently only works in 
 *         RGB  mode.  Right now the best way to alter an animated gif is to save the first image separately
 *         and then read the single gif and use the plugin animated reader to read the animated gif to the 
 *         stack. Let this plugin encode the stack using the single gif's color table.
 *      2) Add support for background colors easy but I have no use for them
 *      3) RGB to 8 bit converter is a linear search. Needs to be replaced with sorted list and fast search. But 
 *          this update could cause problems with some types of gifs. Easy fix get a faster computer.
 *      4) Try updating NN color converter seems to be heavily weighted towards quantity of pixels.
 *        example:
 *           if there is 90% of the image covered in shades of one color or grey the 10% of other colors tend 
 *           to be poorly represented it  over fits the shades and under fits the others. Works well if the
 *          distribution  is balanced.
 *       5) Add support for all sizes of Color Look Up tables.
 *       6) Re-code to be cleaner. This is my second Java program and I started with some code with too 
 *           many  global variables and I added more switches so its a bit hard to follow.
 *
 * Credits for the base conversion codes
 * No copyright asserted on the source code of this class.  May be used
 * for any purpose, however, refer to the Unisys LZW patent for restrictions
 * on use of the associated LZWEncoder class.  Please forward any corrections
 * to kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software
 * @version 1.0 December 2000
 *
 *
 * Example:
 *    AnimatedGifEncoder2 e = new AnimatedGifEncoder2();
 *    e.start(outputFileName);
 *    e.addFrame(image1);
 *    e.addFrame(image2);
 *      "           "             "
 *    e.finish();
 * </pre>
 *
 *
 */
class AnimatedGifEncoder2 {

    protected int width;

    protected int height;

    protected boolean transparent = false;

    protected int transIndex;

    protected int repeat = -1;

    protected int delay = 50;

    protected boolean started = false;

    protected OutputStream out;

    protected ImagePlus image;

    protected byte[] pixels;

    protected byte[] indexedPixels;

    protected int colorDepth;

    protected byte[] colorTab;

    protected int lctSize = 7;

    protected int dispose = 0;

    protected boolean closeStream = false;

    protected boolean firstFrame = true;

    protected boolean sizeSet = false;

    protected int sample = 2;

    protected byte[] gct = null;

    protected boolean gctused = false;

    protected boolean autotransparent = false;

    protected boolean GCTextracted = false;

    protected boolean GCTloadedExternal = false;

    protected int GCTred = 0;

    protected int GCTgrn = 0;

    protected int GCTbl = 0;

    protected int GCTcindex = 0;

    protected boolean GCTsetTransparent = false;

    protected boolean GCToverideIndex = false;

    protected boolean GCToverideColor = false;

    /**
    * Adds next GIF frame.  The frame is not written immediately, but is
    * actually deferred until the next frame is received so that timing
    * data can be inserted.  Invoking <code>finish()</code> flushes all
    * frames.  If <code>setSize</code> was not invoked, the size of the
    * first image is used for all subsequent frames.
    *
    * @param im  containing frame to write.
    * @return true if successful.
    */
    public boolean addFrame(ImagePlus image) {
        if ((image == null) || !started) return false;
        boolean ok = true;
        try {
            if (firstFrame) {
                if (!sizeSet) {
                    setSize(image.getWidth(), image.getHeight());
                }
                if (gctused) writeLSDgct();
                if (GCTloadedExternal) {
                    colorTab = gct;
                    TransparentIndex(colorTab);
                    writePalette();
                    if (repeat >= 0) writeNetscapeExt();
                }
                if (!gctused) {
                    writeLSD();
                    if (repeat >= 0) writeNetscapeExt();
                }
                firstFrame = false;
            }
            int type = image.getType();
            int k;
            if ((type == 0) || (type == 3)) {
                Process8bitCLT(image);
            } else if (type == 4) {
                packrgb(image);
                OverRideQuality(image.getWidth() * image.getHeight());
                if (gctused && (gct == null)) {
                    analyzePixels();
                    colorTab = gct;
                    TransparentIndex(colorTab);
                    writePalette();
                    if (repeat >= 0) writeNetscapeExt();
                } else analyzePixels();
            } else throw new IllegalArgumentException("Image must be 8-bit or RGB");
            TransparentIndex(colorTab);
            writeGraphicCtrlExt();
            writeImageDesc();
            if (!gctused) writePalette();
            writePixels();
        } catch (IOException e) {
            ok = false;
        }
        return ok;
    }

    void TransparentIndex(byte[] colorTab) {
        if (autotransparent || !GCTsetTransparent) return;
        if (colorTab == null) throw new IllegalArgumentException("Color Table not loaded.");
        int len = colorTab.length;
        setTransparent(true);
        if (!(GCToverideColor || GCToverideIndex)) {
            transIndex = GCTcindex;
            return;
        }
        if (GCToverideIndex) GCTcindex = findClosest(colorTab, GCTred, GCTgrn, GCTbl);
        transIndex = GCTcindex;
        int pindex = 3 * GCTcindex;
        if (pindex > (len - 3)) throw new IllegalArgumentException("Index (" + transIndex + ") too large for Color Lookup table.");
        colorTab[pindex++] = (byte) GCTred;
        colorTab[pindex++] = (byte) GCTgrn;
        colorTab[pindex] = (byte) GCTbl;
    }

    String name;

    public boolean setoptions() {
        String[] GCTtype = { "Do not use", "Load from Current Image", "Load from another Image RGB or 8 Bit", "Use another RGB to create a new color table " };
        String[] DisposalType = { "No Disposal", "Do not Dispose", "Restore to Background", "Restore to previous" };
        String[] TransparencyType = { "No Transparency", "Automatically Set if Available (8 bit only)", "Set to Index", "Set to index with specified color", "Set to the index that is closest to specified color" };
        int setdelay = delay * 10;
        int gctType = 0;
        int setTrans;
        if (GCTloadedExternal) gctType = 2;
        if (GCTextracted && GCTloadedExternal) gctType = 3;
        if (gctused && !(GCTextracted || GCTloadedExternal)) gctType = 1;
        setTrans = 1;
        if (!(autotransparent || GCTsetTransparent || GCToverideIndex || GCToverideColor)) setTrans = 0;
        if (GCTsetTransparent && !(GCToverideIndex || GCToverideColor)) setTrans = 2;
        if (GCTsetTransparent && GCToverideIndex && !GCToverideColor) setTrans = 4;
        if (GCTsetTransparent && !GCToverideIndex && GCToverideColor) setTrans = 3;
        int red = GCTred;
        int grn = GCTgrn;
        int bl = GCTbl;
        int cindex = GCTcindex;
        int disposalType = dispose;
        String title1 = "";
        int[] wList = WindowManager.getIDList();
        if (wList == null) {
            IJ.error("No windows are open.");
            return false;
        }
        String[] titles = new String[wList.length];
        for (int i = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null && imp.getStackSize() == 1) titles[i] = imp.getTitle(); else titles[i] = "";
        }
        GenericDialog gd = new GenericDialog("Animated Gif Writer");
        gd.addStringField("Name:", name, 12);
        String defaultItem;
        if (title1.equals("")) defaultItem = titles[0]; else defaultItem = title1;
        gd.addChoice("Set_Global_Lookup_Table_Options", GCTtype, GCTtype[gctType]);
        gd.addChoice("Optional Image for Global Color Lookup Table", titles, defaultItem);
        gd.addChoice("Image Disposal Code", DisposalType, DisposalType[disposalType]);
        gd.addNumericField("Set delay in milliseconds", (double) setdelay, 0);
        gd.addNumericField("Number of plays 0 is loop continuously -1 default", (double) repeat, 0);
        gd.addChoice("Transparency setting", TransparencyType, TransparencyType[setTrans]);
        gd.addNumericField("Red value", (double) red, 0);
        gd.addNumericField("Green value", (double) grn, 0);
        gd.addNumericField("Blue value", (double) bl, 0);
        gd.addNumericField("Index in Color Table", (double) cindex, 0);
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        name = gd.getNextString();
        gctType = gd.getNextChoiceIndex();
        int index1 = gd.getNextChoiceIndex();
        title1 = titles[index1];
        disposalType = gd.getNextChoiceIndex();
        setdelay = (int) gd.getNextNumber();
        if (setdelay >= 0) setDelay(setdelay);
        setRepeat((int) gd.getNextNumber());
        setTrans = gd.getNextChoiceIndex();
        red = (int) gd.getNextNumber();
        grn = (int) gd.getNextNumber();
        bl = (int) gd.getNextNumber();
        if ((red < 0) || (red > 255)) red = -1;
        if ((grn < 0) || (grn > 255)) red = -1;
        if ((bl < 0) || (bl > 255)) red = -1;
        cindex = (int) gd.getNextNumber();
        if ((cindex < 0) || (cindex > 255)) cindex = -1;
        autotransparent = false;
        GCTsetTransparent = false;
        GCToverideIndex = false;
        GCToverideColor = false;
        setTransparent(false);
        switch(setTrans) {
            case 0:
                break;
            case 1:
                autotransparent = true;
                break;
            case 2:
                if (cindex > -1) {
                    GCTsetTransparent = true;
                    GCTcindex = cindex;
                } else IJ.error("Incorrect color index must have value between 0 and 255");
                break;
            case 3:
                if ((cindex > -1) && (red > -1)) {
                    GCTsetTransparent = true;
                    GCToverideColor = true;
                    GCTcindex = cindex;
                    GCTred = red;
                    GCTgrn = grn;
                    GCTbl = bl;
                } else IJ.error("Incorrect colors or color index, they must have values between 0 and 255.");
                break;
            case 4:
                if (red > -1) {
                    GCTsetTransparent = true;
                    GCToverideIndex = true;
                    GCTred = red;
                    GCTgrn = grn;
                    GCTbl = bl;
                } else IJ.error("Incorrect colors, they must have values between 0 and 255.");
                break;
            default:
                break;
        }
        gctused = false;
        GCTextracted = false;
        GCTloadedExternal = false;
        if (gctType == 1) gctused = true; else if (gctType >= 2) {
            ImagePlus img = WindowManager.getImage(wList[index1]);
            if (img == null) {
                IJ.error("No window selected for generating color table");
                return false;
            }
            int type = img.getType();
            if (!((type == 0) || (type == 3) || (type == 4))) {
                IJ.error("Incorrect window type selected for generating color table (RGB or 8bit only).");
                return false;
            }
            if (gctType == 3) {
                if (type == 4) extractGCTrgb(img); else {
                    IJ.error("RGB image only for this mode of generating color table.");
                    return false;
                }
            }
            if (gctType == 2) {
                if (type == 4) loadGCTrgb(img); else loadGCT8bit(img);
            }
        }
        return true;
    }

    /********************************************************
*    Gets Color lookup Table from  8 bit image plus pointer to image
*/
    void Process8bitCLT(ImagePlus image) {
        colorDepth = 8;
        setTransparent(false);
        ByteProcessor pg = new ByteProcessor(image.getImage());
        ColorModel cm = pg.getColorModel();
        if (cm instanceof IndexColorModel) {
            indexedPixels = (byte[]) (pg.getPixels());
        } else throw new IllegalArgumentException("Image must be 8-bit");
        IndexColorModel m = (IndexColorModel) cm;
        if (autotransparent) {
            transIndex = m.getTransparentPixel();
            if ((transIndex > -1) && (transIndex < 256)) setTransparent(true); else transIndex = 0;
        }
        int mapSize = m.getMapSize();
        int k;
        if (gctused && (gct == null)) {
            gct = new byte[mapSize * 3];
            for (int i = 0; i < mapSize; i++) {
                k = i * 3;
                gct[k] = (byte) m.getRed(i);
                gct[k + 2] = (byte) m.getBlue(i);
                gct[k + 1] = (byte) m.getGreen(i);
            }
            try {
                if (!GCTloadedExternal) {
                    colorTab = gct;
                    writePalette();
                    if (repeat >= 0) writeNetscapeExt();
                }
            } catch (IOException e) {
                System.err.println("Caught IOException: " + e.getMessage());
            }
        }
        if (gctused) {
            colorTab = gct;
        } else {
            colorTab = new byte[mapSize * 3];
            for (int i = 0; i < mapSize; i++) {
                k = i * 3;
                colorTab[k] = (byte) m.getRed(i);
                colorTab[k + 1] = (byte) m.getBlue(i);
                colorTab[k + 2] = (byte) m.getGreen(i);
            }
        }
        m.finalize();
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
        GCTextracted = false;
        GCTloadedExternal = false;
        transIndex = 0;
        transparent = false;
        gct = null;
        out = null;
        image = null;
        pixels = null;
        indexedPixels = null;
        colorTab = null;
        closeStream = false;
        firstFrame = true;
        return ok;
    }

    public void loadGCT8bit(ImagePlus image) {
        int type = image.getType();
        if (!(((type == 0) || (type == 3)) && (image != null))) throw new IllegalArgumentException("Color Table Image must be 8 bit");
        gctused = true;
        GCTloadedExternal = true;
        gct = null;
        Process8bitCLT(image);
    }

    public void extractGCTrgb(ImagePlus image) {
        if ((image == null) || (4 != image.getType())) throw new IllegalArgumentException("Color Table Image must be RGB");
        packrgb(image);
        gctused = true;
        GCTextracted = true;
        GCTloadedExternal = true;
        gct = null;
        OverRideQuality(image.getWidth() * image.getHeight());
        analyzePixels();
        pixels = null;
    }

    void packrgb(ImagePlus image) {
        int len = image.getWidth() * image.getHeight();
        ImageProcessor imp = image.getProcessor();
        int[] pix = (int[]) imp.getPixels();
        pixels = new byte[len * 3];
        for (int i = 0; i < len; i++) {
            int k = i * 3;
            pixels[k + 2] = (byte) ((pix[i] & 0xff0000) >> 16);
            pixels[k + 1] = (byte) ((pix[i] & 0x00ff00) >> 8);
            pixels[k] = (byte) (pix[i] & 0x0000ff);
        }
    }

    public void loadGCTrgb(ImagePlus image) {
        if ((image == null) || (4 != image.getType())) throw new IllegalArgumentException("Color Table Image must be RGB");
        int len = image.getWidth() * image.getHeight();
        if (len > 255) len = 255;
        ImageProcessor imp = image.getProcessor();
        int[] pix = (int[]) imp.getPixels();
        gct = new byte[len * 3];
        for (int i = 0; i < len; i++) {
            int k = i * 3;
            gct[k] = (byte) ((pix[i] & 0xff0000) >> 16);
            gct[k + 1] = (byte) ((pix[i] & 0x00ff00) >> 8);
            gct[k + 2] = (byte) (pix[i] & 0x0000ff);
        }
        gctused = true;
        GCTloadedExternal = true;
    }

    public void setGCT(boolean flag) {
        gctused = flag;
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
 *	Set True for Global Color Table use 
 *	This saves space in the output file but colors may not be so goodif the stack uses
 *      True color images
 */
    public void GlobalColorTableused(boolean gtu) {
        gctused = gtu;
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
    public void setTransparent(boolean c) {
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
	Sets Net sample size depending on image size
	
**/
    public void OverRideQuality(int npixs) {
        if (npixs > 100000) sample = 10; else sample = npixs / 10000;
        if (sample < 1) sample = 1;
    }

    /**
    * Analyzes image colors and creates color map.
    */
    protected void analyzePixels() {
        int len = pixels.length;
        int nPix = len / 3;
        indexedPixels = new byte[nPix];
        if (gctused && (gct == null)) {
            NeuQuant nq = new NeuQuant(pixels, len, sample);
            colorTab = nq.process();
            gct = new byte[colorTab.length];
            for (int i = 0; i < colorTab.length; i += 3) {
                byte temp = colorTab[i];
                colorTab[i] = colorTab[i + 2];
                colorTab[i + 2] = temp;
                gct[i] = colorTab[i];
                gct[i + 1] = colorTab[i + 1];
                gct[i + 2] = colorTab[i + 2];
            }
            if (GCTextracted) {
                indexedPixels = null;
                return;
            }
        }
        if (!gctused) {
            NeuQuant nq = new NeuQuant(pixels, len, sample);
            colorTab = nq.process();
            for (int i = 0; i < colorTab.length; i += 3) {
                byte temp = colorTab[i];
                colorTab[i] = colorTab[i + 2];
                colorTab[i + 2] = temp;
            }
            int k = 0;
            for (int i = 0; i < nPix; i++) indexedPixels[i] = (byte) nq.map(pixels[k++] & 0xff, pixels[k++] & 0xff, pixels[k++] & 0xff);
            pixels = null;
            colorDepth = 8;
            lctSize = 7;
        }
        if (gctused) {
            colorTab = gct;
            int k = 0;
            int minpos;
            for (int j = 0; j < nPix; j++) {
                int b = pixels[k++] & 0xff;
                int g = pixels[k++] & 0xff;
                int r = pixels[k++] & 0xff;
                minpos = 0;
                int dmin = 256 * 256 * 256;
                int lenct = colorTab.length;
                for (int i = 0; i < lenct; ) {
                    int dr = r - (colorTab[i++] & 0xff);
                    int dg = g - (colorTab[i++] & 0xff);
                    int db = b - (colorTab[i] & 0xff);
                    int d = dr * dr + dg * dg + db * db;
                    if (d < dmin) {
                        dmin = d;
                        minpos = i / 3;
                    }
                    i++;
                }
                indexedPixels[j] = (byte) minpos;
            }
            pixels = null;
            colorDepth = 8;
            lctSize = 7;
        }
    }

    /**
    * Returns index of palette color closest to c
    *
    */
    protected int findClosest(byte[] colorTab, int r, int g, int b) {
        if (colorTab == null) return -1;
        int minpos = 0;
        int dmin = 256 * 256 * 256;
        int len = colorTab.length;
        for (int i = 0; i < len; ) {
            int dr = r - (colorTab[i++] & 0xff);
            int dg = g - (colorTab[i++] & 0xff);
            int db = b - (colorTab[i] & 0xff);
            int d = dr * dr + dg * dg + db * db;
            if (d < dmin) {
                dmin = d;
                minpos = i / 3;
            }
            i++;
        }
        return minpos;
    }

    /**
    * Writes Graphic Control Extension
    */
    protected void writeGraphicCtrlExt() throws IOException {
        out.write(0x21);
        out.write(0xf9);
        out.write(4);
        int transp, disp;
        if (!transparent) {
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
        if (gctused) out.write(0x00); else out.write(0x80 | 0 | 0 | 0 | lctSize);
    }

    /**
    * Writes Logical Screen Descriptor with global color table
    */
    protected void writeLSDgct() throws IOException {
        writeShort(width);
        writeShort(height);
        out.write((0x80 | 0x70 | 0x00 | lctSize));
        out.write(0);
        out.write(0);
    }

    /**
    * Writes Logical Screen Descriptor without global color table
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
        LZWEncoder2 encoder = new LZWEncoder2(width, height, indexedPixels, colorDepth);
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

/**

Writes a stack as an animated Gif

*/
public class Gif_Stack_Writer implements PlugIn {

    static String type = "gif";

    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.showMessage("Save As ", "No images are open.");
            return;
        }
        String name = imp.getTitle();
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex >= 0) name = name.substring(0, dotIndex);
        AnimatedGifEncoder2 fr = new AnimatedGifEncoder2();
        fr.name = name;
        if (!fr.setoptions()) return;
        name = fr.name;
        IJ.register(Gif_Stack_Writer.class);
        SaveDialog sd = new SaveDialog("Filename", name + "." + type, "." + type);
        String file = sd.getFileName();
        if (file == null) return;
        String directory = sd.getDirectory();
        ImageStack stack = imp.getStack();
        ImagePlus tmp = new ImagePlus();
        int nSlices = stack.getSize();
        fr.start(directory + file);
        for (int i = 1; i <= nSlices; i++) {
            IJ.showStatus("writing: " + i + "/" + nSlices);
            IJ.showProgress((double) i / nSlices);
            tmp.setProcessor(null, stack.getProcessor(i));
            try {
                fr.addFrame(tmp);
            } catch (Exception e) {
                IJ.showMessage("Save as " + type, "" + e);
            }
            System.gc();
        }
        fr.finish();
        IJ.showStatus("");
        IJ.showProgress(1.0);
    }
}

class LZWEncoder2 {

    private static final int EOF = -1;

    private int imgW, imgH;

    private byte[] pixAry;

    private int initCodeSize;

    private int remaining;

    private int curPixel;

    static final int BITS = 12;

    static final int HSIZE = 5003;

    int n_bits;

    int maxbits = BITS;

    int maxcode;

    int maxmaxcode = 1 << BITS;

    int[] htab = new int[HSIZE];

    int[] codetab = new int[HSIZE];

    int hsize = HSIZE;

    int free_ent = 0;

    boolean clear_flg = false;

    int g_init_bits;

    int ClearCode;

    int EOFCode;

    int cur_accum = 0;

    int cur_bits = 0;

    int masks[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF };

    int a_count;

    byte[] accum = new byte[256];

    LZWEncoder2(int width, int height, byte[] pixels, int color_depth) {
        imgW = width;
        imgH = height;
        pixAry = pixels;
        initCodeSize = Math.max(2, color_depth);
    }

    void char_out(byte c, OutputStream outs) throws IOException {
        accum[a_count++] = c;
        if (a_count >= 254) flush_char(outs);
    }

    void cl_block(OutputStream outs) throws IOException {
        cl_hash(hsize);
        free_ent = ClearCode + 2;
        clear_flg = true;
        output(ClearCode, outs);
    }

    void cl_hash(int hsize) {
        for (int i = 0; i < hsize; ++i) htab[i] = -1;
    }

    void compress(int init_bits, OutputStream outs) throws IOException {
        int fcode;
        int i;
        int c;
        int ent;
        int disp;
        int hsize_reg;
        int hshift;
        g_init_bits = init_bits;
        clear_flg = false;
        n_bits = g_init_bits;
        maxcode = MAXCODE(n_bits);
        ClearCode = 1 << (init_bits - 1);
        EOFCode = ClearCode + 1;
        free_ent = ClearCode + 2;
        a_count = 0;
        ent = nextPixel();
        hshift = 0;
        for (fcode = hsize; fcode < 65536; fcode *= 2) ++hshift;
        hshift = 8 - hshift;
        hsize_reg = hsize;
        cl_hash(hsize_reg);
        output(ClearCode, outs);
        outer_loop: while ((c = nextPixel()) != EOF) {
            fcode = (c << maxbits) + ent;
            i = (c << hshift) ^ ent;
            if (htab[i] == fcode) {
                ent = codetab[i];
                continue;
            } else if (htab[i] >= 0) {
                disp = hsize_reg - i;
                if (i == 0) disp = 1;
                do {
                    if ((i -= disp) < 0) i += hsize_reg;
                    if (htab[i] == fcode) {
                        ent = codetab[i];
                        continue outer_loop;
                    }
                } while (htab[i] >= 0);
            }
            output(ent, outs);
            ent = c;
            if (free_ent < maxmaxcode) {
                codetab[i] = free_ent++;
                htab[i] = fcode;
            } else cl_block(outs);
        }
        output(ent, outs);
        output(EOFCode, outs);
    }

    void encode(OutputStream os) throws IOException {
        os.write(initCodeSize);
        remaining = imgW * imgH;
        curPixel = 0;
        compress(initCodeSize + 1, os);
        os.write(0);
    }

    void flush_char(OutputStream outs) throws IOException {
        if (a_count > 0) {
            outs.write(a_count);
            outs.write(accum, 0, a_count);
            a_count = 0;
        }
    }

    final int MAXCODE(int n_bits) {
        return (1 << n_bits) - 1;
    }

    private int nextPixel() {
        if (remaining == 0) return EOF;
        --remaining;
        byte pix = pixAry[curPixel++];
        return pix & 0xff;
    }

    void output(int code, OutputStream outs) throws IOException {
        cur_accum &= masks[cur_bits];
        if (cur_bits > 0) cur_accum |= (code << cur_bits); else cur_accum = code;
        cur_bits += n_bits;
        while (cur_bits >= 8) {
            char_out((byte) (cur_accum & 0xff), outs);
            cur_accum >>= 8;
            cur_bits -= 8;
        }
        if (free_ent > maxcode || clear_flg) {
            if (clear_flg) {
                maxcode = MAXCODE(n_bits = g_init_bits);
                clear_flg = false;
            } else {
                ++n_bits;
                if (n_bits == maxbits) maxcode = maxmaxcode; else maxcode = MAXCODE(n_bits);
            }
        }
        if (code == EOFCode) {
            while (cur_bits > 0) {
                char_out((byte) (cur_accum & 0xff), outs);
                cur_accum >>= 8;
                cur_bits -= 8;
            }
            flush_char(outs);
        }
    }
}

class NeuQuant {

    protected static final int netsize = 256;

    protected static final int prime1 = 499;

    protected static final int prime2 = 491;

    protected static final int prime3 = 487;

    protected static final int prime4 = 503;

    protected static final int minpicturebytes = (3 * prime4);

    protected static final int maxnetpos = (netsize - 1);

    protected static final int netbiasshift = 4;

    protected static final int ncycles = 100;

    protected static final int intbiasshift = 16;

    protected static final int intbias = (((int) 1) << intbiasshift);

    protected static final int gammashift = 10;

    protected static final int gamma = (((int) 1) << gammashift);

    protected static final int betashift = 10;

    protected static final int beta = (intbias >> betashift);

    protected static final int betagamma = (intbias << (gammashift - betashift));

    protected static final int initrad = (netsize >> 3);

    protected static final int radiusbiasshift = 6;

    protected static final int radiusbias = (((int) 1) << radiusbiasshift);

    protected static final int initradius = (initrad * radiusbias);

    protected static final int radiusdec = 30;

    protected static final int alphabiasshift = 10;

    protected static final int initalpha = (((int) 1) << alphabiasshift);

    protected int alphadec;

    protected static final int radbiasshift = 8;

    protected static final int radbias = (((int) 1) << radbiasshift);

    protected static final int alpharadbshift = (alphabiasshift + radbiasshift);

    protected static final int alpharadbias = (((int) 1) << alpharadbshift);

    protected byte[] thepicture;

    protected int lengthcount;

    protected int samplefac;

    protected int[][] network;

    protected int[] netindex = new int[256];

    protected int[] bias = new int[netsize];

    protected int[] freq = new int[netsize];

    protected int[] radpower = new int[initrad];

    public NeuQuant(byte[] thepic, int len, int sample) {
        int i;
        int[] p;
        thepicture = thepic;
        lengthcount = len;
        samplefac = sample;
        network = new int[netsize][];
        for (i = 0; i < netsize; i++) {
            network[i] = new int[4];
            p = network[i];
            p[0] = p[1] = p[2] = (i << (netbiasshift + 8)) / netsize;
            freq[i] = intbias / netsize;
            bias[i] = 0;
        }
    }

    public byte[] colorMap() {
        byte[] map = new byte[3 * netsize];
        int[] index = new int[netsize];
        for (int i = 0; i < netsize; i++) index[network[i][3]] = i;
        int k = 0;
        for (int i = 0; i < netsize; i++) {
            int j = index[i];
            map[k++] = (byte) (network[j][0]);
            map[k++] = (byte) (network[j][1]);
            map[k++] = (byte) (network[j][2]);
        }
        return map;
    }

    public void inxbuild() {
        int i, j, smallpos, smallval;
        int[] p;
        int[] q;
        int previouscol, startpos;
        previouscol = 0;
        startpos = 0;
        for (i = 0; i < netsize; i++) {
            p = network[i];
            smallpos = i;
            smallval = p[1];
            for (j = i + 1; j < netsize; j++) {
                q = network[j];
                if (q[1] < smallval) {
                    smallpos = j;
                    smallval = q[1];
                }
            }
            q = network[smallpos];
            if (i != smallpos) {
                j = q[0];
                q[0] = p[0];
                p[0] = j;
                j = q[1];
                q[1] = p[1];
                p[1] = j;
                j = q[2];
                q[2] = p[2];
                p[2] = j;
                j = q[3];
                q[3] = p[3];
                p[3] = j;
            }
            if (smallval != previouscol) {
                netindex[previouscol] = (startpos + i) >> 1;
                for (j = previouscol + 1; j < smallval; j++) netindex[j] = i;
                previouscol = smallval;
                startpos = i;
            }
        }
        netindex[previouscol] = (startpos + maxnetpos) >> 1;
        for (j = previouscol + 1; j < 256; j++) netindex[j] = maxnetpos;
    }

    public void learn() {
        int i, j, b, g, r;
        int radius, rad, alpha, step, delta, samplepixels;
        byte[] p;
        int pix, lim;
        if (lengthcount < minpicturebytes) samplefac = 1;
        alphadec = 30 + ((samplefac - 1) / 3);
        p = thepicture;
        pix = 0;
        lim = lengthcount;
        samplepixels = lengthcount / (3 * samplefac);
        delta = samplepixels / ncycles;
        alpha = initalpha;
        radius = initradius;
        rad = radius >> radiusbiasshift;
        if (rad <= 1) rad = 0;
        for (i = 0; i < rad; i++) radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad));
        if (lengthcount < minpicturebytes) step = 3; else if ((lengthcount % prime1) != 0) step = 3 * prime1; else {
            if ((lengthcount % prime2) != 0) step = 3 * prime2; else {
                if ((lengthcount % prime3) != 0) step = 3 * prime3; else step = 3 * prime4;
            }
        }
        i = 0;
        while (i < samplepixels) {
            b = (p[pix + 0] & 0xff) << netbiasshift;
            g = (p[pix + 1] & 0xff) << netbiasshift;
            r = (p[pix + 2] & 0xff) << netbiasshift;
            j = contest(b, g, r);
            altersingle(alpha, j, b, g, r);
            if (rad != 0) alterneigh(rad, j, b, g, r);
            pix += step;
            if (pix >= lim) pix -= lengthcount;
            i++;
            if (i % delta == 0) {
                alpha -= alpha / alphadec;
                radius -= radius / radiusdec;
                rad = radius >> radiusbiasshift;
                if (rad <= 1) rad = 0;
                for (j = 0; j < rad; j++) radpower[j] = alpha * (((rad * rad - j * j) * radbias) / (rad * rad));
            }
        }
    }

    public int map(int b, int g, int r) {
        int i, j, dist, a, bestd;
        int[] p;
        int best;
        bestd = 1000;
        best = -1;
        i = netindex[g];
        j = i - 1;
        while ((i < netsize) || (j >= 0)) {
            if (i < netsize) {
                p = network[i];
                dist = p[1] - g;
                if (dist >= bestd) i = netsize; else {
                    i++;
                    if (dist < 0) dist = -dist;
                    a = p[0] - b;
                    if (a < 0) a = -a;
                    dist += a;
                    if (dist < bestd) {
                        a = p[2] - r;
                        if (a < 0) a = -a;
                        dist += a;
                        if (dist < bestd) {
                            bestd = dist;
                            best = p[3];
                        }
                    }
                }
            }
            if (j >= 0) {
                p = network[j];
                dist = g - p[1];
                if (dist >= bestd) j = -1; else {
                    j--;
                    if (dist < 0) dist = -dist;
                    a = p[0] - b;
                    if (a < 0) a = -a;
                    dist += a;
                    if (dist < bestd) {
                        a = p[2] - r;
                        if (a < 0) a = -a;
                        dist += a;
                        if (dist < bestd) {
                            bestd = dist;
                            best = p[3];
                        }
                    }
                }
            }
        }
        return (best);
    }

    public byte[] process() {
        learn();
        unbiasnet();
        inxbuild();
        return colorMap();
    }

    public void unbiasnet() {
        int i, j;
        for (i = 0; i < netsize; i++) {
            network[i][0] >>= netbiasshift;
            network[i][1] >>= netbiasshift;
            network[i][2] >>= netbiasshift;
            network[i][3] = i;
        }
    }

    protected void alterneigh(int rad, int i, int b, int g, int r) {
        int j, k, lo, hi, a, m;
        int[] p;
        lo = i - rad;
        if (lo < -1) lo = -1;
        hi = i + rad;
        if (hi > netsize) hi = netsize;
        j = i + 1;
        k = i - 1;
        m = 1;
        while ((j < hi) || (k > lo)) {
            a = radpower[m++];
            if (j < hi) {
                p = network[j++];
                try {
                    p[0] -= (a * (p[0] - b)) / alpharadbias;
                    p[1] -= (a * (p[1] - g)) / alpharadbias;
                    p[2] -= (a * (p[2] - r)) / alpharadbias;
                } catch (Exception e) {
                }
            }
            if (k > lo) {
                p = network[k--];
                try {
                    p[0] -= (a * (p[0] - b)) / alpharadbias;
                    p[1] -= (a * (p[1] - g)) / alpharadbias;
                    p[2] -= (a * (p[2] - r)) / alpharadbias;
                } catch (Exception e) {
                }
            }
        }
    }

    protected void altersingle(int alpha, int i, int b, int g, int r) {
        int[] n = network[i];
        n[0] -= (alpha * (n[0] - b)) / initalpha;
        n[1] -= (alpha * (n[1] - g)) / initalpha;
        n[2] -= (alpha * (n[2] - r)) / initalpha;
    }

    protected int contest(int b, int g, int r) {
        int i, dist, a, biasdist, betafreq;
        int bestpos, bestbiaspos, bestd, bestbiasd;
        int[] n;
        bestd = ~(((int) 1) << 31);
        bestbiasd = bestd;
        bestpos = -1;
        bestbiaspos = bestpos;
        for (i = 0; i < netsize; i++) {
            n = network[i];
            dist = n[0] - b;
            if (dist < 0) dist = -dist;
            a = n[1] - g;
            if (a < 0) a = -a;
            dist += a;
            a = n[2] - r;
            if (a < 0) a = -a;
            dist += a;
            if (dist < bestd) {
                bestd = dist;
                bestpos = i;
            }
            biasdist = dist - ((bias[i]) >> (intbiasshift - netbiasshift));
            if (biasdist < bestbiasd) {
                bestbiasd = biasdist;
                bestbiaspos = i;
            }
            betafreq = (freq[i] >> betashift);
            freq[i] -= betafreq;
            bias[i] += (betafreq << gammashift);
        }
        freq[bestpos] += beta;
        bias[bestpos] -= betagamma;
        return (bestbiaspos);
    }
}
