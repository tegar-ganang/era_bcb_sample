package org.photovault.image;

import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import javax.media.jai.Histogram;
import javax.media.jai.IHSColorSpace;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandCombineDescriptor;
import javax.media.jai.operator.HistogramDescriptor;
import javax.media.jai.operator.LookupDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.RenderableDescriptor;
import javax.media.jai.operator.TranslateDescriptor;

/**
 PhotovaultImage is a facade fro Photovault imaging pipeline. It is abstract 
 class, different image providers must derive their own classes from it.
 */
public abstract class PhotovaultImage {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PhotovaultImage.class.getName());

    /** Creates a new instance of PhotovaultImage */
    public PhotovaultImage() {
    }

    File f = null;

    /**
     * Get aperture (f-stop) used when shooting the image
     * 
     * @return F-stop number reported by dcraw
     */
    public abstract double getAperture();

    /**
     * Get the camera mode used to shoot the image
     * 
     * @return Camera model reported by dcraw
     */
    public abstract String getCamera();

    /**
     Get the original image
     @deprecated Use getRenderedImage instead
     */
    public abstract RenderableOp getCorrectedImage(int minWidth, int minHeight, boolean isLowQualityAllowed);

    /**
     Get the original image
     @deprecated Use getRenderedImage instead. TODO: This should be changed to 
     private, but some test cases depend on this.
     */
    public RenderableOp getCorrectedImage() {
        return getCorrectedImage(Integer.MAX_VALUE, Integer.MAX_VALUE, false);
    }

    /**
     Get the sample model of the corrected image. This must be overriden by 
     derived classes
     */
    public abstract SampleModel getCorrectedImageSampleModel();

    /**
     Get the color model of the corrected image. This must be overriden by 
     derived classes
     */
    public abstract ColorModel getCorrectedImageColorModel();

    RenderableOp previousCorrectedImage = null;

    RenderableOp cropped = null;

    RenderableOp saturated = null;

    RenderableOp colorCorrected = null;

    RenderableOp rotatedImage = null;

    RenderableOp croppedImage = null;

    RenderableOp xformCroppedImage = null;

    /**
     The multiplyChan operation used to adjust saturation
     */
    RenderableOp saturatedIhsImage = null;

    RenderableOp originalImage = null;

    /**
     Last rendering created.
     */
    RenderedImage lastRendering = null;

    protected void buildPipeline(RenderableOp original) {
        originalImage = original;
        cropped = getCropped(original);
        cropped.setProperty("org.photovault.opname", "cropped_image");
        colorCorrected = getColorCorrected(cropped);
        colorCorrected.setProperty("org.photovault.opname", "color_corrected_rgb_image");
        saturated = getSaturated(colorCorrected);
        previousCorrectedImage = original;
    }

    /**
     Objects that should be notified about new renderings.
     */
    Set<ImageRenderingListener> renderingListeners = new HashSet<ImageRenderingListener>();

    /**
     Request that a given {@link ImageRenderingListener} should be notified of
     changes to this image.
     */
    public void addRenderingListener(ImageRenderingListener l) {
        renderingListeners.add(l);
    }

    /**
     Request that a given {@link ImageRenderingListener} should not anymore be 
     notified of changes to this image.
     */
    public void removeRenderingListener(ImageRenderingListener l) {
        renderingListeners.remove(l);
    }

    protected void fireNewRenderingEvent() {
        for (ImageRenderingListener l : renderingListeners) {
            l.newRenderingCreated(this);
        }
    }

    /**
     Get the image, adjusted according to current parameters and scaled to a 
     specified resolution.
     @param maxWidth Maximum width of the image in pixels. Image aspect ratio is
     preserved so actual width can be smaller than this.
     @param maxHeight Maximum height of the image in pixels. Image aspect ratio is
     preserved so actual height can be smaller than this.
     @param isLowQualityAllowed Specifies whether image quality can be traded off 
     for speed/memory consumpltion optimizations.
     @return The image as RenderedImage
     */
    public RenderedImage getRenderedImage(int maxWidth, int maxHeight, boolean isLowQualityAllowed) {
        double rotRad = rot * Math.PI / 180.0;
        double rotSin = Math.abs(Math.sin(rotRad));
        double rotCos = Math.abs(Math.cos(rotRad));
        double rotW = rotCos * getWidth() + rotSin * getHeight();
        double rotH = rotSin * getWidth() + rotCos * getHeight();
        double cropW = rotW * (cropMaxX - cropMinX);
        double cropH = rotH * (cropMaxY - cropMinY);
        double scaleW = maxWidth / cropW;
        double scaleH = maxHeight / cropH;
        double scale = Math.min(scaleW, scaleH);
        double needW = getWidth() * scale;
        double needH = getHeight() * scale;
        RenderableOp original = getCorrectedImage((int) needW, (int) needH, isLowQualityAllowed);
        if (previousCorrectedImage != original) {
            buildPipeline(original);
        }
        int renderingWidth = maxWidth;
        int renderingHeight = (int) (renderingWidth * cropH / cropW);
        if (renderingHeight > maxHeight) {
            renderingHeight = maxHeight;
            renderingWidth = (int) (renderingHeight * cropW / cropH);
        }
        RenderingHints hints = new RenderingHints(null);
        hints.put(JAI.KEY_INTERPOLATION, new InterpolationBilinear());
        RenderedImage rendered = null;
        if (lastRendering != null && lastRendering instanceof PlanarImage) {
            ((PlanarImage) lastRendering).dispose();
            System.gc();
        }
        if (saturated != null) {
            rendered = saturated.createScaledRendering(renderingWidth, renderingHeight, hints);
            lastRendering = rendered;
        } else {
            rendered = colorCorrected.createScaledRendering(renderingWidth, renderingHeight, hints);
            lastRendering = rendered;
        }
        fireNewRenderingEvent();
        return rendered;
    }

    /**
     Get the image, adjusted according to current parameters and scaled with a 
     specified factor.
     @param scale Scaling compared to original image file.
     @param isLowQualityAllowed Specifies whether image quality can be traded off 
     for speed/memory consumpltion optimizations.
     @return The image as RenderedImage
     */
    public RenderedImage getRenderedImage(double scale, boolean isLowQualityAllowed) {
        double rotRad = rot * Math.PI / 180.0;
        double rotSin = Math.abs(Math.sin(rotRad));
        double rotCos = Math.abs(Math.cos(rotRad));
        double rotW = rotCos * getWidth() + rotSin * getHeight();
        double rotH = rotSin * getWidth() + rotCos * getHeight();
        double cropW = rotW * (cropMaxX - cropMinX);
        double cropH = rotH * (cropMaxY - cropMinY);
        RenderableOp original = getCorrectedImage((int) (getWidth() * scale), (int) (getHeight() * scale), isLowQualityAllowed);
        if (previousCorrectedImage != original) {
            buildPipeline(original);
        }
        RenderingHints hints = new RenderingHints(null);
        hints.put(JAI.KEY_INTERPOLATION, new InterpolationBilinear());
        if (lastRendering != null && lastRendering instanceof PlanarImage) {
            ((PlanarImage) lastRendering).dispose();
            System.gc();
        }
        RenderedImage rendered = null;
        if (saturated != null) {
            rendered = saturated.createScaledRendering((int) (scale * cropW), (int) (scale * cropH), hints);
            lastRendering = rendered;
        } else {
            rendered = colorCorrected.createScaledRendering((int) (scale * cropW), (int) (scale * cropH), hints);
            lastRendering = rendered;
        }
        fireNewRenderingEvent();
        return rendered;
    }

    public static final String HISTOGRAM_RGB_CHANNELS = "rgb";

    public static final String HISTOGRAM_IHS_CHANNELS = "ihs";

    /**
     Get a histogram of a specific phase of imaging pipeline
     @param histType What histogram to retrieve. Possible values in PhotovaultImage
     are
     <ul>
     <li>HISTOGRAM_RGB_CHANNELS - sRGB color space histogam calculated after 
     cropping but before color correction is applied</li>
     <li>HISTOGRAM_IHS_CHANNELS - IHS color space histogram calculated after RGB 
     color corrections but before saturation correction</li>
     </ul>
     Derived classes may add additional histograms
     @return The histogram from last rendering. TODO: Currently returns 
     <code>null</code> if no rendering has been made.
     */
    public Histogram getHistogram(String histType) {
        Histogram ret = null;
        RenderedImage src = null;
        if (histType.equals(HISTOGRAM_RGB_CHANNELS)) {
            src = findNamedRendering(lastRendering, "cropped_image");
        } else if (histType.equals(HISTOGRAM_IHS_CHANNELS)) {
            src = findNamedRendering(lastRendering, "color_corrected_ihs_image");
        }
        if (src != null) {
            int[] componentSizes = src.getColorModel().getComponentSize();
            int numBins[] = new int[componentSizes.length];
            double lowValue[] = new double[componentSizes.length];
            double highValue[] = new double[componentSizes.length];
            for (int n = 0; n < componentSizes.length; n++) {
                numBins[n] = 1 << componentSizes[n];
                lowValue[n] = 0.0;
                highValue[n] = (double) numBins[n] - 1.0;
            }
            RenderedOp histOp = HistogramDescriptor.create(src, null, Integer.valueOf(1), Integer.valueOf(1), numBins, lowValue, highValue, null);
            ret = (Histogram) histOp.getProperty("histogram");
        }
        return ret;
    }

    public void dispose() {
        if (lastRendering != null && lastRendering instanceof PlanarImage) {
            ((PlanarImage) lastRendering).dispose();
        }
        System.gc();
    }

    /**
     Get width of the original image
     @return width in pixels
     */
    public abstract int getWidth();

    /**
     Get height of the original image
     @return height in pixels
     */
    public abstract int getHeight();

    /**
     Amount of rotation that is applied to the image.
     */
    double rot = 0.0;

    /**
     Set the rotation applied to original image
     @param r Rotation in degrees. The image is rotated clockwise
     */
    public void setRotation(double r) {
        rot = r;
        applyRotCrop();
    }

    /**
     Get current rotation
     @return Rotation in degrees, clockwise.
     */
    public double getRotation() {
        return rot;
    }

    double saturation = 1.0;

    /**
     Set saturation for the image
     @param s New saturation
     */
    public void setSaturation(double s) {
        saturation = s;
        ColorCurve satCurve = new ColorCurve();
        satCurve.addPoint(0.0, 0.0);
        if (s < 1.0) {
            satCurve.addPoint(1.0, s);
        } else {
            satCurve.addPoint(1.0 / s, 1.0);
        }
        ChannelMapOperationFactory f = new ChannelMapOperationFactory(channelMap);
        f.setChannelCurve("saturation", satCurve);
        channelMap = f.create();
        if (saturatedIhsImage != null) {
            saturatedIhsImage.setParameter(createSaturationMappingLUT(), 0);
        }
    }

    /**
     Get current saturation multiplier for the image
     @return saturation
     */
    public double getSaturation() {
        return saturation;
    }

    /**
     Mapping of color channels
     */
    ChannelMapOperation channelMap = null;

    /**
     *     Set color channel mapping
     * 
     * @param cm New mapping of color channels
     */
    public void setColorAdjustment(ChannelMapOperation cm) {
        this.channelMap = cm;
        applyColorMapping();
    }

    /**
     Get current color channel mapping
     */
    public ChannelMapOperation getColorAdjustment() {
        return channelMap;
    }

    /**
     Set the lookup tables in JAI pipeline to match color adjustment
     */
    protected void applyColorMapping() {
        if (channelMap == null || colorCorrected == null) {
            return;
        }
        LookupTableJAI jailut = createColorMappingLUT();
        colorCorrected.setParameter(jailut, 0);
        if (saturatedIhsImage != null) {
            saturatedIhsImage.setParameter(createSaturationMappingLUT(), 0);
        }
    }

    /**
     Create a color mapping LUT based on current color channel mapping.
     @return the created LUT. If not channel mapping is specified, returns an
     identity mapping.
     */
    private LookupTableJAI createColorMappingLUT() {
        ColorModel colorModel = this.getCorrectedImageColorModel();
        ColorSpace colorSpace = colorModel.getColorSpace();
        int[] componentSizes = colorModel.getComponentSize();
        ColorCurve valueCurve = null;
        ColorCurve[] componentCurves = new ColorCurve[componentSizes.length];
        boolean[] applyValueCurve = new boolean[componentSizes.length];
        for (int n = 0; n < componentSizes.length; n++) {
            applyValueCurve[n] = false;
        }
        if (channelMap != null) {
            valueCurve = channelMap.getChannelCurve("value");
            switch(colorSpace.getType()) {
                case ColorSpace.TYPE_GRAY:
                    componentCurves[0] = valueCurve;
                    break;
                case ColorSpace.TYPE_RGB:
                    componentCurves[0] = channelMap.getChannelCurve("red");
                    componentCurves[1] = channelMap.getChannelCurve("green");
                    componentCurves[2] = channelMap.getChannelCurve("blue");
                    applyValueCurve[0] = true;
                    applyValueCurve[1] = true;
                    applyValueCurve[2] = true;
                    break;
                default:
                    break;
            }
        }
        if (valueCurve == null) {
            valueCurve = new ColorCurve();
        }
        LookupTableJAI jailut = null;
        if (componentSizes[0] == 8) {
            byte[][] lut = new byte[componentSizes.length][256];
            double dx = 1.0 / 256.0;
            for (int band = 0; band < colorModel.getNumComponents(); band++) {
                for (int n = 0; n < lut[band].length; n++) {
                    double x = dx * n;
                    double val = x;
                    if (band < componentCurves.length && componentCurves[band] != null) {
                        val = componentCurves[band].getValue(val);
                    }
                    if (band < applyValueCurve.length && applyValueCurve[band]) {
                        val = valueCurve.getValue(val);
                    }
                    val = Math.max(0.0, Math.min(val, 1.0));
                    lut[band][n] = (byte) ((lut[band].length - 1) * val);
                }
            }
            jailut = new LookupTableJAI(lut);
        } else if (componentSizes[0] == 16) {
            short[][] lut = new short[componentSizes.length][0x10000];
            double dx = 1.0 / 65536.0;
            for (int band = 0; band < colorModel.getNumComponents(); band++) {
                for (int n = 0; n < lut[band].length; n++) {
                    double x = dx * n;
                    double val = x;
                    if (band < componentCurves.length && componentCurves[band] != null) {
                        val = componentCurves[band].getValue(val);
                    }
                    if (band < applyValueCurve.length && applyValueCurve[band]) {
                        val = valueCurve.getValue(val);
                    }
                    val = Math.max(0.0, Math.min(val, 1.0));
                    lut[band][n] = (short) ((lut[band].length - 1) * val);
                }
            }
            jailut = new LookupTableJAI(lut, true);
        } else {
            log.error("Unsupported data type with with = " + componentSizes[0]);
        }
        return jailut;
    }

    /**
     Create lookup table for saturation correction. The operation is done 
     in IHS color space, so the lookup table will contain identity mapping
     for intensity & hue channels and map based "saturation" curve from channelMap
     for saturation. If "saturation curve is nonexistent, use identity mapping for 
     saturation as well.
     
     */
    private LookupTableJAI createSaturationMappingLUT() {
        ColorModel colorModel = this.getCorrectedImageColorModel();
        int[] componentSizes = colorModel.getComponentSize();
        ColorCurve satCurve = null;
        if (channelMap != null) {
            satCurve = channelMap.getChannelCurve("saturation");
        }
        if (satCurve == null) {
            satCurve = new ColorCurve();
        }
        LookupTableJAI jailut = null;
        if (componentSizes[0] == 8) {
            byte[][] lut = new byte[componentSizes.length][256];
            double dx = 1.0 / 256.0;
            for (int band = 0; band < componentSizes.length; band++) {
                for (int n = 0; n < lut[band].length; n++) {
                    double x = dx * n;
                    double val = x;
                    if (band == 2) {
                        val = satCurve.getValue(val);
                    }
                    val = Math.max(0.0, Math.min(val, 1.0));
                    lut[band][n] = (byte) ((lut[band].length - 1) * val);
                }
            }
            jailut = new LookupTableJAI(lut);
        } else if (componentSizes[0] == 16) {
            short[][] lut = new short[componentSizes.length][0x10000];
            double dx = 1.0 / 65536.0;
            for (int band = 0; band < componentSizes.length; band++) {
                for (int n = 0; n < lut[band].length; n++) {
                    double x = dx * n;
                    double val = x;
                    if (band == 2) {
                        val = satCurve.getValue(val);
                    }
                    val = Math.max(0.0, Math.min(val, 1.0));
                    lut[band][n] = (short) ((lut[band].length - 1) * val);
                }
            }
            jailut = new LookupTableJAI(lut, true);
        } else {
            log.error("Unsupported data type with with = " + componentSizes[0]);
        }
        return jailut;
    }

    double cropMinX = 0.0;

    double cropMinY = 0.0;

    double cropMaxX = 1.0;

    double cropMaxY = 1.0;

    /**
     Set new crop bounds for the image. Crop bounds are applied after rotation,
     so that top left corner is (0, 0) and bottom right corner (1, 1)
     @param c New crop bounds
     */
    public void setCropBounds(Rectangle2D c) {
        cropMinX = Math.min(1.0, Math.max(0.0, c.getMinX()));
        cropMinY = Math.min(1.0, Math.max(0.0, c.getMinY()));
        cropMaxX = Math.min(1.0, Math.max(0.0, c.getMaxX()));
        cropMaxY = Math.min(1.0, Math.max(0.0, c.getMaxY()));
        if (cropMaxX - cropMinX <= 0.0) {
            double tmp = cropMaxX;
            cropMaxX = cropMinX;
            cropMinX = tmp;
        }
        if (cropMaxY - cropMinY <= 0.0) {
            double tmp = cropMaxY;
            cropMaxY = cropMinY;
            cropMinY = tmp;
        }
        applyRotCrop();
    }

    /**
     Get the current crop bounds
     @return Crop bounds
     */
    public Rectangle2D getCropBounds() {
        return new Rectangle2D.Double(cropMinX, cropMinY, cropMaxX - cropMinX, cropMaxY - cropMinY);
    }

    protected RenderableOp getCropped(RenderableOp uncroppedImage) {
        float origWidth = uncroppedImage.getWidth();
        float origHeight = uncroppedImage.getHeight();
        AffineTransform xform = org.photovault.image.ImageXform.getRotateXform(rot, origWidth, origHeight);
        ParameterBlockJAI rotParams = new ParameterBlockJAI("affine");
        rotParams.addSource(uncroppedImage);
        rotParams.setParameter("transform", xform);
        rotParams.setParameter("interpolation", Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        RenderingHints hints = new RenderingHints(null);
        hints.put(JAI.KEY_INTERPOLATION, new InterpolationBilinear());
        rotatedImage = JAI.createRenderable("affine", rotParams, hints);
        ParameterBlockJAI cropParams = new ParameterBlockJAI("crop");
        cropParams.addSource(rotatedImage);
        float cropWidth = (float) (cropMaxX - cropMinX);
        cropWidth = (cropWidth > 0.000001) ? cropWidth : 0.000001f;
        float cropHeight = (float) (cropMaxY - cropMinY);
        cropHeight = (cropHeight > 0.000001) ? cropHeight : 0.000001f;
        float cropX = (float) (rotatedImage.getMinX() + cropMinX * rotatedImage.getWidth());
        float cropY = (float) (rotatedImage.getMinY() + cropMinY * rotatedImage.getHeight());
        float cropW = cropWidth * rotatedImage.getWidth();
        float cropH = cropHeight * rotatedImage.getHeight();
        Rectangle2D.Float cropRect = new Rectangle2D.Float(cropX, cropY, cropW, cropH);
        Rectangle2D.Float srcRect = new Rectangle2D.Float(rotatedImage.getMinX(), rotatedImage.getMinY(), rotatedImage.getWidth(), rotatedImage.getHeight());
        if (!srcRect.contains(cropRect)) {
            Rectangle2D.intersect(srcRect, cropRect, cropRect);
            if (!srcRect.contains(cropRect)) {
                cropX = rotatedImage.getMinX();
                cropY = rotatedImage.getMinY();
                cropW = rotatedImage.getWidth();
                cropH = rotatedImage.getHeight();
            }
        }
        cropParams.setParameter("x", cropX);
        cropParams.setParameter("y", cropY);
        cropParams.setParameter("width", cropW);
        cropParams.setParameter("height", cropH);
        croppedImage = JAI.createRenderable("crop", cropParams, hints);
        ParameterBlockJAI pbXlate = new ParameterBlockJAI("translate");
        pbXlate.addSource(croppedImage);
        pbXlate.setParameter("xTrans", (float) (-croppedImage.getMinX()));
        pbXlate.setParameter("yTrans", (float) (-croppedImage.getMinY()));
        xformCroppedImage = JAI.createRenderable("translate", pbXlate);
        return xformCroppedImage;
    }

    /**
     Apply the current rotation & cropping settings to JAi image pipeline.
     */
    protected void applyRotCrop() {
        if (originalImage == null) {
            return;
        }
        float origWidth = originalImage.getWidth();
        float origHeight = originalImage.getHeight();
        AffineTransform xform = org.photovault.image.ImageXform.getRotateXform(rot, origWidth, origHeight);
        ParameterBlockJAI rotParams = (ParameterBlockJAI) rotatedImage.getParameterBlock();
        rotParams.setParameter("transform", xform);
        rotParams.setParameter("interpolation", Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        rotatedImage.setParameterBlock(rotParams);
        ParameterBlockJAI cropParams = (ParameterBlockJAI) croppedImage.getParameterBlock();
        float cropWidth = (float) (cropMaxX - cropMinX);
        cropWidth = (cropWidth > 0.000001) ? cropWidth : 0.000001f;
        float cropHeight = (float) (cropMaxY - cropMinY);
        cropHeight = (cropHeight > 0.000001) ? cropHeight : 0.000001f;
        float cropX = (float) (rotatedImage.getMinX() + cropMinX * rotatedImage.getWidth());
        float cropY = (float) (rotatedImage.getMinY() + cropMinY * rotatedImage.getHeight());
        float cropW = cropWidth * rotatedImage.getWidth();
        float cropH = cropHeight * rotatedImage.getHeight();
        cropParams.setParameter("x", cropX);
        cropParams.setParameter("y", cropY);
        cropParams.setParameter("width", cropW);
        cropParams.setParameter("height", cropH);
        croppedImage.setParameterBlock(cropParams);
        ParameterBlockJAI pbXlate = (ParameterBlockJAI) xformCroppedImage.getParameterBlock();
        pbXlate.addSource(croppedImage);
        pbXlate.setParameter("xTrans", (float) (-croppedImage.getMinX()));
        pbXlate.setParameter("yTrans", (float) (-croppedImage.getMinY()));
        xformCroppedImage.setParameterBlock(pbXlate);
    }

    protected PlanarImage getScaled(RenderableOp unscaledImage, int maxWidth, int maxHeight) {
        AffineTransform thumbScale = org.photovault.image.ImageXform.getFittingXform(maxWidth, maxHeight, 0, unscaledImage.getWidth(), unscaledImage.getHeight());
        ParameterBlockJAI scaleParams = new ParameterBlockJAI("affine");
        scaleParams.addSource(unscaledImage);
        scaleParams.setParameter("transform", thumbScale);
        scaleParams.setParameter("interpolation", Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        RenderedOp scaledImage = JAI.create("affine", scaleParams);
        return scaledImage;
    }

    protected PlanarImage getScaled(PlanarImage unscaledImage, double scale) {
        AffineTransform thumbScale = org.photovault.image.ImageXform.getScaleXform(scale, 0, unscaledImage.getWidth(), unscaledImage.getHeight());
        ParameterBlockJAI scaleParams = new ParameterBlockJAI("affine");
        scaleParams.addSource(unscaledImage);
        scaleParams.setParameter("transform", thumbScale);
        scaleParams.setParameter("interpolation", Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
        RenderedOp scaledImage = JAI.create("affine", scaleParams);
        return scaledImage;
    }

    /**
     Get the JAI graph node for color correction
     @param src Node to use as input for color correction.
     */
    protected RenderableOp getColorCorrected(RenderableOp src) {
        LookupTableJAI jailut = createColorMappingLUT();
        return LookupDescriptor.createRenderable(src, jailut, null);
    }

    /**
     Add saturation mapping into from of the image processing pipeline.
     @param src The image to which saturation correction is applied
     @return Saturation change operator.
     */
    protected RenderableOp getSaturated(RenderableOp src) {
        IHSColorSpace ihs = IHSColorSpace.getInstance();
        ColorModel srcCm = getCorrectedImageColorModel();
        int[] componentSizes = srcCm.getComponentSize();
        if (componentSizes.length != 3) {
            return null;
        }
        ColorModel ihsColorModel = new ComponentColorModel(ihs, componentSizes, false, false, Transparency.OPAQUE, srcCm.getTransferType());
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        pb.add(ihsColorModel);
        RenderableOp ihsImage = JAI.createRenderable("colorconvert", pb);
        ihsImage.setProperty("org.photovault.opname", "color_corrected_ihs_image");
        LookupTableJAI jailut = createSaturationMappingLUT();
        saturatedIhsImage = LookupDescriptor.createRenderable(ihsImage, jailut, null);
        pb = new ParameterBlock();
        pb.addSource(saturatedIhsImage);
        ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel srgbColorModel = new ComponentColorModel(sRGB, componentSizes, false, false, Transparency.OPAQUE, srcCm.getTransferType());
        pb.add(srgbColorModel);
        RenderableOp saturatedImage = JAI.createRenderable("colorconvert", pb);
        saturatedImage.setProperty("org.photovault.opname", "saturated_image");
        return saturatedImage;
    }

    /**
     Find the last cached rendering of a certain phase of image processing pipeline.
     The name is stored in JAI property "org.photovault.opname".
     <p>
     This function makes a depth-first search to the sources of given node and 
     returns the first node that has the name.
     @param op The "sink" node of the image processing graph that is searched
     @param The name
     @return First node found with given name or <code>NULL</codel> if no such 
     node is found.
     */
    RenderedImage findNamedRendering(RenderedImage op, String name) {
        if (op != null) {
            Object imgName = op.getProperty("org.photovault.opname");
            if (imgName.equals(name)) {
                return op;
            }
            Vector sources = op.getSources();
            if (sources == null) {
                return null;
            }
            for (Object o : sources) {
                if (o != null && o instanceof RenderedImage) {
                    RenderedImage candidate = findNamedRendering((RenderedImage) o, name);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the film speed setting used when shooting the image
     * 
     * @return Film speed (in ISO) as reported by dcraw
     */
    public abstract int getFilmSpeed();

    /**
     * Get the focal length from image file meta data.
     * 
     * @return Focal length used when taking the picture (in millimetres)
     */
    public abstract double getFocalLength();

    public abstract RenderedImage getImage();

    public File getImageFile() {
        return f;
    }

    /**
     * Get the shutter speed used when shooting the image
     * 
     * @return Exposure time (in seconds) as reported by dcraw
     */
    public abstract double getShutterSpeed();

    /**
     * Get the shooting time of the image
     * 
     * @return Shooting time as reported by dcraw or <CODE>null</CODE> if
     * not available
     */
    public abstract Date getTimestamp();
}
