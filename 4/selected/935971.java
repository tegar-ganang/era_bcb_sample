package fr.inria.zvtm.glyphs;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import fr.inria.zvtm.engine.VirtualSpaceManager;
import fr.inria.zvtm.engine.Camera;
import fr.inria.zvtm.engine.LongPoint;
import fr.inria.zvtm.glyphs.Glyph;
import fr.inria.zvtm.glyphs.ClosedShape;
import fr.inria.zvtm.glyphs.VImage;
import fr.inria.zvtm.glyphs.RectangularShape;
import fr.inria.zvtm.glyphs.projection.RProjectedCoordsP;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.net.URL;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

/** Glyph encapsulating a PDFPage from <a href="https://pdf-renderer.dev.java.net/">SwingLabs' PDFRenderer</a>.
 *@author Emmanuel Pietriga
 */
public class ZPDFPageImg extends ZPDFPage {

    Image pageImage;

    private String filePath;

    private int pageNumber;

    /** For internal use. Made public for easier outside package subclassing. */
    public float trueCoef = 1.0f;

    /** For internal use. Made public for easier outside package subclassing. */
    public AffineTransform at;

    /**
	 * Create a new PDF page as a ZVTM glyph, rendered at a resolution
	 * that matches the default scale for that page.
	 * @throws IOException
	 */
    public ZPDFPageImg(long x, long y, int z, String filePath, int pageNumber) throws IOException {
        this.filePath = filePath;
        this.pageNumber = pageNumber;
        File file = new File(filePath);
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        PDFFile pdfFile = new PDFFile(buf);
        PDFPage page = pdfFile.getPage(pageNumber);
        initZPDFPageImg(x, y, z, page);
    }

    /** Instantiate a PDF page as a ZVTM glyph, rendered at a resolution that matches the default scale for that page.
	 *@param page the PDF page from pdf-renderer
	 */
    private void initZPDFPageImg(long x, long y, int z, PDFPage page) {
        vx = x;
        vy = y;
        vz = z;
        Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
        pageImage = page.getImage(rect.width, rect.height, rect, null, true, true);
        vw = Math.round(rect.width / 2.0);
        vh = Math.round(rect.height / 2.0);
        if (vw == 0 && vh == 0) {
            ar = 1.0f;
        } else {
            ar = (float) vw / (float) vh;
        }
        computeSize();
        orient = 0;
    }

    /** Instantiate a PDF page as a ZVTM glyph, rendered at a resolution that matches the default scale for that page.
	 *@param page the PDF page from pdf-renderer
	 *@param detailFactor multiplication factor applied to compute the actual width and height of the bitmap image in which to render the page, taking the default rendering scale as a basis
     *@param scale scaleFactor in virtual space w.r.t specified image size (default is 1.0)
	 */
    private ZPDFPageImg(long x, long y, int z, PDFPage page, float detailFactor, double scale) {
        vx = x;
        vy = y;
        vz = z;
        Rectangle rect = new Rectangle(0, 0, (int) (page.getBBox().getWidth()), (int) (page.getBBox().getHeight()));
        pageImage = page.getImage((int) (detailFactor * rect.width), (int) (detailFactor * rect.height), rect, null, true, true);
        vw = Math.round(detailFactor * rect.width / 2.0);
        vh = Math.round(detailFactor * rect.height / 2.0);
        if (vw == 0 && vh == 0) {
            ar = 1.0f;
        } else {
            ar = (float) vw / (float) vh;
        }
        computeSize();
        orient = 0;
        scaleFactor = (float) scale;
    }

