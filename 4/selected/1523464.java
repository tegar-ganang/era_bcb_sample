package gnu.javax.imageio.jpeg;

import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;

public class JPEGImageReaderSpi extends ImageReaderSpi {

    static final String vendorName = "GNU";

    static final String version = "0.1";

    static final String readerClassName = "gnu.javax.imageio.jpeg.JPEGImageReader";

    static final String[] names = { "JPEG" };

    static final String[] suffixes = { ".jpeg", ".jpg", ".jpe" };

    static final String[] MIMETypes = { "image/jpeg" };

    static final String[] writerSpiNames = { "gnu.javax.imageio.jpeg.JPEGImageWriterSpi" };

    static final boolean supportsStandardStreamMetadataFormat = false;

    static final String nativeStreamMetadataFormatName = null;

    static final String nativeStreamMetadataFormatClassName = null;

    static final String[] extraStreamMetadataFormatNames = null;

    static final String[] extraStreamMetadataFormatClassNames = null;

    static final boolean supportsStandardImageMetadataFormat = false;

    static final String nativeImageMetadataFormatName = null;

    static final String nativeImageMetadataFormatClassName = null;

    static final String[] extraImageMetadataFormatNames = null;

    static final String[] extraImageMetadataFormatClassNames = null;

    private static JPEGImageReaderSpi readerSpi;

    public JPEGImageReaderSpi() {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName, STANDARD_INPUT_TYPE, writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName, nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames, extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat, nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
        System.out.println("JPEGImageReaderSPI!!!");
    }

    public String getDescription(Locale locale) {
        return "JPEG ISO 10918-1, JFIF V1.02";
    }

    public boolean canDecodeInput(Object input) throws IOException {
        if (!(input instanceof ImageInputStream)) return false;
        ImageInputStream in = (ImageInputStream) input;
        boolean retval;
        in.mark();
        try {
            new JPEGDecoder(in);
            retval = true;
        } catch (JPEGException e) {
            retval = false;
        }
        in.reset();
        return retval;
    }

    public ImageReader createReaderInstance(Object extension) {
        return new JPEGImageReader(this);
    }

    public static void registerSpis(IIORegistry reg) {
        reg.registerServiceProvider(getReaderSpi(), ImageReaderSpi.class);
    }

    public static synchronized JPEGImageReaderSpi getReaderSpi() {
        if (readerSpi == null) readerSpi = new JPEGImageReaderSpi();
        return readerSpi;
    }
}
