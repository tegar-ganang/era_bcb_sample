package org.apache.batik.test.util;

import com.google.code.appengine.awt.Color;
import com.google.code.appengine.awt.Graphics2D;
import com.google.code.appengine.awt.image.BufferedImage;
import com.google.code.appengine.awt.image.ColorModel;
import com.google.code.appengine.awt.image.RenderedImage;
import com.google.code.appengine.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.ext.awt.image.spi.ImageWriter;
import org.apache.batik.ext.awt.image.spi.ImageWriterRegistry;
import org.apache.batik.test.AbstractTest;
import org.apache.batik.test.TestReport;
import org.apache.batik.util.ParsedURL;

/**
 * This test does a pixel comparison of two images and passes if the 
 * two images are identical. It fails otherwise, producing a report
 * describing why the two images are different.
 *
 * @author <a href="mailto:vhardy@apache.org">Vincent Hardy</a>
 * @version $Id: ImageCompareTest.java 475477 2006-11-15 22:44:28Z cam $
 */
public class ImageCompareTest extends AbstractTest {

    public static final String ERROR_COULD_NOT_OPEN_IMAGE = "ImageCompareTest.error.could.not.open.image";

    public static final String ERROR_COULD_NOT_LOAD_IMAGE = "ImageCompareTest.error.could.not.load.image";

    public static final String ERROR_DIFFERENCES = "ImageCompareTest.error.differences";

    public static final String ERROR_WHILE_COMPARING_FILES = "ImageCompareTest.error.while.comparing.files";

    public static final String ENTRY_KEY_FIRST_IMAGE = "ImageCompareTest.entry.key.first.image";

    public static final String ENTRY_KEY_SECOND_IMAGE = "ImageCompareTest.entry.key.second.image";

    public static final String ENTRY_KEY_COMPARISON = "ImageCompareTest.entry.key.comparison";

    public static final String ENTRY_KEY_DIFFERENCE = "ImageCompareTest.entry.key.difference";

    public static final String ENTRY_KEY_IMAGE_URL = "ImageCompareTest.entry.key.image.url";

    public static final String IMAGE_TYPE_DIFFERENCE = "_diff";

    public static final String IMAGE_TYPE_COMPARISON = "_cmp";

    /**
     * Prefix for the temporary files created by Tests
     * of this class
     */
    public static final String TEMP_FILE_PREFIX = "ImageCompareTest";

    /**
     * Suffix for the temporary files created by 
     * Tests of this class
     */
    public static final String TEMP_FILE_SUFFIX = "";

    /**
     * URL for the first image to be compared.
     */
    protected String urlAStr;

    protected URL urlA;

    /**
     * URL for the second image to be compared
     */
    protected String urlBStr;

    protected URL urlB;

    /**
     * Resolves the input string as follows.
     * + First, the string is interpreted as a file description.
     *   If the file exists, then the file name is turned into
     *   a URL.
     * + Otherwise, the string is supposed to be a URL. If it
     *   is an invalid URL, an IllegalArgumentException is thrown.
     */
    protected URL resolveURL(String url) {
        File f = (new File(url)).getAbsoluteFile();
        if (f.exists()) {
            try {
                return f.toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException();
            }
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(url);
        }
    }

    /**
     * This test makes a binary comparison of the two images
     * (and not a pixel comparison). If the images are different,
     * the test generates a report containing the two images and
     * a delta images to help the user visualize the difference.
     *
     * @param urlA first image
     * @param urlB second image
     */
    public ImageCompareTest(String urlA, String urlB) {
        urlAStr = urlA;
        urlBStr = urlB;
    }

    protected void initURLs() {
        if (urlA == null) {
            throw new IllegalArgumentException();
        }
        if (urlB == null) {
            throw new IllegalArgumentException();
        }
        this.urlA = resolveURL(urlAStr);
        this.urlB = resolveURL(urlBStr);
    }

    public TestReport rumImpl() throws Exception {
        initURLs();
        InputStream streamA = null;
        try {
            streamA = new BufferedInputStream(urlA.openStream());
        } catch (IOException e) {
            return reportException(ERROR_COULD_NOT_OPEN_IMAGE, e);
        }
        InputStream streamB = null;
        try {
            streamB = new BufferedInputStream(urlB.openStream());
        } catch (IOException e) {
            return reportException(ERROR_COULD_NOT_OPEN_IMAGE, e);
        }
        boolean accurate = false;
        try {
            accurate = compare(streamA, streamB);
        } catch (IOException e) {
            TestReport report = reportException(ERROR_WHILE_COMPARING_FILES, e);
            report.addDescriptionEntry(ENTRY_KEY_FIRST_IMAGE, urlA.toString());
            report.addDescriptionEntry(ENTRY_KEY_SECOND_IMAGE, urlB.toString());
            return report;
        }
        if (accurate) {
            return reportSuccess();
        }
        BufferedImage imageA = getImage(urlA);
        if (imageA == null) {
            TestReport report = reportError(ERROR_COULD_NOT_LOAD_IMAGE);
            report.addDescriptionEntry(ENTRY_KEY_IMAGE_URL, urlA.toString());
            return report;
        }
        BufferedImage imageB = getImage(urlB);
        if (imageB == null) {
            TestReport report = reportError(ERROR_COULD_NOT_LOAD_IMAGE);
            report.addDescriptionEntry(ENTRY_KEY_IMAGE_URL, urlB.toString());
            return report;
        }
        BufferedImage diff = buildDiffImage(imageA, imageB);
        BufferedImage cmp = buildCompareImage(imageA, imageB);
        File tmpDiff = imageToFile(diff, IMAGE_TYPE_DIFFERENCE);
        File tmpCmp = imageToFile(cmp, IMAGE_TYPE_COMPARISON);
        TestReport report = reportError(ERROR_DIFFERENCES);
        report.addDescriptionEntry(ENTRY_KEY_COMPARISON, tmpCmp);
        report.addDescriptionEntry(ENTRY_KEY_DIFFERENCE, tmpDiff);
        return report;
    }