    /** Instantiate a PDF page as a ZVTM glyph, rendered at a resolution that matches the default scale for that page.
	 *@param page the PDF page from pdf-renderer
	 *@param w width of bitmap image in which to render the page
	 *@param h height of bitmap image in which to render the page
     *@param scale scaleFactor in virtual space w.r.t specified image size (default is 1.0)
	 */
    private ZPDFPageImg(long x, long y, int z, PDFPage page, int w, int h, double scale) {
        vx = x;
        vy = y;
        vz = z;
        Rectangle rect = new Rectangle(0, 0, w, h);
        pageImage = page.getImage(rect.width, rect.height, rect, null, true, true);
        vw = Math.round(rect.width / 2.0);
        vh = Math.round(rect.height / 2.0);
        if (vw == 0 && vh == 0) {
            ar = 1.0f;
        } else {
            ar = (float) vw / (float) vh;
        }
        computeSize();
        orient = 0;
        scaleFactor = (float) scale;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    /** For internal use. Made public for easier outside package subclassing. */
    public Object interpolationMethod = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

    /** Specify how image should be interpolated when drawn at a scale different from its original scale.
        *@param im one of java.awt.RenderingHints.{VALUE_INTERPOLATION_NEAREST_NEIGHBOR,VALUE_INTERPOLATION_BILINEAR,VALUE_INTERPOLATION_BICUBIC} ; default is VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        */
    public void setInterpolationMethod(Object im) {
        interpolationMethod = im;
    }

    /** Get information about how image should be interpolated when drawn at a scale different from its original scale.
        *@return one of java.awt.RenderingHints.{VALUE_INTERPOLATION_NEAREST_NEIGHBOR,VALUE_INTERPOLATION_BILINEAR,VALUE_INTERPOLATION_BICUBIC} ; default is VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        */
    public Object getInterpolationMethod() {
        return interpolationMethod;
    }

    public void draw(Graphics2D g, int vW, int vH, int i, Stroke stdS, AffineTransform stdT, int dx, int dy) {
        if ((pc[i].cw > 1) && (pc[i].ch > 1)) {
            if (zoomSensitive) {
                trueCoef = scaleFactor * coef;
            } else {
                trueCoef = scaleFactor;
            }
            if (Math.abs(trueCoef - 1.0f) < 0.01f) {
                trueCoef = 1.0f;
            }
            if (trueCoef != 1.0f) {
                at = AffineTransform.getTranslateInstance(dx + pc[i].cx - pc[i].cw, dy + pc[i].cy - pc[i].ch);
                g.setTransform(at);
                if (interpolationMethod != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, interpolationMethod);
                    g.drawImage(pageImage, AffineTransform.getScaleInstance(trueCoef, trueCoef), null);
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                } else {
                    g.drawImage(pageImage, AffineTransform.getScaleInstance(trueCoef, trueCoef), null);
                }
                g.setTransform(stdT);
                if ((drawBorder == 1 && pc[i].prevMouseIn) || drawBorder == 2) {
                    g.setColor(borderColor);
                    g.drawRect(dx + pc[i].cx - pc[i].cw, dy + pc[i].cy - pc[i].ch, 2 * pc[i].cw - 1, 2 * pc[i].ch - 1);
                }
            } else {
                if (interpolationMethod != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, interpolationMethod);
                    g.drawImage(pageImage, dx + pc[i].cx - pc[i].cw, dy + pc[i].cy - pc[i].ch, null);
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                } else {
                    g.drawImage(pageImage, dx + pc[i].cx - pc[i].cw, dy + pc[i].cy - pc[i].ch, null);
                }
                if ((drawBorder == 1 && pc[i].prevMouseIn) || drawBorder == 2) {
                    g.setColor(borderColor);
                    g.drawRect(dx + pc[i].cx - pc[i].cw, dy + pc[i].cy - pc[i].ch, 2 * pc[i].cw - 1, 2 * pc[i].ch - 1);
                }
            }
        } else {
            g.setColor(this.borderColor);
            g.fillRect(dx + pc[i].cx, dy + pc[i].cy, 1, 1);
        }
    }

    public void drawForLens(Graphics2D g, int vW, int vH, int i, Stroke stdS, AffineTransform stdT, int dx, int dy) {
        if ((pc[i].lcw > 1) && (pc[i].lch > 1)) {
            if (zoomSensitive) {
                trueCoef = scaleFactor * coef;
            } else {
                trueCoef = scaleFactor;
            }
            if (Math.abs(trueCoef - 1.0f) < 0.01f) {
                trueCoef = 1.0f;
            }
            if (trueCoef != 1.0f) {
                g.setTransform(AffineTransform.getTranslateInstance(dx + pc[i].lcx - pc[i].lcw, dy + pc[i].lcy - pc[i].lch));
                if (interpolationMethod != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, interpolationMethod);
                    g.drawImage(pageImage, AffineTransform.getScaleInstance(trueCoef, trueCoef), null);
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                } else {
                    g.drawImage(pageImage, AffineTransform.getScaleInstance(trueCoef, trueCoef), null);
                }
                g.setTransform(stdT);
                if ((drawBorder == 1 && pc[i].prevMouseIn) || drawBorder == 2) {
                    g.setColor(borderColor);
                    g.drawRect(dx + pc[i].lcx - pc[i].lcw, dy + pc[i].lcy - pc[i].lch, 2 * pc[i].lcw - 1, 2 * pc[i].lch - 1);
                }
            } else {
                if (interpolationMethod != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, interpolationMethod);
                    g.drawImage(pageImage, dx + pc[i].lcx - pc[i].lcw, dy + pc[i].lcy - pc[i].lch, null);
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                } else {
                    g.drawImage(pageImage, dx + pc[i].lcx - pc[i].lcw, dy + pc[i].lcy - pc[i].lch, null);
                }
                if ((drawBorder == 1 && pc[i].prevMouseIn) || drawBorder == 2) {
                    g.setColor(borderColor);
                    g.drawRect(dx + pc[i].lcx - pc[i].lcw, dy + pc[i].lcy - pc[i].lch, 2 * pc[i].lcw - 1, 2 * pc[i].lch - 1);
                }
            }
        } else {
            g.setColor(this.borderColor);
            g.fillRect(dx + pc[i].lcx, dy + pc[i].lcy, 1, 1);
        }
    }

    public Object clone() {
        return null;
    }
}
