package com.sun.imageio.plugins.png;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.util.Locale;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.spi.ImageWriterSpi;

/**
 * @version 0.5
 */
public class PNGImageWriterSpi extends ImageWriterSpi {

    private static final String vendorName = "Sun Microsystems, Inc.";

    private static final String version = "1.0";

    private static final String[] names = { "png", "PNG" };

    private static final String[] suffixes = { "png" };

    private static final String[] MIMETypes = { "image/png", "image/x-png" };

    private static final String writerClassName = "com.sun.imageio.plugins.png.PNGImageWriter";

    private static final String[] readerSpiNames = { "com.sun.imageio.plugins.png.PNGImageReaderSpi" };

    public PNGImageWriterSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName, STANDARD_OUTPUT_TYPE, readerSpiNames, false, null, null, null, null, true, PNGMetadata.nativeMetadataFormatName, "com.sun.imageio.plugins.png.PNGMetadataFormat", null, null);
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        SampleModel sampleModel = type.getSampleModel();
        ColorModel colorModel = type.getColorModel();
        int[] sampleSize = sampleModel.getSampleSize();
        int bitDepth = sampleSize[0];
        for (int i = 1; i < sampleSize.length; i++) {
            if (sampleSize[i] > bitDepth) {
                bitDepth = sampleSize[i];
            }
        }
        if (bitDepth < 1 || bitDepth > 16) {
            return false;
        }
        int numBands = sampleModel.getNumBands();
        if (numBands < 1 || numBands > 4) {
            return false;
        }
        boolean hasAlpha = colorModel.hasAlpha();
        if (colorModel instanceof IndexColorModel) {
            return true;
        }
        if ((numBands == 1 || numBands == 3) && hasAlpha) {
            return false;
        }
        if ((numBands == 2 || numBands == 4) && !hasAlpha) {
            return false;
        }
        return true;
    }

    public String getDescription(Locale locale) {
        return "Standard PNG image writer";
    }

    public ImageWriter createWriterInstance(Object extension) {
        return new PNGImageWriter(this);
    }
}
