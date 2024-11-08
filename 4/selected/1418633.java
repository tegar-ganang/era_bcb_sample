package javax.imageio.spi;

import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * @author Michael Koch (konqueror@gmx.de)
 */
public abstract class ImageWriterSpi extends ImageReaderWriterSpi {

    public static final Class[] STANDARD_OUTPUT_TYPE = { ImageOutputStream.class };

    protected Class[] outputTypes;

    protected String[] readerSpiNames;

    protected ImageWriterSpi() {
    }

    public ImageWriterSpi(String vendorName, String version, String[] names, String[] suffixes, String[] MIMETypes, String writerClassName, Class[] outputTypes, String[] readerSpiNames, boolean supportsStandardStreamMetadataFormat, String nativeStreamMetadataFormatName, String nativeStreamMetadataFormatClassName, String[] extraStreamMetadataFormatNames, String[] extraStreamMetadataFormatClassNames, boolean supportsStandardImageMetadataFormat, String nativeImageMetadataFormatName, String nativeImageMetadataFormatClassName, String[] extraImageMetadataFormatNames, String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
        if (writerClassName == null) throw new IllegalArgumentException("writerClassName is null");
        if (outputTypes == null || outputTypes.length == 0) throw new IllegalArgumentException("outputTypes may not be null or empty");
        this.outputTypes = outputTypes;
        this.readerSpiNames = readerSpiNames;
    }

    public abstract boolean canEncodeImage(ImageTypeSpecifier type);

    public boolean canEncodeImage(RenderedImage image) {
        return canEncodeImage(new ImageTypeSpecifier(image));
    }

    public ImageWriter createWriterInstance() throws IOException {
        return createWriterInstance(null);
    }

    public abstract ImageWriter createWriterInstance(Object extension) throws IOException;

    public String[] getImageReaderSpiNames() {
        return readerSpiNames;
    }

    public Class[] getOutputTypes() {
        return outputTypes;
    }

    public boolean isFormatLossless() {
        return true;
    }

    public boolean isOwnWriter(ImageWriter writer) {
        if (writer == null) throw new IllegalArgumentException("writer may not be null");
        return pluginClassName.equals(writer.getClass().getName());
    }
}
