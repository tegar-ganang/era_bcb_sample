package com.ebixio.virtmus.imgsrc;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MainApp.Rotation;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.VirtMusKernel;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.openide.util.Exceptions;

/**
 *
 * @author GBURCA
 */
@XStreamAlias("pdfImg")
public class PdfRender extends ImgSrc {

    public int pageNum;

    transient File tmpImgFile = null;

    public PdfRender(File sourceFile, int pageNum) {
        super(sourceFile);
        this.pageNum = pageNum;
    }

    @Override
    public Dimension getDimension() {
        PDFPage pdfPage = getPdfPage();
        Dimension dim = getLargestDisplay();
        int max = Math.max(dim.width, dim.height);
        dim = pdfPage.getUnstretchedSize(max, max, null);
        int rotation = pdfPage.getRotation();
        List<PDFImage> imgs = pdfPage.getPageImages();
        if (imgs.size() == 1) {
            PDFImage img = imgs.get(0);
            if (rotation == 90 || rotation == 270) {
                return new Dimension(img.getHeight(), img.getWidth());
            } else {
                return new Dimension(img.getWidth(), img.getHeight());
            }
        }
        return dim;
    }

    public Dimension getLargestDisplay() {
        Dimension[] dims = Utils.getScreenSizes();
        int biggest = -1, idx = -1;
        for (int i = 0; i < dims.length; i++) {
            if (dims[i].width * dims[i].height > biggest) {
                biggest = dims[i].width * dims[i].height;
                idx = i;
            }
        }
        Dimension dim = dims[idx];
        return dim;
    }

    @Override
    public synchronized BufferedImage getImage(Dimension containerSize, Rotation rotation, boolean fillSize, MusicPage page) {
        RenderedOp srcImg, destImg = null;
        Rectangle destSize;
        GraphicsEnvironment graphEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphDevice = graphEnv.getDefaultScreenDevice();
        GraphicsConfiguration graphicConf = graphDevice.getDefaultConfiguration();
        System.gc();
        BufferedImage result = graphicConf.createCompatibleImage(containerSize.width, containerSize.height, Transparency.OPAQUE);
        Graphics2D g = result.createGraphics();
        RenderingHints renderingHints = new RenderingHints(RenderingHintsKeyExt.KEY_BUFFERED_IMAGE, new WeakReference<BufferedImage>(result));
        g.addRenderingHints(renderingHints);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, result.getWidth(), result.getHeight());
        g.setColor(Color.WHITE);
        RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        RenderingHints interpHints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        AffineTransform origXform = g.getTransform();
        g.setTransform(rotation.getTransform(containerSize.getSize()));
        switch(rotation) {
            case Clockwise_90:
            case Clockwise_270:
                g.setRenderingHints(interpHints);
                destSize = new Rectangle(containerSize.height, containerSize.width);
                break;
            default:
                destSize = new Rectangle(containerSize);
                break;
        }
        float scale = (float) Utils.scaleProportional(destSize, new Rectangle(getDimension()));
        srcImg = getFullRenderedOp();
        if (scale == 1.0) {
            destImg = srcImg;
        } else if (scale < 1.0 && Math.min(destSize.height, destSize.width) < 600) {
            destImg = JAI.create("SubsampleAverage", srcImg, (double) scale, (double) scale, qualityHints);
        } else {
            if (scale < 1.0) {
                KernelJAI k;
                k = VirtMusKernel.getKernel(1, 1, 6);
                destImg = JAI.create("Convolve", srcImg, k);
                srcImg = destImg;
            } else {
                scale = Math.min(scale, 2.0F);
            }
            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
            scale = Math.min(scale, 2.0F);
            ParameterBlock params = new ParameterBlock();
            params.addSource(srcImg);
            params.add(scale);
            params.add(scale);
            params.add(0.0F);
            params.add(0.0F);
            params.add(interp);
            destImg = JAI.create("scale", params);
        }
        srcImg = destImg;
        Point destPt;
        if (fillSize) {
            destPt = Utils.centerItem(destSize, Utils.scale(new Rectangle(getDimension()), scale));
        } else {
            destPt = new Point(0, 0);
        }
        AffineTransform newXform = g.getTransform();
        newXform.concatenate(AffineTransform.getTranslateInstance(destPt.x, destPt.y));
        g.setTransform(newXform);
        g.drawImage(srcImg.getAsBufferedImage(), 0, 0, null);
        newXform.concatenate(AffineTransform.getScaleInstance(scale, scale));
        g.setTransform(newXform);
        if (page != null) page.paintAnnotations(g);
        g.setTransform(origXform);
        Dimension dim = srcImg.getBounds().getSize();
        srcImg.dispose();
        g.dispose();
        if (fillSize) {
            return result;
        } else {
            return result.getSubimage(0, 0, dim.width, dim.height);
        }
    }

    @Override
    public PlanarImage getFullImg() {
        return PlanarImage.wrapRenderedImage(getFullRenderedOp());
    }

    private RenderedOp getFullRenderedOp() {
        RenderedOp rend = JAI.create("AWTImage", getPageImage());
        return rend;
    }

    private Image getPageImage() {
        PDFPage pdfPage = getPdfPage();
        Rectangle2D bbox = pdfPage.getBBox();
        Rectangle rect1 = new Rectangle((int) bbox.getX(), (int) bbox.getY(), (int) bbox.getWidth(), (int) bbox.getHeight());
        Dimension dim = getDimension();
        Rectangle rect = new Rectangle(0, 0, dim.width, dim.height);
        int rotation = pdfPage.getRotation();
        if (rotation == 90 || rotation == 270) {
            rect1 = new Rectangle(0, 0, rect1.height, rect1.width);
            bbox = new Rectangle2D.Double(bbox.getX(), bbox.getY(), bbox.getHeight(), bbox.getWidth());
        }
        MainApp.log("ImgSizes: pg:" + pageNum + "  rot:" + rotation + "   " + rect1.getSize().toString() + "   " + rect.getSize().toString());
        Image img = pdfPage.getImage((int) rect.width, (int) rect.height, bbox, null, true, true);
        return img;
    }

    private PDFPage getPdfPage() {
        try {
            RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdfFile = new PDFFile(buf);
            PDFPage pdfPage = pdfFile.getPage(getPageNum() + 1, true);
            return pdfPage;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public File createImageFile() {
        try {
            tmpImgFile = File.createTempFile("VirtMus", ".jpg");
            ImageIO.write(getFullRenderedOp(), "jpg", tmpImgFile);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return tmpImgFile;
    }

    @Override
    public void destroyImageFile() {
        try {
            if (tmpImgFile != null) tmpImgFile.delete();
            tmpImgFile = null;
        } catch (Exception e) {
        }
    }

    /**
     * @return the pageNum
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * @param pageNum the pageNum to set
     */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    @Override
    public String getName() {
        return super.getName() + " p" + (pageNum + 1);
    }
}
