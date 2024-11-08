package javax.imageio.spi;

import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * @author Michael Koch (konqueror@gmx.de)
 */
public abstract class ImageReaderSpi extends ImageReaderWriterSpi {

    public static final Class[] STANDARD_INPUT_TYPE = { ImageInputStream.class };

    protected Class[] inputTypes;

    protected String[] writerSpiNames;

    protected ImageReaderSpi() {
    }

    public ImageReaderSpi(String vendorName, String version, String[] names, String[] suffixes, String[] MIMETypes, String readerClassName, Class[] inputTypes, String[] writerSpiNames, boolean supportsStandardStreamMetadataFormat, String nativeStreamMetadataFormatName, String nativeStreamMetadataFormatClassName, String[] extraStreamMetadataFormatNames, String[] extraStreamMetadataFormatClassNames, boolean supportsStandardImageMetadataFormat, String nativeImageMetadataFormatName, String nativeImageMetadataFormatClassName, String[] extraImageMetadataFormatNames, String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
        if (inputTypes == null || inputTypes.length == 0) throw new IllegalArgumentException("inputTypes may not be null or empty");
        this.inputTypes = inputTypes;
        this.writerSpiNames = writerSpiNames;
    }

    public abstract boolean canDecodeInput(Object source) throws IOException;

    public ImageReader createReaderInstance() throws IOException {
        return createReaderInstance(null);
    }

    public abstract ImageReader createReaderInstance(Object extension) throws IOException;

    public String[] getImageWriterSpiNames() {
        return writerSpiNames;
    }

    public Class[] getInputTypes() {
        return inputTypes;
    }

    public boolean isOwnReader(ImageReader reader) {
        if (reader == null) throw new IllegalArgumentException("reader may not be null");
        return pluginClassName.equals(reader.getClass().getName());
    }
}
