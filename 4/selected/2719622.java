package com.yesmail.gwt.rolodex.rebind;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;

public class RolodexImageBundleBuilder {

    /**
   * The rectangle at which the original image is placed into the composite
   * image.
   */
    public static class ImageRect {

        public int height, mirroredHeight;

        public int heightOffset;

        public BufferedImage image, mirroredImage;

        public int expandedLeft, leftCollapsedLeft, rightCollapsedLeft;

        public int collapsedWidth;

        public int width;

        public ImageRect(BufferedImage image) {
            this.image = image;
            if (image == null) return;
            this.width = image.getWidth();
            this.height = image.getHeight();
        }
    }

    protected static final AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f);

    protected static final int COLUMN_BUFFER_SIZE = 3;

    protected static final double COLLAPSE_SCALE = 0.333333;

    protected static final int imageType = BufferedImage.TYPE_INT_ARGB_PRE;

    protected static final double WET_FLOOR_SCALE_FACTOR = 1.5;

    private static final boolean FORCE_DEFAULT_STRATEGY = false;

    private static HashMap<RenderingHints.Key, Object> renderConfig = new HashMap<RenderingHints.Key, Object>();

    static {
        renderConfig.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        renderConfig.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        renderConfig.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        renderConfig.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderConfig.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DEFAULT);
        renderConfig.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
        renderConfig.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        renderConfig.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        renderConfig.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    protected static RenderingHints renderingHints = new RenderingHints(renderConfig);

    private static final String BUNDLE_FILE_TYPE = "png";

    private final Map<String, ImageRect> imageNameToImageRectMap = new HashMap<String, ImageRect>();

    private int maxHeight;

    /**
   * Assimilates the image associated with a particular image method into the
   * master composite. If the method names an image that has already been
   * assimilated, the existing image rectangle is reused.
   *
   * @param logger    a hierarchical logger which logs to the hosted console
   * @param imageName the name of an image that can be found on the classpath
   * @throws com.google.gwt.core.ext.UnableToCompleteException
   *          if the image with name
   *          <code>imageName</code> cannot be added to the master composite
   *          image
   */
    public void assimilate(TreeLogger logger, String imageName) throws UnableToCompleteException {
        ImageRect rect = getMapping(imageName);
        if (rect == null) {
            rect = addImage(logger, imageName);
            putMapping(imageName, rect);
        }
    }

    public ImageRect getMapping(String imageName) {
        return imageNameToImageRectMap.get(imageName);
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public String writeBundledImage(TreeLogger logger, GeneratorContext context, int floorColor) throws UnableToCompleteException {
        BufferedImage bundledImage = drawBundledImage(logger, floorColor);
        byte[] imageBytes;
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bundledImage, BUNDLE_FILE_TYPE, byteOutputStream);
            imageBytes = byteOutputStream.toByteArray();
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Unable to generate file name for image bundle file", null);
            throw new UnableToCompleteException();
        }
        String bundleFileName = Util.computeStrongName(imageBytes) + ".cache." + BUNDLE_FILE_TYPE;
        OutputStream outStream = context.tryCreateResource(logger, bundleFileName);
        if (outStream != null) {
            try {
                outStream.write(imageBytes);
                context.commitResource(logger, outStream);
            } catch (IOException e) {
                logger.log(TreeLogger.ERROR, "Failed while writing", e);
                throw new UnableToCompleteException();
            }
        } else {
            logger.log(TreeLogger.TRACE, "Generated image bundle file already exists; no need to rewrite it.", null);
        }
        return bundleFileName;
    }

    protected BufferedImage prepareBundledImage(Collection<ImageRect> orderedImageRects) {
        int nextLeft = 0;
        int maxHeight = 0;
        for (ImageRect imageRect : orderedImageRects) {
            imageRect.collapsedWidth = (int) (imageRect.width * (1 - COLLAPSE_SCALE)) + COLUMN_BUFFER_SIZE + 2;
            imageRect.leftCollapsedLeft = nextLeft;
            imageRect.rightCollapsedLeft = nextLeft + imageRect.collapsedWidth;
            imageRect.expandedLeft = nextLeft + (imageRect.collapsedWidth * 2);
            nextLeft += imageRect.width + (imageRect.collapsedWidth * 2);
            if (imageRect.height > maxHeight) {
                maxHeight = imageRect.height;
            }
        }
        int maxBaseHeight = maxHeight;
        for (ImageRect imageRect : orderedImageRects) {
            imageRect.heightOffset = maxBaseHeight - imageRect.height;
        }
        maxHeight = (int) (maxHeight * WET_FLOOR_SCALE_FACTOR);
        BufferedImage bundledImage = new BufferedImage(nextLeft, maxHeight, imageType);
        this.maxHeight = maxHeight;
        return bundledImage;
    }

    private ImageRect addImage(TreeLogger logger, String imageName) throws UnableToCompleteException {
        logger = logger.branch(TreeLogger.TRACE, "Adding image '" + imageName + "'", null);
        try {
            URL imageUrl = getClass().getClassLoader().getResource(imageName);
            if (imageUrl == null) {
                logger.log(TreeLogger.ERROR, "Resource not found on classpath (is the name specified as " + "Class.getResource() would expect?)", null);
                throw new UnableToCompleteException();
            }
            BufferedImage image;
            try {
                image = ImageIO.read(imageUrl);
            } catch (IllegalArgumentException iex) {
                if (imageName.toLowerCase().endsWith("png") && iex.getMessage() != null && iex.getStackTrace()[0].getClassName().equals("javax.imageio.ImageTypeSpecifier$Indexed")) {
                    logger.log(TreeLogger.ERROR, "Unable to read image. The image may not be in valid PNG format. " + "This problem may also be due to a bug in versions of the " + "JRE prior to 1.6. See " + "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5098176 " + "for more information. If this bug is the cause of the " + "error, try resaving the image using a different image " + "program, or upgrade to a newer JRE.", null);
                    throw new UnableToCompleteException();
                } else {
                    throw iex;
                }
            }
            if (image == null) {
                logger.log(TreeLogger.ERROR, "Unrecognized image file format", null);
                throw new UnableToCompleteException();
            }
            return new ImageRect(image);
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Unable to read image resource", null);
            throw new UnableToCompleteException();
        }
    }

    protected static void clearGraphics(Graphics2D g2d, BufferedImage image) {
        Composite composite = g2d.getComposite();
        g2d.setComposite(alphaComposite);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setComposite(composite);
    }

    private BufferedImage drawBundledImage(TreeLogger logger, int floorColor) {
        Color backgroundColor = new Color(floorColor);
        SortedMap<String, ImageRect> sortedImageNameToImageRectMap = new TreeMap<String, ImageRect>();
        sortedImageNameToImageRectMap.putAll(imageNameToImageRectMap);
        Collection<ImageRect> orderedImageRects = sortedImageNameToImageRectMap.values();
        BufferedImage bundledImage = prepareBundledImage(orderedImageRects);
        Graphics2D g2d = bundledImage.createGraphics();
        g2d.setRenderingHints(renderingHints);
        clearGraphics(g2d, bundledImage);
        ImageEffectsStrategy imageEffectsStrategy = getImageEffectsStrategy(logger);
        for (ImageRect imageRect : orderedImageRects) {
            imageRect.mirroredImage = imageEffectsStrategy.drawWetFloorImage(imageRect, backgroundColor);
            imageRect.mirroredHeight = imageRect.mirroredImage.getHeight();
            imageEffectsStrategy.drawCollapsedLeftImage(imageRect, g2d);
            imageEffectsStrategy.drawCollapsedRightImage(imageRect, g2d);
            g2d.drawImage(imageRect.mirroredImage, imageRect.expandedLeft, imageRect.heightOffset, null);
        }
        g2d.dispose();
        return bundledImage;
    }

    protected ImageEffectsStrategy getImageEffectsStrategy(TreeLogger logger) {
        Class jaiClass = null;
        try {
            jaiClass = Class.forName("javax.media.jai.GraphicsJAI");
        } catch (ClassNotFoundException e) {
        }
        if (jaiClass == null | FORCE_DEFAULT_STRATEGY) {
            return new DefaultImageEffectsStrategyImpl();
        } else {
            try {
                Class jaiStrategyClass = Class.forName("com.yesmail.gwt.rolodex.rebind.JAIImageEffectsStrategyImpl");
                ImageEffectsStrategy strategy = (ImageEffectsStrategy) jaiStrategyClass.getConstructor().newInstance();
                return strategy;
            } catch (Exception e) {
                logger.log(TreeLogger.WARN, "Unable to load JAIImageEffectsStrategyImpl, using DefaultImageEffectsStrategyImpl", e);
                return new DefaultImageEffectsStrategyImpl();
            }
        }
    }

    private void putMapping(String imageName, ImageRect rect) {
        imageNameToImageRectMap.put(imageName, rect);
    }
}
