package org.boblight4j.client.v4l;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;
import org.boblight4j.client.Client;
import org.boblight4j.client.FlagManager;
import org.boblight4j.client.grabber.AbstractPassiveGrabber;
import org.boblight4j.exception.BoblightException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.V4L4JRaster;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

/**
 * 
 * @author agebauer
 * 
 */
public class V4LImageGrabberImpl extends AbstractPassiveGrabber implements CaptureCallback {

    private static final Logger LOG = LoggerFactory.getLogger(V4LImageGrabberImpl.class);

    private final boolean blackBarDetection = true;

    private FrameGrabber fg;

    private FlagManagerV4l flagManager;

    private boolean stop;

    private VideoDevice vd;

    public V4LImageGrabberImpl(final Client client, final boolean sync, final int width, final int height) {
        super(client, sync, width, height);
    }

    @Override
    public final void cleanup() {
        this.fg.stopCapture();
        this.vd.releaseFrameGrabber();
    }

    /**
	 * Creates a scaled image.
	 * 
	 * @param originalImage
	 * @param scaledWidth
	 * @param scaledHeight
	 * @return
	 */
    final BufferedImage createResizedCopy(final BufferedImage originalImage, final int scaledWidth, final int scaledHeight) {
        LOG.info("resizing...");
        final BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = scaledBI.createGraphics();
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }

    final BufferedImage createResizedCopyJM(final BufferedImage originalImage, final int scaledWidth, final int scaledHeight) {
        try {
            final ImageInfo imageInfo = new ImageInfo();
            final MagickImage magickImage = new MagickImage(imageInfo);
            magickImage.scaleImage(scaledWidth, scaledHeight);
        } catch (final MagickException e) {
            LOG.error("", e);
        }
        LOG.info("resizing...");
        final BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = scaledBI.createGraphics();
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }

    @Override
    public void setup(final FlagManager flagManager) throws BoblightException {
        this.flagManager = (FlagManagerV4l) flagManager;
        try {
            this.vd = new VideoDevice(this.flagManager.device);
        } catch (final V4L4JException e) {
            throw new BoblightException(e);
        }
        try {
            this.fg = this.vd.getRawFrameGrabber(this.flagManager.width, this.flagManager.height, this.flagManager.getChannel(), 1);
        } catch (final V4L4JException e) {
            throw new BoblightException(e);
        }
        this.getClient().getLightsHolder().setScanRange(this.flagManager.width, this.flagManager.height);
        this.fg.setCaptureCallback(this);
        try {
            this.fg.startCapture();
        } catch (final V4L4JException e1) {
            throw new BoblightException(e1);
        }
    }

    @Override
    public final void exceptionReceived(final V4L4JException e) {
        LOG.warn("Error occurred during capturing.", e);
    }

    @Override
    public final void nextFrame(final VideoFrame frame) {
        final long diffCaptCurr = frame.getCaptureTime() / 1000 - System.currentTimeMillis();
        if (Math.abs(diffCaptCurr) > 25) {
            LOG.info("dropping frame " + frame.getSequenceNumber());
            frame.recycle();
            return;
        }
        final DataBuffer dataBuffer = frame.getDataBuffer();
        final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        final PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, this.fg.getWidth(), this.fg.getHeight(), 3, this.fg.getWidth() * 3, new int[] { 2, 1, 0 });
        final V4L4JRaster raster = new V4L4JRaster(sm, dataBuffer, new Point(0, 0));
        BufferedImage bufferedImage;
        if (cs != null) {
            bufferedImage = new BufferedImage(new ComponentColorModel(cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE), raster, false, null);
        } else {
            bufferedImage = null;
        }
        if (this.blackBarDetection) {
        }
        try {
            this.frameToBoblight(bufferedImage);
            this.getClient().sendRgb(false, null);
        } catch (final Exception e) {
            LOG.error("", e);
        }
        frame.recycle();
    }
}