    protected BufferedImage buildCompareImage(BufferedImage ref, BufferedImage gen) {
        BufferedImage cmp = new BufferedImage(ref.getWidth() * 2, ref.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cmp.createGraphics();
        g.setPaint(Color.white);
        g.fillRect(0, 0, cmp.getWidth(), cmp.getHeight());
        g.drawImage(ref, 0, 0, null);
        g.translate(ref.getWidth(), 0);
        g.drawImage(gen, 0, 0, null);
        g.dispose();
        return cmp;
    }

    /**
     * Creates a temporary File into which the input image is
     * saved.
     */
    protected File imageToFile(BufferedImage img, String imageType) throws IOException {
        File imageFile = makeRandomFileName(imageType);
        imageFile.deleteOnExit();
        ImageWriter writer = ImageWriterRegistry.getInstance().getWriterFor("image/png");
        OutputStream out = new FileOutputStream(imageFile);
        try {
            writer.writeImage(img, out);
        } finally {
            out.close();
        }
        return imageFile;
    }

    /**
     * Creates a temporary File into which the input image is
     * saved.
     */
    protected File makeRandomFileName(String imageType) throws IOException {
        return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX + imageType, null);
    }

    /**
     * Builds a new BufferedImage that is the difference between the two input images
     */
    public static BufferedImage buildDiffImage(BufferedImage ref, BufferedImage gen) {
        BufferedImage diff = new BufferedImage(ref.getWidth(), ref.getHeight(), BufferedImage.TYPE_INT_ARGB);
        WritableRaster refWR = ref.getRaster();
        WritableRaster genWR = gen.getRaster();
        WritableRaster dstWR = diff.getRaster();
        boolean refPre = ref.isAlphaPremultiplied();
        if (!refPre) {
            ColorModel cm = ref.getColorModel();
            cm = GraphicsUtil.coerceData(refWR, cm, true);
            ref = new BufferedImage(cm, refWR, true, null);
        }
        boolean genPre = gen.isAlphaPremultiplied();
        if (!genPre) {
            ColorModel cm = gen.getColorModel();
            cm = GraphicsUtil.coerceData(genWR, cm, true);
            gen = new BufferedImage(cm, genWR, true, null);
        }
        int w = ref.getWidth();
        int h = ref.getHeight();
        int y, i, val;
        int[] refPix = null;
        int[] genPix = null;
        for (y = 0; y < h; y++) {
            refPix = refWR.getPixels(0, y, w, 1, refPix);
            genPix = genWR.getPixels(0, y, w, 1, genPix);
            for (i = 0; i < refPix.length; i++) {
                val = ((refPix[i] - genPix[i]) * 10) + 128;
                if ((val & 0xFFFFFF00) != 0) if ((val & 0x80000000) != 0) val = 0; else val = 255;
                genPix[i] = val;
            }
            dstWR.setPixels(0, y, w, 1, genPix);
        }
        if (!genPre) {
            ColorModel cm = gen.getColorModel();
            cm = GraphicsUtil.coerceData(genWR, cm, false);
        }
        if (!refPre) {
            ColorModel cm = ref.getColorModel();
            cm = GraphicsUtil.coerceData(refWR, cm, false);
        }
        return diff;
    }

    /**
     * Compare the two input streams
     */
    public static boolean compare(InputStream refStream, InputStream newStream) throws IOException {
        int b, nb;
        do {
            b = refStream.read();
            nb = newStream.read();
        } while (b != -1 && nb != -1 && b == nb);
        refStream.close();
        newStream.close();
        return (b == nb);
    }

    /**
     * Loads an image from a URL
     */
    protected BufferedImage getImage(URL url) {
        ImageTagRegistry reg = ImageTagRegistry.getRegistry();
        Filter filt = reg.readURL(new ParsedURL(url));
        if (filt == null) {
            return null;
        }
        RenderedImage red = filt.createDefaultRendering();
        if (red == null) {
            return null;
        }
        BufferedImage img = new BufferedImage(red.getWidth(), red.getHeight(), BufferedImage.TYPE_INT_ARGB);
        red.copyData(img.getRaster());
        return img;
    }
}
